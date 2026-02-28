package me.nullicorn.hytale.serpent.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.matrix.Matrix4d;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
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
        float dt,
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

        final TimeResource timeResource = store.getResource(TimeResource.getResourceType());
        final float timeDilationModifier = timeResource.getTimeDilationModifier();
        final World world = store.getExternalData().getWorld();
        dt = 1.0F / world.getTps();
        dt *= timeDilationModifier;

        final Vector3d[] predictions = new Vector3d[serpent.joints.length];

        for (int i = 0; i < serpent.joints.length; i++) {
            predictions[i] = serpent.joints[i].position.clone().add(serpent.joints[i].velocity.clone().scale(dt));
        }

        predictions[0] = serpent.target.clone();

        for (int i = 1; i < serpent.joints.length; i++) {
            // Get how long this segment is intended to be.
            final double length = serpent.getSegmentConfig(i - 1).getLength();
            if (length == 0.0) {
                predictions[i] = predictions[i - 1].clone();
            } else {
                if (i < serpent.joints.length - 1) {
                    final Vector3d dirIn = predictions[i].clone().subtract(predictions[i - 1]).normalize();
                    final Vector3d dirOut = predictions[i + 1].clone().subtract(predictions[i]).normalize();
                    final double angleLimit = Math.toRadians(serpent.getConfig().getDefaultSoftAngleLimit());
                    final double angleBetween = getAngleBetween(dirIn, dirOut);
                    if (angleBetween > angleLimit) {
                        final Vector3d perp = dirIn.cross(dirOut);
                        final Matrix4d matrix = new Matrix4d();
                        matrix.setRotateAxis((angleBetween - angleLimit) * 0.9 * dt, perp.x, perp.y, perp.z);
                        matrix.invert();
                        matrix.multiplyDirection(dirIn);
                    }

                    predictions[i] = predictions[i - 1].clone().add(dirIn.clone().scale(length));
                } else {
                    // Get how long the segment is currently.
                    final double distance = predictions[i].distanceTo(predictions[i - 1]);
                    final double correction = length / distance;
                    // Lerp `predictions[i]` toward `predictions[i-1]` by `correction`.
                    predictions[i] = Vector3d.lerpUnclamped(predictions[i], predictions[i - 1], 1 - correction);
                }
            }
        }

        for (int i = 0; i < serpent.joints.length; i++) {
            final Serpent.Joint joint = serpent.joints[i];
            // Derive velocity from the change in `position` this tick.
            joint.velocity.assign(predictions[i].clone().subtract(joint.position).scale(1 / dt));
            // `prediction` becomes our new `position`.
            joint.position.assign(predictions[i].clone());

            // Dampen velocity.
            final double speed = joint.velocity.length();
            if (speed > serpent.getConfig().getDefaultHardDampingSpeed()) {
                // Normalize velocity and then scale it to exactly the hard speed cap.
                joint.velocity.scale((1 / speed) * serpent.getConfig().getDefaultHardDampingSpeed());
            } else if (speed > serpent.getConfig().getDefaultSoftDampingSpeed()) {
                // Dampen velocity from its current value.
                joint.velocity.scale(serpent.getConfig().getDefaultSoftDampingCoefficient());
            }
        }
    }

    private static double getAngleBetween(final Vector3d v1, final Vector3d v2) {
        return Math.acos(v1.dot(v2));
    }
}
