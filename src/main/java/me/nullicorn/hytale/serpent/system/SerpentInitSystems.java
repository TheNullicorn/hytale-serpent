package me.nullicorn.hytale.serpent.system;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.HolderSystem;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.hytale.serpent.component.Serpent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SerpentInitSystems {
    private static void initHeadEntity(
        final Serpent serpent,
        final Ref<EntityStore> ref,
        final ComponentAccessor<EntityStore> componentAccessor
    ) {
        serpent.bones[0] = ref;

        final Model headModel = Model.createUnitScaleModel(serpent.getBoneConfig(0).getModel());
        componentAccessor.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(headModel));

        if (componentAccessor.getComponent(ref, NetworkId.getComponentType()) == null) {
            componentAccessor.addComponent(ref, NetworkId.getComponentType(), new NetworkId(componentAccessor.getExternalData().takeNextNetworkId()));
        }
    }

    /**
     * Initializes {@link Serpent} entities before they have been added to a world.
     */
    public static final class PreSpawnSystem extends HolderSystem<EntityStore> {
        @Nullable
        @Override
        public Query<EntityStore> getQuery() {
            return Serpent.getComponentType();
        }

        @Override
        public void onEntityAdd(
            @Nonnull final Holder<EntityStore> holder,
            @Nonnull final AddReason reason,
            @Nonnull final Store<EntityStore> store
        ) {
            final Serpent serpent = holder.getComponent(Serpent.getComponentType());
            assert serpent != null;

            if (holder.getComponent(NetworkId.getComponentType()) == null) {
                holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
            }

            if (holder.getComponent(HeadRotation.getComponentType()) == null) {
                holder.addComponent(HeadRotation.getComponentType(), new HeadRotation());
            }

            final Transform headTransform = serpent.getBoneTransform(0);
            final TransformComponent actualTransform = holder.getComponent(TransformComponent.getComponentType());
            if (actualTransform == null) {
                // Entity doesn't have a transform of its own, so give it the serpent's head transform.
                holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(headTransform.getPosition(), headTransform.getRotation()));
            } else {
                // Entity already has a transform. Move the entire serpent to that position.
                final Vector3d difference = headTransform.getPosition().clone().subtract(actualTransform.getPosition());
                if (difference.length() > 0.0001) {
                    for (final Serpent.Joint joint : serpent.joints) {
                        joint.position.add(difference);
                    }
                }
                // Ignore the existing rotation. Replace it with our head rotation.
                actualTransform.setRotation(headTransform.getRotation());
            }
        }

        @Override
        public void onEntityRemoved(
            @Nonnull final Holder<EntityStore> holder,
            @Nonnull final RemoveReason reason,
            @Nonnull final Store<EntityStore> store
        ) {
        }
    }

    /**
     * Initializes {@link Serpent} entities after they have been added to a world.
     */
    public static final class SpawnSystem extends RefSystem<EntityStore> {
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
            initHeadEntity(serpent, ref, commandBuffer);
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

    /**
     * Initializes {@link Serpent} entities made from existing entities.
     */
    public static final class ChangeSystem extends RefChangeSystem<EntityStore, Serpent> {
        @Nonnull
        @Override
        public ComponentType<EntityStore, Serpent> componentType() {
            return Serpent.getComponentType();
        }

        @Nullable
        @Override
        public Query<EntityStore> getQuery() {
            return Query.any();
        }

        @Override
        public void onComponentAdded(
            @Nonnull final Ref<EntityStore> ref,
            @Nonnull final Serpent component,
            @Nonnull final Store<EntityStore> store,
            @Nonnull final CommandBuffer<EntityStore> commandBuffer
        ) {
            initHeadEntity(component, ref, commandBuffer);
        }

        @Override
        public void onComponentSet(
            @Nonnull final Ref<EntityStore> ref,
            @Nullable final Serpent oldComponent,
            @Nonnull final Serpent newComponent,
            @Nonnull final Store<EntityStore> store,
            @Nonnull final CommandBuffer<EntityStore> commandBuffer
        ) {
            initHeadEntity(newComponent, ref, commandBuffer);
        }

        @Override
        public void onComponentRemoved(
            @Nonnull final Ref<EntityStore> ref,
            @Nonnull final Serpent component,
            @Nonnull final Store<EntityStore> store,
            @Nonnull final CommandBuffer<EntityStore> commandBuffer
        ) {
            final PlayerSkinComponent playerSkinComponent = commandBuffer.getComponent(ref, PlayerSkinComponent.getComponentType());
            if (playerSkinComponent == null) {
                // This entity probably isn't a player.
                return;
            }

            // Schedule the skin to be resynced with clients.
            playerSkinComponent.setNetworkOutdated();

            // Validate the skin and make a player model for it.
            final Model playerModel = CosmeticsModule.get().createModel(playerSkinComponent.getPlayerSkin());
            if (playerModel == null) {
                return;
            }

            // Assign the new model to the player.
            commandBuffer.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(playerModel));
        }
    }
}
