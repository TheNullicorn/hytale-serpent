package me.nullicorn.hytale.serpent.system;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.hytale.serpent.component.Serpent;
import me.nullicorn.hytale.serpent.component.SerpentSegment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SerpentAddRemoveSystem extends RefSystem<EntityStore> {
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

        final Model headModel = Model.createUnitScaleModel(serpent.config.getHead().getModel());
        final Model bodyModel = Model.createUnitScaleModel(serpent.config.getBody().getModel());
        final Model tailModel = Model.createUnitScaleModel(serpent.config.getTail().getModel());

        //noinspection unchecked
        serpent.segments = new Ref[serpent.joints.length - 1];

        for (int i = 0; i < serpent.segments.length; i++) {
            final TransformComponent transform = new TransformComponent(serpent.joints[i + 1].position.clone(), new Vector3f());
            final SerpentSegment segment = new SerpentSegment(ref, i);
            if (i == 0) {
                commandBuffer.addComponent(ref, TransformComponent.getComponentType(), transform);
                commandBuffer.addComponent(ref, ModelComponent.getComponentType(), new ModelComponent(headModel));
                commandBuffer.addComponent(ref, SerpentSegment.getComponentType(), segment);
                serpent.segments[i] = ref;
            } else {
                final Holder<EntityStore> holder = store.getRegistry().newHolder();
                holder.addComponent(TransformComponent.getComponentType(), transform);
                holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
                holder.addComponent(UUIDComponent.getComponentType(), UUIDComponent.randomUUID());
                holder.addComponent(ModelComponent.getComponentType(),
                    new ModelComponent(i == serpent.segments.length - 1
                        ? tailModel
                        : bodyModel
                    )
                );
                holder.addComponent(store.getRegistry().getNonSerializedComponentType(), NonSerialized.get());
                holder.addComponent(SerpentSegment.getComponentType(), segment);
                serpent.segments[i] = commandBuffer.addEntity(holder, reason);
            }
        }
    }

    @Override
    public void onEntityRemove(
        @Nonnull final Ref<EntityStore> ref,
        @Nonnull final RemoveReason reason,
        @Nonnull final Store<EntityStore> store,
        @Nonnull final CommandBuffer<EntityStore> commandBuffer
    ) {
        final Serpent serpent = commandBuffer.getComponent(ref, Serpent.getComponentType());
        assert serpent != null;

        if (serpent.segments == null) {
            return;
        }

        for (final Ref<EntityStore> segmentRef : serpent.segments) {
            if (segmentRef == null || segmentRef.equals(ref) || !segmentRef.isValid()) {
                continue;
            }
            commandBuffer.removeEntity(segmentRef, reason);
        }
    }
}
