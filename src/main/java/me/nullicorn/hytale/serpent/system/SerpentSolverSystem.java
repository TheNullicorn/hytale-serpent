package me.nullicorn.hytale.serpent.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.matrix.Matrix4d;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
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
        float dt,
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

        final TimeResource timeResource = store.getResource(TimeResource.getResourceType());
        final float timeDilationModifier = timeResource.getTimeDilationModifier();
        final World world = store.getExternalData().getWorld();
        dt = 1.0F / world.getTps();
        dt *= timeDilationModifier;

        final Vector3d[] predictions = new Vector3d[serpent.joints.length];

        for (int i = 0; i < serpent.joints.length; i++) {
            predictions[i] = serpent.joints[i].position.clone();
        }

        final TransformComponent headTransform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        final HeadRotation headRotation = archetypeChunk.getComponent(index, HeadRotation.getComponentType());
        if (headTransform != null && headRotation != null) {
            final Vector3d offset = headRotation.getDirection().scale(serpent.getBoneConfig(0).getLength() / 2);
            // Move the first joint to the front of the head.
            predictions[0].assign(headTransform.getPosition().clone().add(offset));
            // Move the second joint to the rear of the head.
            predictions[1].assign(headTransform.getPosition().clone().subtract(offset));
        }

        for (int i = 2; i < predictions.length; i++) {
            // Get how long this bone is intended to be.
            final double length = serpent.getBoneConfig(i - 1).getLength();
            if (length == 0.0) {
                predictions[i] = predictions[i - 1].clone();
            } else {
                if (i < predictions.length - 1) {
                    final Vector3d prevBoneDir = predictions[i - 1].clone().subtract(predictions[i - 2]).normalize();
                    final Vector3d thisBoneDir = predictions[i].clone().subtract(predictions[i - 1]).normalize();
                    final double angleBetween = getAngleBetween(prevBoneDir, thisBoneDir);
                    final double angleLimit = Math.toRadians(serpent.getConfig().getDefaultSoftAngleLimit());
                    if (angleBetween > angleLimit) {
                        // Get the axis we need to rotating around, perpendicular to the plane formed by the bones.
                        final Vector3d rotationAxis = prevBoneDir.cross(thisBoneDir);
                        // Joints closer to the head are corrected more instantaneously to make them feel stiffer.
                        // Joints closer to the tail are corrected gradually over time.
                        // TODO: Move the time multiplier into `SerpentConfig` (the `* 10` part below).
                        final double correctionScale = MathUtil.lerp(1.0, Math.min(1.0, dt * 10), (double) i / (predictions.length - 1));

                        final Matrix4d matrix = new Matrix4d();
                        matrix.setRotateAxis((angleBetween - angleLimit) * correctionScale, rotationAxis.x, rotationAxis.y, rotationAxis.z);
                        matrix.multiplyDirection(thisBoneDir);
                    }

                    predictions[i] = predictions[i - 1].clone().add(thisBoneDir.clone().scale(length));
                } else {
                    // Get how long the bone is currently.
                    final double distance = predictions[i].distanceTo(predictions[i - 1]);
                    final double correction = length / distance;
                    // Lerp `predictions[i]` toward `predictions[i-1]` by `correction`.
                    predictions[i] = Vector3d.lerpUnclamped(predictions[i], predictions[i - 1], 1 - correction);
                }
            }
        }

        for (int i = 0; i < serpent.joints.length; i++) {
            final Serpent.Joint joint = serpent.joints[i];
            // `prediction` becomes our new `position`.
            joint.position.assign(predictions[i].clone());
        }
    }

    private static double getAngleBetween(final Vector3d v1, final Vector3d v2) {
        return Math.acos(Math.clamp(v1.dot(v2), -1.0, 1.0));
    }
}
