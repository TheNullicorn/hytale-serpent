package me.nullicorn.serpentine.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.serpentine.component.Serpent;
import me.nullicorn.serpentine.component.SerpentBone;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An abstract class for systems ticking {@link SerpentBone} entities.
 * <p>
 * If overriding the default {@link #getQuery()}, be sure to include {@link SerpentBone} as a requirement in the query.
 */
public abstract class SerpentBoneTickingSystem extends EntityTickingSystem<EntityStore> {
    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return SerpentBone.getComponentType();
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

        if (boneComponent.index() < 0 || !boneComponent.serpent().isValid()) {
            // Index is out of bounds (negative), or the serpent that owns the bone is gone.
            return;
        }

        final Serpent serpent = commandBuffer.getComponent(boneComponent.serpent(), Serpent.getComponentType());
        if (serpent == null || boneComponent.index() >= serpent.bones().size()) {
            // Bone points to a non-existent serpent, or the bone's index is too high for the serpent.
            return;
        }

        final Serpent.Bone boneState = serpent.bones().get(boneComponent.index());
        if (boneState.ref() != boneRef) {
            // Bone doesn't match the one stored in the serpent at that index, or the bone opts out of auto-scaling.
            return;
        }

        this.tick(dt, index, archetypeChunk, store, commandBuffer, serpent, boneState, boneComponent);
    }

    protected abstract void tick(
        final float dt,
        final int index,
        final ArchetypeChunk<EntityStore> archetypeChunk,
        final Store<EntityStore> store,
        final CommandBuffer<EntityStore> commandBuffer,
        final Serpent serpent,
        final Serpent.Bone bone,
        final SerpentBone boneComponent
    );
}
