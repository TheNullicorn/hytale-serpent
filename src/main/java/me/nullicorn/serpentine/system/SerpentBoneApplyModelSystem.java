package me.nullicorn.serpentine.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.serpentine.component.Serpent;
import me.nullicorn.serpentine.component.SerpentBone;
import me.nullicorn.serpentine.component.SerpentBoneAutoApplyModel;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

/**
 * Runs each tick to add or update the {@link ModelComponent} of {@link SerpentBone} entities to match
 * {@link Serpent.Bone#model()}.
 * <p>
 * Bones can be opted out of this behaviour by removing the {@link SerpentBoneAutoApplyModel} component.
 */
public final class SerpentBoneApplyModelSystem extends SerpentBoneTickingSystem {
    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(
            new SystemDependency<>(Order.AFTER, SerpentBoneSpawnSystem.class)
        );
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(super.getQuery(), SerpentBoneAutoApplyModel.getComponentType());
    }

    @Override
    public void tick(
        final float dt,
        final int index,
        final ArchetypeChunk<EntityStore> archetypeChunk,
        final Store<EntityStore> store,
        final CommandBuffer<EntityStore> commandBuffer,
        final Serpent serpent,
        final Serpent.Bone bone,
        final SerpentBone boneComponent
    ) {
        final Ref<EntityStore> boneRef = archetypeChunk.getReferenceTo(index);
        final ModelComponent modelComponent = archetypeChunk.getComponent(index, ModelComponent.getComponentType());
        final Model.ModelReference modelRef = bone.model();

        if (modelComponent == null) {
            if (modelRef == null) {
                return;
            }
            final Model model = modelRef.toModel();
            if (model == null) {
                return;
            }
            commandBuffer.addComponent(boneRef, ModelComponent.getComponentType(), new ModelComponent(model));
            return;
        }

        if (modelRef == null) {
            // Model component is present despite bone having no model set; remove the component.
            commandBuffer.removeComponent(boneRef, ModelComponent.getComponentType());
            return;
        }

        if (modelRef.equals(modelComponent.getModel().toReference())) {
            // Model is identical to the one already applied.
            return;
        }

        final Model model = modelRef.toModel();
        if (model == null) {
            // Model failed to dereference.
            commandBuffer.removeComponent(boneRef, ModelComponent.getComponentType());
            return;
        }

        commandBuffer.putComponent(boneRef, ModelComponent.getComponentType(), new ModelComponent(model));
    }
}
