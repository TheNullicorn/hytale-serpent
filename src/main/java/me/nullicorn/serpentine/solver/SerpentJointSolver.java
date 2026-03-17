package me.nullicorn.serpentine.solver;

import com.hypixel.hytale.codec.lookup.BuilderCodecMapCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.serpentine.component.Serpent;

public interface SerpentJointSolver {
    BuilderCodecMapCodec<SerpentJointSolver> CODEC = new BuilderCodecMapCodec<>("Type");

    void tick(
        Serpent serpent,
        Ref<EntityStore> serpentRef,
        float dt,
        ComponentAccessor<EntityStore> componentAccessor
    );

    default void init(final Serpent serpent) {
    }

    SerpentJointSolver clone();
}
