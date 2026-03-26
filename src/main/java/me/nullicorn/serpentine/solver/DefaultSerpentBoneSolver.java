package me.nullicorn.serpentine.solver;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.serpentine.component.Serpent;
import org.joml.Vector3d;

import java.util.List;

public final class DefaultSerpentBoneSolver implements SerpentBoneSolver {
    /**
     * ID of this type's codec in {@link SerpentBoneSolver#CODEC}.
     */
    public static final String ID = "Default";
    public static final BuilderCodec<DefaultSerpentBoneSolver> CODEC = BuilderCodec.builder(DefaultSerpentBoneSolver.class, DefaultSerpentBoneSolver::new).build();

    @Override
    public void tick(
        final Serpent serpent,
        final Ref<EntityStore> serpentRef,
        final float dt,
        final ComponentAccessor<EntityStore> componentAccessor
    ) {
        final List<Serpent.Joint> joints = serpent.joints();
        final List<Serpent.Bone> bones = serpent.bones();

        for (int i = 0; i < serpent.bones().size(); i++) {
            final Serpent.Bone bone = bones.get(i);
            final double length = bone.baseLength() * bone.scale();

            final Vector3d direction = new Vector3d(joints.get(i).position()).sub(joints.get(i + 1).position()).normalize();
            final Vector3d offset = new Vector3d(direction).mul(length / 2.0);
            final Vector3d position = new Vector3d(joints.get(i + 1).position()).add(offset);
            bone.transform().setPosition(position);

            // Only calculate rotation for bones spawned in the world.
            if (bone.ref() != null && bone.ref().isValid()) {
                final Rotation3f rotation = new Rotation3f();
                rotation.setPitch((float) Math.asin(direction.y));
                rotation.setYaw((float) (Math.atan2(direction.x, direction.z) + Math.PI));
                bone.transform().setRotation(rotation);
            }
        }
    }

    @Override
    public SerpentBoneSolver clone() {
        return new DefaultSerpentBoneSolver();
    }
}
