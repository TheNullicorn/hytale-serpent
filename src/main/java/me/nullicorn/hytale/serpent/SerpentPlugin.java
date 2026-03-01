package me.nullicorn.hytale.serpent;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.hytale.serpent.asset.SerpentBoneConfig;
import me.nullicorn.hytale.serpent.asset.SerpentConfig;
import me.nullicorn.hytale.serpent.command.SerpentCommand;
import me.nullicorn.hytale.serpent.component.Serpent;
import me.nullicorn.hytale.serpent.component.SerpentBone;
import me.nullicorn.hytale.serpent.system.*;

import javax.annotation.Nonnull;

public final class SerpentPlugin extends JavaPlugin {
    private static SerpentPlugin instance;

    private ComponentType<EntityStore, Serpent> serpentComponentType;
    private ComponentType<EntityStore, SerpentBone> serpentBoneComponentType;

    public static SerpentPlugin get() {
        return instance;
    }

    public SerpentPlugin(@Nonnull final JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        instance = this;

        this.getAssetRegistry().register(
            HytaleAssetStore.builder(SerpentBoneConfig.class, new DefaultAssetMap<>())
                .setPath(SerpentBoneConfig.PATH)
                .setCodec(SerpentBoneConfig.CODEC)
                .setKeyFunction(SerpentBoneConfig::getId)
                .loadsAfter(ModelAsset.class).build()
        );
        this.getAssetRegistry().register(
            HytaleAssetStore.builder(SerpentConfig.class, new DefaultAssetMap<>())
                .setPath(SerpentConfig.PATH)
                .setCodec(SerpentConfig.CODEC)
                .setKeyFunction(SerpentConfig::getId)
                .loadsAfter(SerpentBoneConfig.class).build()
        );

        this.serpentComponentType = this.getEntityStoreRegistry().registerComponent(Serpent.class, Serpent.ID, Serpent.CODEC);
        this.serpentBoneComponentType = this.getEntityStoreRegistry().registerComponent(SerpentBone.class, SerpentBone::new);

        // Systems for initializing `Serpent` entities.
        this.getEntityStoreRegistry().registerSystem(new SerpentInitSystems.PreSpawnSystem());
        this.getEntityStoreRegistry().registerSystem(new SerpentInitSystems.SpawnSystem());
        this.getEntityStoreRegistry().registerSystem(new SerpentInitSystems.ChangeSystem());

        // Systems for controlling `Serpent` internal states.
        this.getEntityStoreRegistry().registerSystem(new SerpentTargetSystem());
        this.getEntityStoreRegistry().registerSystem(new SerpentSolverSystem());

        // Systems for managing `SerpentBone` transforms and lifetimes.
        this.getEntityStoreRegistry().registerSystem(new SerpentBoneLoadAndTransformSystem());
        this.getEntityStoreRegistry().registerSystem(new SerpentBoneUnloadSystem());

        // Asset hot reloading.
        this.getEventRegistry().register(LoadedAssetsEvent.class, SerpentConfig.class, this::updateSerpentAssets);

        this.getCommandRegistry().registerCommand(new SerpentCommand());
    }

    @Override
    protected void shutdown() {
        super.shutdown();
    }

    public ComponentType<EntityStore, Serpent> getSerpentComponentType() {
        return this.serpentComponentType;
    }

    public ComponentType<EntityStore, SerpentBone> getSerpentBoneComponentType() {
        return this.serpentBoneComponentType;
    }

    /**
     * Hot reloading support for {@link SerpentConfig}.
     * <p>
     * Handles {@link LoadedAssetsEvent} in order to update each {@link Serpent} when its {@link SerpentConfig} asset is
     * reloaded.
     */
    private void updateSerpentAssets(final LoadedAssetsEvent<String, SerpentConfig, DefaultAssetMap<String, SerpentConfig>> event) {
        // Look in each world for serpents.
        for (final World world : Universe.get().getWorlds().values()) {
            final Store<EntityStore> store = world.getEntityStore().getStore();

            // Schedule this block to be run in the `world` thread.
            world.execute(() ->
                store.forEachEntityParallel(Serpent.getComponentType(), (index, archetypeChunk, commandBuffer) -> {
                    final Ref<EntityStore> serpentRef = archetypeChunk.getReferenceTo(index);
                    final Serpent serpent = archetypeChunk.getComponent(index, Serpent.getComponentType());
                    assert serpent != null;

                    final SerpentConfig newConfig = event.getLoadedAssets().get(serpent.getConfig().getId());
                    if (newConfig == null) {
                        // This serpent's config isn't included in the even.
                        return;
                    }

                    serpent.setConfig(newConfig);

                    // Remove and re-add the `Serpent` component. This will trigger `SerpentInitSystems.SpawnSystem`
                    // to reapply the new head model.
                    commandBuffer.removeComponent(serpentRef, Serpent.getComponentType());
                    commandBuffer.addComponent(serpentRef, Serpent.getComponentType(), serpent);

                    // Remove all bone entities. This will trigger `SerpentBoneLoadAndTransformSystem` to recreate
                    // them with the updated config.
                    for (final Ref<EntityStore> boneRef : serpent.bones) {
                        if (boneRef == null || boneRef.equals(serpentRef) || !boneRef.isValid()) {
                            continue;
                        }
                        commandBuffer.removeEntity(boneRef, RemoveReason.UNLOAD);
                    }
                })
            );
        }
    }
}
