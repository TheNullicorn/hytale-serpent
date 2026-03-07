package me.nullicorn.hytale.serpent.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.hytale.serpent.SerpentPlugin;
import me.nullicorn.hytale.serpent.asset.SerpentBoneConfig;
import me.nullicorn.hytale.serpent.asset.SerpentConfig;

import java.util.ArrayList;
import java.util.List;

public final class Serpent implements Component<EntityStore> {
    public static final String ID = "Serpent";

    @SuppressWarnings("unchecked") // for initializing `bones` array which has generics (Ref<EntityStore>).
    public static final BuilderCodec<Serpent> CODEC = BuilderCodec.builder(Serpent.class, Serpent::new)
        .append(
            new KeyedCodec<>("Config", Codec.STRING, true),
            (serpent, s) -> serpent.configAssetId = s,
            (serpent) -> serpent.configAssetId
        )
        .addValidator(Validators.nonNull())
        .addValidator(SerpentConfig.VALIDATOR_CACHE.getValidator())
        .add()
        .append(
            new KeyedCodec<>("Joints", new ArrayCodec<>(Joint.CODEC, Joint[]::new), true),
            (serpent, s) -> {
                serpent.joints = s;
                serpent.bones = new Ref[serpent.joints.length - 1];
            },
            (serpent) -> serpent.joints
        )
        .addValidator(Validators.nonNull())
        .addValidator(Validators.arraySizeRange(2, Integer.MAX_VALUE))
        .add()
        .afterDecode(serpent -> {
            serpent.config = SerpentConfig.getAssetMap().getAsset(serpent.configAssetId);
            serpent.resetPath();
        })
        .build();

    private String configAssetId;
    public Joint[] joints;

    private SerpentConfig config;
    public Ref<EntityStore>[] bones;
    public List<Vector3d> path = new ArrayList<>();

    public static ComponentType<EntityStore, Serpent> getComponentType() {
        return SerpentPlugin.get().getSerpentComponentType();
    }

    private Serpent() {
    }

    @SuppressWarnings("unchecked") // for initializing `bones` array which has generics (Ref<EntityStore>).
    public Serpent(final Vector3d[] joints, final SerpentConfig config) {
        if (joints.length < 2) {
            throw new IllegalArgumentException("joints must have at least 2 elements");
        }
        this.config = config;
        this.configAssetId = this.config.getId();
        this.joints = new Joint[joints.length];
        for (int i = 0; i < joints.length; i++) {
            final Joint joint = new Joint();
            joint.position.assign(joints[i]);
            this.joints[i] = joint;
        }
        this.bones = new Ref[this.joints.length - 1];

        this.resetPath();
    }

    public SerpentConfig getConfig() {
        return this.config;
    }

    public void setConfig(final SerpentConfig config) {
        this.config = config;
        this.configAssetId = this.config.getId();
    }

    public SerpentBoneConfig getBoneConfig(final int index) {
        if (index < 0 || index >= this.bones.length) {
            throw new IndexOutOfBoundsException(index);
        }
        if (index == 0) {
            return this.config.getHead();
        }
        if (index < this.bones.length - 1) {
            return this.config.getBody();
        }
        return this.config.getTail();
    }

    public Transform getBoneTransform(final int index) {
        if (index < 0 || index >= this.bones.length) {
            throw new IndexOutOfBoundsException(index);
        }

        final Vector3d direction = this.joints[index].position.clone().subtract(this.joints[index + 1].position).normalize();
        final double length = this.getBoneConfig(index).getLength();

        final Vector3d offset = direction.clone().scale(length / 2.0);
        final Vector3d position = this.joints[index + 1].position.clone().add(offset);

        final Vector3f rotation = new Vector3f();
        rotation.setPitch((float) Math.asin(direction.y));
        rotation.setYaw((float) (Math.atan2(direction.x, direction.z) + Math.PI));

        return new Transform(position, rotation);
    }

    @Override
    @SuppressWarnings("unchecked") // for initializing `bones` array which has generics (Ref<EntityStore>).
    public Component<EntityStore> clone() {
        final Serpent clone = new Serpent();
        clone.config = this.config;
        clone.configAssetId = clone.config.getId();
        clone.joints = new Joint[this.joints.length];
        clone.bones = new Ref[this.bones.length];
        for (int i = 0; i < this.joints.length; i++) {
            clone.joints[i] = this.joints[i].clone();
        }
        clone.resetPath();
        return clone;
    }

    private void resetPath() {
        if (this.joints == null) {
            return;
        }
        this.path.clear();
        for (final Joint joint : this.joints) {
            this.path.add(joint.position.clone());
        }
    }

    public static final class Joint implements Cloneable {
        public static final BuilderCodec<Joint> CODEC = BuilderCodec.builder(Joint.class, Joint::new)
            .append(
                new KeyedCodec<>("Position", Vector3d.CODEC, true),
                (joint, s) -> joint.position.assign(s),
                (joint) -> joint.position
            )
            .addValidator(Validators.nonNull())
            .add()
            .build();

        public final Vector3d position = new Vector3d();

        @Override
        protected Joint clone() {
            final Joint clone = new Joint();
            clone.position.assign(this.position);
            return clone;
        }
    }
}
