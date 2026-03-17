package me.nullicorn.serpentine;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.serpentine.asset.*;
import me.nullicorn.serpentine.command.SerpentCommand;
import me.nullicorn.serpentine.component.*;
import me.nullicorn.serpentine.solver.DefaultSerpentBoneSolver;
import me.nullicorn.serpentine.solver.DefaultSerpentJointSolver;
import me.nullicorn.serpentine.solver.SerpentBoneSolver;
import me.nullicorn.serpentine.solver.SerpentJointSolver;
import me.nullicorn.serpentine.system.*;

import javax.annotation.Nonnull;

public final class SerpentPlugin extends JavaPlugin {
    private static SerpentPlugin instance;

    private ComponentType<EntityStore, Serpent> serpentComponentType;
    private ComponentType<EntityStore, SerpentBone> serpentBoneComponentType;
    private ComponentType<EntityStore, SerpentBoneAutoApplyScale> serpentBoneAutoApplyScaleComponentType;
    private ComponentType<EntityStore, SerpentBoneAutoApplyModel> serpentBoneAutoApplyModelComponentType;
    private ComponentType<EntityStore, SerpentBoneAutoApplyTransform> serpentBoneAutoApplyTransformComponentType;

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

        SerpentLayoutNode.CODEC.register(SerpentLayoutBone.ID, SerpentLayoutBone.class, SerpentLayoutBone.CODEC);
        SerpentLayoutNode.CODEC.register(SerpentLayoutSequence.ID, SerpentLayoutSequence.class, SerpentLayoutSequence.CODEC);
        SerpentLayoutNode.CODEC.register(SerpentLayoutRepeater.ID, SerpentLayoutRepeater.class, SerpentLayoutRepeater.CODEC);

        SerpentJointSolver.CODEC.register(DefaultSerpentJointSolver.ID, DefaultSerpentJointSolver.class, DefaultSerpentJointSolver.CODEC);
        SerpentBoneSolver.CODEC.register(DefaultSerpentBoneSolver.ID, DefaultSerpentBoneSolver.class, DefaultSerpentBoneSolver.CODEC);

        this.serpentComponentType = this.getEntityStoreRegistry().registerComponent(Serpent.class, Serpent.ID, Serpent.CODEC);
        this.serpentBoneComponentType = this.getEntityStoreRegistry().registerComponent(SerpentBone.class, () -> {
            throw new UnsupportedOperationException("Not implemented");
        });
        this.serpentBoneAutoApplyScaleComponentType = this.getEntityStoreRegistry().registerComponent(SerpentBoneAutoApplyScale.class, SerpentBoneAutoApplyScale::get);
        this.serpentBoneAutoApplyModelComponentType = this.getEntityStoreRegistry().registerComponent(SerpentBoneAutoApplyModel.class, SerpentBoneAutoApplyModel::get);
        this.serpentBoneAutoApplyTransformComponentType = this.getEntityStoreRegistry().registerComponent(SerpentBoneAutoApplyTransform.class, SerpentBoneAutoApplyTransform::get);

        this.getEntityStoreRegistry().registerSystem(new SerpentHeadSpawnSystems.SpawnRefSystem());
        this.getEntityStoreRegistry().registerSystem(new SerpentHeadSpawnSystems.SpawnRefChangeSystem());
        this.getEntityStoreRegistry().registerSystem(new SerpentSolverSystem());
        this.getEntityStoreRegistry().registerSystem(new SerpentBoneSpawnSystem());
        this.getEntityStoreRegistry().registerSystem(new SerpentBoneDespawnSystem());
        this.getEntityStoreRegistry().registerSystem(new SerpentBoneApplyScaleSystem());
        this.getEntityStoreRegistry().registerSystem(new SerpentBoneApplyModelSystem());
        this.getEntityStoreRegistry().registerSystem(new SerpentBoneApplyTransformSystem());

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

    public ComponentType<EntityStore, SerpentBoneAutoApplyScale> getSerpentBoneAutoApplyScaleComponentType() {
        return this.serpentBoneAutoApplyScaleComponentType;
    }

    public ComponentType<EntityStore, SerpentBoneAutoApplyModel> getSerpentBoneAutoApplyModelComponentType() {
        return this.serpentBoneAutoApplyModelComponentType;
    }

    public ComponentType<EntityStore, SerpentBoneAutoApplyTransform> getSerpentBoneAutoApplyTransformComponentType() {
        return this.serpentBoneAutoApplyTransformComponentType;
    }
}
