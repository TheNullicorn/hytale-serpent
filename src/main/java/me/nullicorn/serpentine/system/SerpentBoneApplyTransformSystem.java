package me.nullicorn.serpentine.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.system.TransformSystems;
import com.hypixel.hytale.server.core.modules.entity.system.UpdateLocationSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.serpentine.component.Serpent;
import me.nullicorn.serpentine.component.SerpentBone;
import me.nullicorn.serpentine.component.SerpentBoneAutoApplyTransform;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Runs each tick to add or update the {@link TransformComponent} of {@link SerpentBone} entities to match
 * {@link Serpent.Bone#transform()}.
 * <p>
 * Bones can be opted out of this behaviour by removing the {@link SerpentBoneAutoApplyTransform} component.
 */
public final class SerpentBoneApplyTransformSystem extends SerpentBoneTickingSystem {
    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(
            new SystemDependency<>(Order.BEFORE, TransformSystems.EntityTrackerUpdate.class), // The system that sends entity movements to players.
            new SystemDependency<>(Order.BEFORE, UpdateLocationSystems.TickingSystem.class)   // The system that removes entities that leave the loaded world bounds.
        );
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(super.getQuery(), SerpentBoneAutoApplyTransform.getComponentType(), Query.not(Serpent.getComponentType()));
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
        final Transform transform = bone.transform();

        final TransformComponent transformComponent = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        if (transformComponent == null) {
            commandBuffer.addComponent(boneRef, TransformComponent.getComponentType(), new TransformComponent(transform.getPosition().clone(), transform.getRotation().clone()));
        } else if (!transform.equals(transformComponent.getTransform())) {
            transformComponent.getPosition().assign(transform.getPosition());
            transformComponent.getRotation().assign(transform.getRotation());
        }
    }
}
