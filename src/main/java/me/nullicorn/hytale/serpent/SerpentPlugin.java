package me.nullicorn.hytale.serpent;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.hytale.serpent.asset.SerpentConfig;
import me.nullicorn.hytale.serpent.asset.SerpentSegmentConfig;
import me.nullicorn.hytale.serpent.command.SerpentCommand;
import me.nullicorn.hytale.serpent.component.Serpent;
import me.nullicorn.hytale.serpent.component.SerpentSegment;
import me.nullicorn.hytale.serpent.system.*;

import javax.annotation.Nonnull;

public final class SerpentPlugin extends JavaPlugin {
    private static SerpentPlugin instance;

    private ComponentType<EntityStore, Serpent> serpentComponentType;
    private ComponentType<EntityStore, SerpentSegment> serpentSegmentComponentType;

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
            HytaleAssetStore.builder(SerpentSegmentConfig.class, new DefaultAssetMap<>())
                .setPath(SerpentSegmentConfig.PATH)
                .setCodec(SerpentSegmentConfig.CODEC)
                .setKeyFunction(SerpentSegmentConfig::getId)
                .loadsAfter(ModelAsset.class).build()
        );
        this.getAssetRegistry().register(
            HytaleAssetStore.builder(SerpentConfig.class, new DefaultAssetMap<>())
                .setPath(SerpentConfig.PATH)
                .setCodec(SerpentConfig.CODEC)
                .setKeyFunction(SerpentConfig::getId)
                .loadsAfter(SerpentSegmentConfig.class).build()
        );

        this.serpentComponentType = this.getEntityStoreRegistry().registerComponent(Serpent.class, Serpent.ID, Serpent.CODEC);
        this.serpentSegmentComponentType = this.getEntityStoreRegistry().registerComponent(SerpentSegment.class, SerpentSegment::new);

        // Systems for initializing `Serpent` entities.
        this.getEntityStoreRegistry().registerSystem(new SerpentInitSystems.PreSpawnSystem());
        this.getEntityStoreRegistry().registerSystem(new SerpentInitSystems.SpawnSystem());
        this.getEntityStoreRegistry().registerSystem(new SerpentInitSystems.ChangeSystem());

        // Systems for controlling `Serpent` internal states.
        this.getEntityStoreRegistry().registerSystem(new SerpentTargetSystem());
        this.getEntityStoreRegistry().registerSystem(new SerpentSolverSystem());

        // Systems for managing `SerpentSegment` transforms and lifetimes.
        this.getEntityStoreRegistry().registerSystem(new SerpentSegmentLoadAndTransformSystem());
        this.getEntityStoreRegistry().registerSystem(new SerpentSegmentUnloadSystem());

        this.getCommandRegistry().registerCommand(new SerpentCommand());
    }

    @Override
    protected void shutdown() {
        super.shutdown();
    }

    public ComponentType<EntityStore, Serpent> getSerpentComponentType() {
        return this.serpentComponentType;
    }

    public ComponentType<EntityStore, SerpentSegment> getSerpentSegmentComponentType() {
        return this.serpentSegmentComponentType;
    }
}
