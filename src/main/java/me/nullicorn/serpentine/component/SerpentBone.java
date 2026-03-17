package me.nullicorn.serpentine.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.serpentine.SerpentPlugin;

public final class SerpentBone implements Component<EntityStore> {
    private final Ref<EntityStore> serpent;
    private final int index;

    public static ComponentType<EntityStore, SerpentBone> getComponentType() {
        return SerpentPlugin.get().getSerpentBoneComponentType();
    }

    public SerpentBone(final Ref<EntityStore> serpent, final int index) {
        this.serpent = serpent;
        this.index = index;
    }

    public Ref<EntityStore> serpent() {
        return this.serpent;
    }

    public int index() {
        return this.index;
    }

    @Override
    public Component<EntityStore> clone() {
        return new SerpentBone(this.serpent, this.index);
    }
}
