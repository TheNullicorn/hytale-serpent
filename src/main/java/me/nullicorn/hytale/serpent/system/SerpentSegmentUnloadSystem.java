package me.nullicorn.hytale.serpent.system;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.system.UpdateLocationSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.hytale.serpent.component.Serpent;
import me.nullicorn.hytale.serpent.component.SerpentSegment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

/**
 * Runs each tick to remove {@link SerpentSegment} entities that are not part of a valid {@link Serpent}.
 */
public final class SerpentSegmentUnloadSystem extends EntityTickingSystem<EntityStore> {
    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(
            new SystemDependency<>(Order.AFTER, UpdateLocationSystems.TickingSystem.class)
        );
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
            SerpentSegment.getComponentType(),
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
        final Ref<EntityStore> segmentRef = archetypeChunk.getReferenceTo(index);
        final SerpentSegment segment = archetypeChunk.getComponent(index, SerpentSegment.getComponentType());
        assert segment != null;

        // Validate the `serpent` ref and the lower bound of `index`.
        if (segment.index >= 0 && segment.serpent != null && segment.serpent.isValid()) {
            final Serpent serpent = commandBuffer.getComponent(segment.serpent, Serpent.getComponentType());
            // Validate the upper bound of `index` and that the serpent contains the segment.
            if (serpent != null && segment.index < serpent.segments.length && serpent.segments[segment.index].equals(segmentRef)) {
                // Segment and serpent have a valid relationship. Don't remove.
                return;
            }
        }

        // Remove the stray segment.
        commandBuffer.removeEntity(segmentRef, RemoveReason.UNLOAD);
    }
}
