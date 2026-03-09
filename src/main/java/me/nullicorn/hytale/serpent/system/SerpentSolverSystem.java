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

        final double guideRailNodeSpacing = serpent.scale * 1.5;

        final TransformComponent headTransform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        final HeadRotation headRotation = archetypeChunk.getComponent(index, HeadRotation.getComponentType());
        assert headTransform != null;
        assert headRotation != null;
        // Snap the two head joints into place based on the head entity's position and rotation.
        updateHeadJoints(serpent, headTransform, headRotation);
        // Initialize the `guideRail` (if necessary) and reposition the two `guideRail` nodes for the head.
        prepareGuideRail(serpent, guideRailNodeSpacing);
        // Enforce spacing and angular limits between nodes on the `guideRail`.
        solveGuideRail(serpent, guideRailNodeSpacing);
        // Reposition joints along the `guideRail`.
        solveJoints(serpent);
    }

    private static void updateHeadJoints(
        final Serpent serpent,
        final TransformComponent headTransform,
        final HeadRotation headRotation
    ) {
        final Vector3d offset = headRotation.getDirection().scale(serpent.getBoneLength(0) / 2);
        // Move the first joint to the front of the head.
        serpent.joints[0].position.assign(headTransform.getPosition().clone().add(offset));
        // Move the second joint to the rear of the head.
        serpent.joints[1].position.assign(headTransform.getPosition().clone().subtract(offset));
    }

    private static void prepareGuideRail(final Serpent serpent, final double nodeSpacing) {
        // If the serpent doesn't have its guide rail set up yet, initialize it to its current joint positions.
        if (serpent.guideRail.isEmpty()) {
            for (int i = 0; i < serpent.joints.length; i++) {
                serpent.guideRail.add(serpent.joints[i].position.clone());
            }
        }

        final Vector3d headFrontNode = serpent.guideRail.get(0); // The very first node: the tip of the head.
        final Vector3d headRearNode = serpent.guideRail.get(1);  // The node that joins the head to the neck.
        final Vector3d bufferNode = serpent.guideRail.get(2);    // The first node into the neck.

        headFrontNode.assign(serpent.joints[0].position);
        headRearNode.assign(serpent.joints[1].position);

        // If the head is too far from the `bufferNode`, create a new node between them. This becomes the new
        // `bufferNode`, and the old one is bumped back a slot.
        if (headRearNode.distanceTo(bufferNode) > nodeSpacing) {
            final Vector3d offset = headRearNode.clone().subtract(bufferNode).setLength(nodeSpacing);
            serpent.guideRail.add(2, bufferNode.clone().add(offset));
        }
    }

    private static void solveGuideRail(final Serpent serpent, final double nodeSpacing) {
        for (int i = 2; i < serpent.guideRail.size(); i++) {
            final Vector3d a = serpent.guideRail.get(i - 2);
            final Vector3d b = serpent.guideRail.get(i - 1);
            final Vector3d c = serpent.guideRail.get(i);
            final Vector3d prevDirection = Vector3d.directionTo(a, b);
            final Vector3d thisDirection = Vector3d.directionTo(b, c);
            final Vector3d newDirection = thisDirection.clone();

            // Get how far along `guideRail` this node is, as a percentage.
            final double t = (double) i / (serpent.guideRail.size() - 1);

            final double angleLimit = Math.toRadians(MathUtil.lerp(20, 180, t));
            final double angle = getAngleBetween(prevDirection, thisDirection);

            if (angle > angleLimit) {
                // Get the axis we need to rotate around; the one perpendicular to both vectors.
                final Vector3d rotationAxis = prevDirection.cross(thisDirection);

                final Matrix4d rotationMatrix = new Matrix4d();
                rotationMatrix.setRotateAxis((angle - angleLimit) * 0.9, rotationAxis.x, rotationAxis.y, rotationAxis.z);
                rotationMatrix.multiplyDirection(newDirection);
            }

            newDirection.scale(i == 2
                ? b.distanceTo(c) // Don't enforce `nodeSpacing` between the buffer node and the node after it.
                : nodeSpacing     // Do enforce `nodeSpacing` between all nodes past the buffer node.
            );


            final Vector3d deltaPosition = b.clone()
                .add(newDirection) // Get the target position.
                .subtract(c)       // Convert it to an offset from the old `c`.
                .scale(1 - t);  // Scale down the offset proportionally to how close the node is to the tail.
            serpent.guideRail.set(i, c.clone().add(deltaPosition));
        }
    }

    private static void solveJoints(final Serpent serpent) {
        int guideRailIndex = 0;
        double remainder = 0.0;

        // `i = 2` because we want to start at the first joint after the neck.
        for (int i = 1; i < serpent.joints.length; i++) {
            final double boneLength = serpent.getBoneLength(i - 1);

            // Account for how far along the `guideRail` segment the previous joint left off.
            double distLeft = remainder + boneLength;
            for (; guideRailIndex < serpent.guideRail.size() - 1; guideRailIndex++) {
                final Vector3d thisPathNode = serpent.guideRail.get(guideRailIndex);
                final Vector3d nextPathNode = serpent.guideRail.get(guideRailIndex + 1);
                final Vector3d pathSegment = nextPathNode.clone().subtract(thisPathNode);
                final double pathSegmentLength = pathSegment.length();
                // See if the joint should be placed along this guideRail segment.
                if (pathSegmentLength > distLeft) {
                    if (i > 1) {
                        // Normalize `pathSegment`.
                        final Vector3d pathSegmentDirection = pathSegment.clone().scale(1 / pathSegmentLength);
                        // Interpolate the joint along the segment.
                        serpent.joints[i].position.assign(thisPathNode.clone().add(pathSegmentDirection.clone().scale(distLeft)));
                    }
                    // Save how far into the segment we left off so that the next joint can continue from there.
                    remainder = distLeft;
                    // Next joint!
                    break;
                }
                // Next path segment!
                distLeft -= pathSegmentLength;
            }
        }

        // Remove `guideRail` nodes when the tail joint passes the node before them.
        if (guideRailIndex < serpent.guideRail.size() - 2) {
            serpent.guideRail.subList(guideRailIndex + 2, serpent.guideRail.size()).clear();
        }
    }

    private static double getAngleBetween(final Vector3d v1, final Vector3d v2) {
        return Math.acos(Math.clamp(v1.dot(v2), -1.0, 1.0));
    }
}
