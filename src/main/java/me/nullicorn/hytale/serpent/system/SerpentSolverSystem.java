package me.nullicorn.hytale.serpent.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.hytale.serpent.component.Serpent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SerpentSolverSystem extends EntityTickingSystem<EntityStore> {
    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Serpent.getComponentType();
    }

    @Override
    public void tick(
        final float dt,
        final int index,
        @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull final Store<EntityStore> store,
        @Nonnull final CommandBuffer<EntityStore> commandBuffer
    ) {
        final Serpent serpent = archetypeChunk.getComponent(index, Serpent.getComponentType());
        assert serpent != null;

        if (serpent.joints == null || serpent.joints.length < 2) {
            return;
        }

        // Stored so we can check if the neck moved substantially. If it didn't we'll exit early.
        final Vector3d oldNeckPosition = serpent.joints[1].position.clone();

        final TransformComponent headTransform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        final HeadRotation headRotation = archetypeChunk.getComponent(index, HeadRotation.getComponentType());
        if (headTransform != null && headRotation != null) {
            final Vector3d offset = headRotation.getDirection().scale(serpent.getBoneConfig(0).getLength() / 2);
            // Move the first joint to the front of the head.
            serpent.joints[0].position.assign(headTransform.getPosition().clone().add(offset));
            // Move the second joint to the rear of the head.
            serpent.joints[1].position.assign(headTransform.getPosition().clone().subtract(offset));
        }

        if (serpent.joints[1].position.distanceTo(oldNeckPosition) < 0.00001) {
            // The neck barely moved, so don't bother moving bones.
            return;
        }

        // Add the new neck position to the front of the path.
        serpent.path.addFirst(serpent.joints[1].position.clone());

        // FIXME: When bones move backward (when the head backtracks) bones that reach the tail get compressed into its
        //        position. Implement some form of extrapolation on `path` so that the tail bone can go backward.

        int pathIndex = 0;
        double remainder = 0.0;

        // `i = 2` because we want to start at the first joint after the neck.
        for (int i = 2; i < serpent.joints.length; i++) {
            final double boneLength = serpent.getBoneConfig(i - 1).getLength();

            // Account for how far along the path segment the previous joint left off.
            double distLeft = remainder + boneLength;
            for (; pathIndex < serpent.path.size() - 1; pathIndex++) {
                final Vector3d thisPathNode = serpent.path.get(pathIndex);
                final Vector3d nextPathNode = serpent.path.get(pathIndex + 1);
                final Vector3d pathSegment = nextPathNode.clone().subtract(thisPathNode);
                final double pathSegmentLength = pathSegment.length();
                // See if the joint should be placed along this path segment.
                if (pathSegmentLength > distLeft) {
                    // Normalize `pathSegment`.
                    final Vector3d pathSegmentDirection = pathSegment.clone().scale(1 / pathSegmentLength);
                    // Interpolate the joint along the segment.
                    serpent.joints[i].position.assign(thisPathNode.clone().add(pathSegmentDirection.clone().scale(distLeft)));
                    // Save how far into the segment we left off so that the next joint can continue from there.
                    remainder = distLeft;
                    // Next joint!
                    break;
                }
                // Next path segment!
                distLeft -= pathSegmentLength;
            }
        }

        // Remove path nodes that the tail bone has gone past.
        if (pathIndex < serpent.path.size() - 2) {
            serpent.path.subList(pathIndex + 2, serpent.path.size()).clear();
        }
    }
}
