package me.nullicorn.hytale.serpent.system;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.hytale.serpent.component.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SerpentHeadSpawnSystems {
    private SerpentHeadSpawnSystems() {
    }

    private static void initHead(
        final Serpent serpent,
        final Ref<EntityStore> serpentRef,
        final ComponentAccessor<EntityStore> componentAccessor
    ) {
        // Make the `Serpent` entity itself a bone, the head bone.
        componentAccessor.putComponent(serpentRef, SerpentBone.getComponentType(), new SerpentBone(serpentRef, 0));
        // Update the head bone inside the `Serpent` to have a ref to itself.
        serpent.bones().getFirst().setRef(serpentRef);

        // Mark head bone to have its model and scale auto updated by default. Other plugins can remove these if needed.
        componentAccessor.putComponent(serpentRef, SerpentBoneAutoApplyModel.getComponentType(), SerpentBoneAutoApplyModel.get());
        componentAccessor.putComponent(serpentRef, SerpentBoneAutoApplyScale.getComponentType(), SerpentBoneAutoApplyScale.get());

        // Add transform components if missing.
        componentAccessor.ensureAndGetComponent(serpentRef, TransformComponent.getComponentType());
        componentAccessor.ensureAndGetComponent(serpentRef, HeadRotation.getComponentType());
    }

    public static final class SpawnRefSystem extends RefSystem<EntityStore> {
        @Nullable
        @Override
        public Query<EntityStore> getQuery() {
            return Serpent.getComponentType();
        }

        @Override
        public void onEntityAdded(
            @Nonnull final Ref<EntityStore> ref,
            @Nonnull final AddReason reason,
            @Nonnull final Store<EntityStore> store,
            @Nonnull final CommandBuffer<EntityStore> commandBuffer
        ) {
            final Serpent serpent = commandBuffer.getComponent(ref, Serpent.getComponentType());
            assert serpent != null;
            initHead(serpent, ref, commandBuffer);
        }

        @Override
        public void onEntityRemove(
            @Nonnull final Ref<EntityStore> ref,
            @Nonnull final RemoveReason reason,
            @Nonnull final Store<EntityStore> store,
            @Nonnull final CommandBuffer<EntityStore> commandBuffer
        ) {
        }
    }

    public static final class SpawnRefChangeSystem extends RefChangeSystem<EntityStore, Serpent> {
        @Nullable
        @Override
        public Query<EntityStore> getQuery() {
            return Serpent.getComponentType();
        }

        @Nonnull
        @Override
        public ComponentType<EntityStore, Serpent> componentType() {
            return Serpent.getComponentType();
        }

        @Override
        public void onComponentAdded(
            @Nonnull final Ref<EntityStore> ref,
            @Nonnull final Serpent component,
            @Nonnull final Store<EntityStore> store,
            @Nonnull final CommandBuffer<EntityStore> commandBuffer
        ) {
            initHead(component, ref, commandBuffer);
        }

        @Override
        public void onComponentSet(
            @Nonnull final Ref<EntityStore> ref,
            @Nullable final Serpent oldComponent,
            @Nonnull final Serpent newComponent,
            @Nonnull final Store<EntityStore> store,
            @Nonnull final CommandBuffer<EntityStore> commandBuffer
        ) {
            initHead(newComponent, ref, commandBuffer);
        }

        @Override
        public void onComponentRemoved(
            @Nonnull final Ref<EntityStore> ref,
            @Nonnull final Serpent component,
            @Nonnull final Store<EntityStore> store,
            @Nonnull final CommandBuffer<EntityStore> commandBuffer
        ) {
            // Housekeeping; remove the self-reference from the head bone to the serpent (both same entity).
            component.bones().getFirst().setRef(null);
            // Remove the `SerpentBone` component so that `SerpentBoneDespawnSystem` doesn't think the entity is a stray
            // bone and delete it. Also remove any other serpent-related components while we're at it.
            commandBuffer.tryRemoveComponent(ref, SerpentBone.getComponentType());
            commandBuffer.tryRemoveComponent(ref, SerpentBoneAutoApplyModel.getComponentType());
            commandBuffer.tryRemoveComponent(ref, SerpentBoneAutoApplyScale.getComponentType());
            commandBuffer.tryRemoveComponent(ref, SerpentBoneAutoApplyTransform.getComponentType());
        }
    }
}
