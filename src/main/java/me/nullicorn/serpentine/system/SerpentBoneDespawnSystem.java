package me.nullicorn.serpentine.system;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.system.UpdateLocationSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.serpentine.component.Serpent;
import me.nullicorn.serpentine.component.SerpentBone;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

/**
 * Runs each tick to despawn bones that aren't linked to a {@link Serpent} entity.
 * <p>
 * To keep a bone entity around without a valid {@link Serpent} reference, remove its {@link SerpentBone} component.
 */
public final class SerpentBoneDespawnSystem extends EntityTickingSystem<EntityStore> {
    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(
            new SystemDependency<>(Order.AFTER, UpdateLocationSystems.TickingSystem.class)
        );
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            SerpentBone.getComponentType(),
            Query.not(Serpent.getComponentType())
        );
    }

    @Override
    public void tick(
        final float dt,
        final int index,
        @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull final Store<EntityStore> store,
        @Nonnull final CommandBuffer<EntityStore> commandBuffer
    ) {
        final Ref<EntityStore> boneRef = archetypeChunk.getReferenceTo(index);
        final SerpentBone boneComponent = archetypeChunk.getComponent(index, SerpentBone.getComponentType());
        assert boneComponent != null;

        if (boneComponent.index() >= 0 && boneComponent.serpent() != null && boneComponent.serpent().isValid()) {
            final Serpent serpent = commandBuffer.getComponent(boneComponent.serpent(), Serpent.getComponentType());
            if (serpent != null && boneComponent.index() < serpent.bones().size()) {
                final Serpent.Bone bone = serpent.bones().get(boneComponent.index());
                if (bone.ref() != null && bone.ref().equals(boneRef)) {
                    return;
                }
            }
        }

        commandBuffer.removeEntity(boneRef, RemoveReason.REMOVE);
    }
}
