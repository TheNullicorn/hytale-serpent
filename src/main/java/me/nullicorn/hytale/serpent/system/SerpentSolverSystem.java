package me.nullicorn.hytale.serpent.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.hytale.serpent.component.Serpent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

public final class SerpentSolverSystem extends EntityTickingSystem<EntityStore> {
    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(
            new SystemDependency<>(Order.AFTER, SerpentTargetSystem.class)
        );
    }

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

        if (serpent.joints == null || serpent.joints.length < 2 || serpent.target == null) {
            return;
        }

        if (serpent.joints[0].position.distanceTo(serpent.target) < 0.00001) {
            return;
        }
        serpent.joints[0].position.assign(serpent.target);
        serpent.path.addFirst(serpent.target.clone());

        int lastPathIndex = 0;
        double offset = 0;
        for (int i = 1; i < serpent.joints.length; i++) {
            offset += serpent.getBoneConfig(i - 1).getLength();

            double distanceLeft = offset;
            for (int p = 0; p < serpent.path.size() - 1; p++) {
                lastPathIndex = Math.max(lastPathIndex, p + 1);
                final double segmentLength = serpent.path.get(p).distanceTo(serpent.path.get(p + 1));
                if (segmentLength >= distanceLeft) {
                    final Vector3d segmentDirection = serpent.path.get(p + 1).clone().subtract(serpent.path.get(p)).scale(1 / segmentLength);
                    serpent.joints[i].position.assign(serpent.path.get(p).clone().add(segmentDirection.clone().scale(distanceLeft)));
                    break;
                }
                distanceLeft -= segmentLength;
            }
        }

        if (lastPathIndex < serpent.path.size() - 1) {
            serpent.path.subList(lastPathIndex + 1, serpent.path.size()).clear();
        }
    }
}
