package me.nullicorn.serpentine.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.serpentine.component.Serpent;
import me.nullicorn.serpentine.component.SerpentBone;
import me.nullicorn.serpentine.component.SerpentBoneAutoApplyScale;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

/**
 * Runs each tick to add or update the {@link EntityScaleComponent} of {@link SerpentBone} entities to match
 * {@link Serpent.Bone#scale()}.
 * <p>
 * Bones can be opted out of this behaviour by removing the {@link SerpentBoneAutoApplyScale} component.
 */
public final class SerpentBoneApplyScaleSystem extends SerpentBoneTickingSystem {
    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(
            new SystemDependency<>(Order.AFTER, SerpentBoneSpawnSystem.class)
        );
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(super.getQuery(), SerpentBoneAutoApplyScale.getComponentType());
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
        final float scale = (float) bone.scale();

        final EntityScaleComponent scaleComponent = archetypeChunk.getComponent(index, EntityScaleComponent.getComponentType());
        if (scaleComponent != null && scaleComponent.getScale() != scale) {
            // Bone already has a scale component and the two values differ, so update the component.
            scaleComponent.setScale(scale);

        } else if (scaleComponent == null && scale != 1.0) {
            // Bone doesn't have a scale component, so add one because the bone has a non-default scale.
            commandBuffer.addComponent(boneRef, EntityScaleComponent.getComponentType(), new EntityScaleComponent(scale));
        }
    }
}
