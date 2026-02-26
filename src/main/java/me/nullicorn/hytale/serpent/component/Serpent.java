package me.nullicorn.hytale.serpent.component;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.hytale.serpent.SerpentPlugin;
import me.nullicorn.hytale.serpent.asset.SerpentConfig;

public final class Serpent implements Component<EntityStore> {
    public static final String ID = "Serpent";
    public static final BuilderCodec<Serpent> CODEC = BuilderCodec.builder(Serpent.class, Serpent::new).build();

    public SerpentConfig config;
    public Vector3d target;
    public Joint[] joints;
    public Ref<EntityStore>[] segments;

    public static ComponentType<EntityStore, Serpent> getComponentType() {
        return SerpentPlugin.get().getSerpentComponentType();
    }

    public Serpent() { // TODO: Make private
    }

    public Serpent(final Vector3d[] joints, final SerpentConfig config) {
        if (joints.length < 2) {
            throw new IllegalArgumentException("joints must have at least 2 elements");
        }
        this.config = config;
        this.joints = new Joint[joints.length];
        for (int i = 0; i < joints.length; i++) {
            final Joint joint = new Joint();
            joint.position = joints[i].clone();
            this.joints[i] = joint;
        }
        this.target = joints[0].clone();
        //noinspection unchecked
        this.segments = new Ref[this.joints.length - 1];
    }

    public double getSegmentLength(final int index) {
        if (index < 0 || index >= this.joints.length) {
            throw new IndexOutOfBoundsException(index);
        }

        if (index == 0) {
            return this.config.getHead().getLength();
        }
        if (index < this.joints.length - 1) {
            return this.config.getBody().getLength();
        }
        return this.config.getTail().getLength();
    }

    @Override
    public Component<EntityStore> clone() {
        final Serpent clone = new Serpent();
        clone.config = this.config;
        // TODO!!!
        return clone;
    }

    public static final class Joint {
        /**
         * The joint's current position.
         */
        public Vector3d position;
        /**
         * The joint's current velocity.
         */
        public Vector3d velocity = new Vector3d();
    }
}
