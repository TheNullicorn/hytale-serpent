package me.nullicorn.hytale.serpent.system;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.system.UpdateLocationSystems;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.hytale.serpent.component.Serpent;
import me.nullicorn.hytale.serpent.component.SerpentBone;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

/**
 * Runs each tick to update the transforms of {@link SerpentBone} entities and spawn in new ones when they move into
 * loaded chunks.
 */
public final class SerpentBoneLoadAndTransformSystem extends EntityTickingSystem<EntityStore> {
    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(
            new SystemDependency<>(Order.BEFORE, UpdateLocationSystems.TickingSystem.class)
        );
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Serpent.getComponentType();
    }

    @Override
    public void tick(
        final float dt,
        final int index,
        @Nonnull final ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull final Store<EntityStore> store,
        @Nonnull final CommandBuffer<EntityStore> commandBuffer
    ) {
        final Ref<EntityStore> serpentRef = archetypeChunk.getReferenceTo(index);
        final Serpent serpent = archetypeChunk.getComponent(index, Serpent.getComponentType());
        assert serpent != null;

        for (int boneIndex = 0; boneIndex < serpent.bones.length; boneIndex++) {
            final Ref<EntityStore> boneRef = serpent.bones[boneIndex];
            if (boneRef != null && boneRef.isValid()) {
                final TransformComponent transform = commandBuffer.getComponent(boneRef, TransformComponent.getComponentType());
                if (transform != null) {
                    final Transform newTransform = serpent.getBoneTransform(boneIndex);
                    transform.setPosition(newTransform.getPosition());
                    transform.setRotation(newTransform.getRotation());
                }
                continue;
            }

            final Transform boneTransform = serpent.getBoneTransform(boneIndex);
            if (boneTransform.getPosition().y < -32) {
                // Don't spawn the bone if it's below the world. See UpdateLocationSystems#updateLocation for reference.
                continue;
            }

            final long chunkIndex = ChunkUtil.indexChunkFromBlock(boneTransform.getPosition().x, boneTransform.getPosition().z);
            final WorldChunk chunk = store.getExternalData().getWorld().getChunkIfLoaded(chunkIndex);
            if (chunk == null) {
                // Don't spawn the bone if it's in an unloaded chunk.
                continue;
            }

            spawnBone(boneIndex, boneTransform, serpent, serpentRef, commandBuffer);
        }
    }

    private static void spawnBone(
        final int boneIndex,
        final Transform boneTransform,
        final Serpent serpent,
        final Ref<EntityStore> serpentRef,
        final ComponentAccessor<EntityStore> componentAccessor
    ) {
        final Model model = Model.createUnitScaleModel(serpent.getBoneConfig(boneIndex).getModel());
        final Holder<EntityStore> holder = componentAccessor.getExternalData().getStore().getRegistry().newHolder();
        holder.addComponent(SerpentBone.getComponentType(), new SerpentBone(serpentRef, boneIndex));
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(componentAccessor.getExternalData().takeNextNetworkId()));
        holder.addComponent(UUIDComponent.getComponentType(), UUIDComponent.randomUUID());
        holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(boneTransform.getPosition(), boneTransform.getRotation()));
        holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
        holder.addComponent(componentAccessor.getExternalData().getStore().getRegistry().getNonSerializedComponentType(), NonSerialized.get());
        // Spawn the bone and store its `Ref` inside the `Serpent`.
        serpent.bones[boneIndex] = componentAccessor.addEntity(holder, AddReason.LOAD);
    }
}
