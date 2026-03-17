package me.nullicorn.serpentine.solver;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.matrix.Matrix4d;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.serpentine.component.Serpent;

import java.util.ArrayList;
import java.util.List;

public final class DefaultSerpentJointSolver implements SerpentJointSolver {
    /**
     * ID of this type's codec in {@link SerpentJointSolver#CODEC}.
     */
    public static final String ID = "Default";
    public static final BuilderCodec<DefaultSerpentJointSolver> CODEC = BuilderCodec.builder(DefaultSerpentJointSolver.class, DefaultSerpentJointSolver::new).build();

    private final List<Vector3d> guideRail = new ArrayList<>();
    private double nodeSpacing;

    @Override
    public void init(final Serpent serpent) {
        this.guideRail.clear();
        for (final Serpent.Joint joint : serpent.joints()) {
            this.guideRail.add(joint.position().clone());
        }
        this.recalculateNodeSpacing(serpent);
    }

    @Override
    public void tick(
        final Serpent serpent,
        final Ref<EntityStore> serpentRef,
        final float dt,
        final ComponentAccessor<EntityStore> componentAccessor
    ) {
        this.moveHeadJoints(serpent, serpentRef, componentAccessor);
        this.moveHeadGuideNodes(serpent);
        this.solveGuideRail();
        this.solveJoints(serpent);
    }

    @Override
    public SerpentJointSolver clone() {
        final DefaultSerpentJointSolver clone = new DefaultSerpentJointSolver();
        for (final Vector3d node : this.guideRail) {
            clone.guideRail.add(node.clone());
        }
        clone.nodeSpacing = this.nodeSpacing;
        return clone;
    }

    private void recalculateNodeSpacing(final Serpent serpent) {
        double minLength = Double.POSITIVE_INFINITY;
        for (int i = 0; i < serpent.bones().size(); i++) {
            final Serpent.Bone bone = serpent.bones().get(i);
            minLength = Math.min(minLength, bone.baseLength() * bone.scale());
        }
        this.nodeSpacing = minLength * 1.5;
    }

    private void moveHeadJoints(
        final Serpent serpent,
        final Ref<EntityStore> serpentRef,
        final ComponentAccessor<EntityStore> componentAccessor
    ) {
        final Serpent.Bone headBone = serpent.bones().getFirst();
        final TransformComponent headTransform = componentAccessor.getComponent(serpentRef, TransformComponent.getComponentType());
        final HeadRotation headRotation = componentAccessor.getComponent(serpentRef, HeadRotation.getComponentType());
        assert headTransform != null;
        assert headRotation != null;

        final Vector3d headJointsOffset = headRotation.getDirection().scale(headBone.baseLength() * headBone.scale() * 0.5);
        serpent.joints().get(0).position().assign(headTransform.getPosition().clone().add(headJointsOffset));
        serpent.joints().get(1).position().assign(headTransform.getPosition().clone().subtract(headJointsOffset));
    }

    private void moveHeadGuideNodes(final Serpent serpent) {
        final Vector3d headFrontNode = this.guideRail.get(0);
        final Vector3d headRearNode = this.guideRail.get(1);
        headFrontNode.assign(serpent.joints().get(0).position());
        headRearNode.assign(serpent.joints().get(1).position());

        if (this.guideRail.size() > 2) {
            final Vector3d bufferNode = this.guideRail.get(2);
            // If the head is too far from the `bufferNode`, create a new node between them. This becomes the new
            // `bufferNode`, and the old one is bumped back a slot.
            if (headRearNode.distanceTo(bufferNode) > this.nodeSpacing) {
                final Vector3d offset = headRearNode.clone().subtract(bufferNode).setLength(this.nodeSpacing);
                this.guideRail.add(2, bufferNode.clone().add(offset));
            }
        }
    }

    private void solveGuideRail() {
        for (int i = 2; i < this.guideRail.size(); i++) {
            final Vector3d a = this.guideRail.get(i - 2);
            final Vector3d b = this.guideRail.get(i - 1);
            final Vector3d c = this.guideRail.get(i);
            final Vector3d prevDirection = Vector3d.directionTo(a, b);
            final Vector3d thisDirection = Vector3d.directionTo(b, c);
            final Vector3d newDirection = thisDirection.clone();

            // Get how far along `guideRail` this node is, as a percentage.
            final double t = (double) i / (this.guideRail.size() - 1);

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
                ? b.distanceTo(c)  // Don't enforce `nodeSpacing` between the buffer node and the node after it.
                : this.nodeSpacing // Do enforce `nodeSpacing` between all nodes past the buffer node.
            );


            final Vector3d deltaPosition = b.clone()
                .add(newDirection) // Get the target position.
                .subtract(c)       // Convert it to an offset from the old `c`.
                .scale(1 - t);  // Scale down the offset proportionally to how close the node is to the tail.
            this.guideRail.set(i, c.clone().add(deltaPosition));
        }
    }

    private void solveJoints(final Serpent serpent) {
        int guideRailIndex = 0;
        double remainder = 0.0;

        // `i = 2` because we want to start at the first joint after the neck.
        for (int i = 1; i < serpent.joints().size(); i++) {
            final Serpent.Bone bone = serpent.bones().get(i - 1);
            final double boneLength = bone.baseLength() * bone.scale();

            // Account for how far along the `guideRail` segment the previous joint left off.
            double distLeft = remainder + boneLength;
            for (; guideRailIndex < this.guideRail.size() - 1; guideRailIndex++) {
                final Vector3d thisPathNode = this.guideRail.get(guideRailIndex);
                final Vector3d nextPathNode = this.guideRail.get(guideRailIndex + 1);
                final Vector3d pathSegment = nextPathNode.clone().subtract(thisPathNode);
                final double pathSegmentLength = pathSegment.length();
                // See if the joint should be placed along this guideRail segment.
                if (pathSegmentLength > distLeft) {
                    if (i > 1) {
                        // Normalize `pathSegment`.
                        final Vector3d pathSegmentDirection = pathSegment.clone().scale(1 / pathSegmentLength);
                        // Interpolate the joint along the segment.
                        serpent.joints().get(i).position().assign(thisPathNode.clone().add(pathSegmentDirection.clone().scale(distLeft)));
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
        if (guideRailIndex < this.guideRail.size() - 2) {
            this.guideRail.subList(guideRailIndex + 2, this.guideRail.size()).clear();
        }
    }

    private static double getAngleBetween(final Vector3d v1, final Vector3d v2) {
        return Math.acos(Math.clamp(v1.dot(v2), -1.0, 1.0));
    }
}
