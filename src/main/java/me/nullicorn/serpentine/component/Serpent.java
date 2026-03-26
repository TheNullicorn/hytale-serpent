package me.nullicorn.serpentine.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3dUtil;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.serpentine.SerpentPlugin;
import me.nullicorn.serpentine.asset.SerpentBoneConfig;
import me.nullicorn.serpentine.asset.SerpentLayoutBone;
import me.nullicorn.serpentine.solver.DefaultSerpentBoneSolver;
import me.nullicorn.serpentine.solver.DefaultSerpentJointSolver;
import me.nullicorn.serpentine.solver.SerpentBoneSolver;
import me.nullicorn.serpentine.solver.SerpentJointSolver;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Serpent implements Component<EntityStore> {
    public static final String ID = "Serpent";

    public static final BuilderCodec<Serpent> CODEC = BuilderCodec.builder(Serpent.class, Serpent::new)
        .append(
            new KeyedCodec<>("Joints", new ArrayCodec<>(Joint.CODEC, Joint[]::new), true),
            (o, s) -> o.joints.addAll(Arrays.asList(s)),
            (o) -> o.joints.toArray(new Joint[0])
        )
        .addValidator(Validators.nonNull())
        .addValidator(Validators.arraySizeRange(2, Integer.MAX_VALUE)) // Need 2 joints per bone, and at least 1 bone.
        .add()
        .append(
            new KeyedCodec<>("Bones", new ArrayCodec<>(Bone.CODEC, Bone[]::new), true),
            (o, s) -> o.bones.addAll(Arrays.asList(s)),
            (o) -> o.bones.toArray(new Bone[0])
        )
        .addValidator(Validators.nonNull())
        .addValidator(Validators.nonEmptyArray()) // Need at least 1 bone (the head)
        .add()
        .append(
            new KeyedCodec<>("JointSolver", SerpentJointSolver.CODEC, false),
            (o, s) -> o.jointSolver = s,
            (o) -> o.jointSolver
        )
        .add()
        .append(
            new KeyedCodec<>("BoneSolver", SerpentBoneSolver.CODEC, false),
            (o, s) -> o.boneSolver = s,
            (o) -> o.boneSolver
        )
        .add()
        .afterDecode(serpent -> {
            if (serpent.jointSolver != null) {
                serpent.jointSolver.init(serpent);
            }
            if (serpent.boneSolver != null) {
                serpent.boneSolver.init(serpent);
            }
        })
        .build();

    public static ComponentType<EntityStore, Serpent> getComponentType() {
        return SerpentPlugin.get().getSerpentComponentType();
    }

    private final List<Joint> joints = new ArrayList<>();
    private final List<Bone> bones = new ArrayList<>();
    @Nullable
    private SerpentJointSolver jointSolver;
    @Nullable
    private SerpentBoneSolver boneSolver;

    private final List<Joint> jointsUnmodifiable = Collections.unmodifiableList(this.joints);
    private final List<Bone> bonesUnmodifiable = Collections.unmodifiableList(this.bones);

    public Serpent(
        final Transform headTransform,
        final List<SerpentLayoutBone> bones
    ) {
        if (bones.isEmpty()) {
            throw new IllegalArgumentException("Serpent requires at least 1 bone");
        }

        final SerpentLayoutBone headBone = bones.getFirst();
        final SerpentBoneConfig headBoneConfig = headBone.boneConfig();

        final Vector3d jointPosition = new Vector3d(headTransform.getPosition());
        if (headBoneConfig != null) {
            jointPosition.add(0, 0, headBoneConfig.length() * headBone.scale() * 0.5);
        }

        this.joints.add(new Joint(jointPosition));

        for (int i = 1; i <= bones.size(); i++) {
            final Vector3d prevJointPosition = new Vector3d(jointPosition);

            final SerpentLayoutBone layoutBone = bones.get(i - 1);
            final SerpentBoneConfig boneConfig = layoutBone.boneConfig();
            Model.ModelReference modelRef = null;
            if (boneConfig != null) {
                jointPosition.sub(0, 0, boneConfig.length() * layoutBone.scale());

                final ModelAsset modelAsset = boneConfig.model();
                if (modelAsset != null) {
                    modelRef = Model.createUnitScaleModel(modelAsset).toReference();
                }
            }

            this.joints.add(new Joint(jointPosition));
            this.bones.add(new Bone(modelRef, new Transform(prevJointPosition.add(jointPosition).div(2)), boneConfig != null ? boneConfig.length() : 0.0, layoutBone.scale()));
        }

        this.setJointSolver(new DefaultSerpentJointSolver());
        this.setBoneSolver(new DefaultSerpentBoneSolver());
    }

    private Serpent() {
        // Only intended for codec.
    }

    /**
     * @return an unmodifiable view of the serpent's joint list.
     */
    public List<Joint> joints() {
        return this.jointsUnmodifiable;
    }

    /**
     * @return an unmodifiable view of the serpent's bone list.
     */
    public List<Bone> bones() {
        return this.bonesUnmodifiable;
    }

    @Nullable
    public SerpentJointSolver jointSolver() {
        return this.jointSolver;
    }

    @Nullable
    public SerpentBoneSolver boneSolver() {
        return this.boneSolver;
    }

    public void setJointSolver(@Nullable final SerpentJointSolver jointSolver) {
        this.jointSolver = jointSolver;
        if (this.jointSolver != null) {
            this.jointSolver.init(this);
        }
    }

    public void setBoneSolver(@Nullable final SerpentBoneSolver boneSolver) {
        this.boneSolver = boneSolver;
        if (this.boneSolver != null) {
            this.boneSolver.init(this);
        }
    }

    @Override
    public Component<EntityStore> clone() {
        final Serpent clone = new Serpent();
        for (final Joint joint : this.joints) {
            clone.joints.add(joint.clone());
        }
        for (final Bone bone : this.bones) {
            final Bone boneClone = bone.clone();
            boneClone.ref = null;
            clone.bones.add(boneClone);
        }
        if (this.jointSolver != null) {
            clone.jointSolver = this.jointSolver.clone();
        }
        if (this.boneSolver != null) {
            clone.boneSolver = this.boneSolver.clone();
        }
        return clone;
    }

    public static final class Joint {
        private static final BuilderCodec<Joint> CODEC = BuilderCodec.builder(Joint.class, Joint::new)
            .append(
                new KeyedCodec<>("Position", Vector3dUtil.CODEC, true),
                (o, s) -> o.position.set(s),
                (o) -> o.position
            )
            .addValidator(Validators.nonNull())
            .add()
            .build();

        private final Vector3d position = new Vector3d();

        private Joint(final Vector3dc position) {
            this.position.set(position);
        }

        private Joint() {
            // Only intended for codec.
        }

        public Vector3d position() {
            return this.position;
        }

        @Override
        public Joint clone() {
            return new Joint(this.position);
        }
    }

    public static final class Bone {
        public static final BuilderCodec<Bone> CODEC = BuilderCodec.builder(Bone.class, Bone::new)
            .append(
                new KeyedCodec<>("Model", Model.ModelReference.CODEC, false),
                (o, s) -> o.model = s,
                (o) -> o.model
            )
            .add()
            .append(
                new KeyedCodec<>("BaseLength", Codec.DOUBLE, true),
                (o, s) -> o.baseLength = s,
                (o) -> o.baseLength
            )
            .addValidator(Validators.nonNull())
            .add()
            .append(
                new KeyedCodec<>("Scale", Codec.DOUBLE, false),
                (o, s) -> o.scale = s,
                (o) -> o.scale
            )
            .addValidator(Validators.nonNull())
            .add()
            .append(
                new KeyedCodec<>("AutoSpawn", Codec.BOOLEAN, false),
                (o, s) -> o.autoSpawn = s,
                (o) -> o.autoSpawn
            )
            .add()
            .build();

        @Nullable
        private Ref<EntityStore> ref;
        @Nullable
        private Model.ModelReference model;
        private Transform transform = new Transform();
        private double baseLength;
        private double scale = 1.0;
        private boolean autoSpawn = true;

        private Bone(
            @Nullable final Model.ModelReference model,
            final Transform transform,
            final double baseLength,
            final double scale
        ) {
            this.model = model;
            this.transform = transform;
            this.baseLength = baseLength;
            this.scale = scale;
        }

        private Bone() {
            // Only intended for codec.
        }

        @Nullable
        public Ref<EntityStore> ref() {
            return this.ref;
        }

        @Nullable
        public Model.ModelReference model() {
            return this.model;
        }

        public Transform transform() {
            return this.transform;
        }

        public double baseLength() {
            return this.baseLength;
        }

        public double scale() {
            return this.scale;
        }

        public boolean isAutoSpawn() {
            return this.autoSpawn;
        }

        public void setRef(@Nullable final Ref<EntityStore> ref) {
            this.ref = ref;
        }

        public void setModel(@Nullable final Model.ModelReference model) {
            this.model = model;
        }

        public void setBaseLength(final double baseLength) {
            this.baseLength = baseLength;
        }

        public void setScale(final double scale) {
            this.scale = scale;
        }

        public void setAutoSpawn(final boolean autoSpawn) {
            this.autoSpawn = autoSpawn;
        }

        @Override
        public Bone clone() {
            final Bone clone = new Bone();
            clone.ref = this.ref;
            clone.model = this.model;
            clone.transform = this.transform.clone();
            clone.baseLength = this.baseLength;
            clone.scale = this.scale;
            clone.autoSpawn = this.autoSpawn;
            return clone;
        }
    }
}
