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

        for (int i = 0; i < serpent.bones.length; i++) {
            final Ref<EntityStore> boneRef = serpent.bones[i];
            if (boneRef != null && boneRef.isValid()) {
                final TransformComponent transform = commandBuffer.getComponent(boneRef, TransformComponent.getComponentType());
                if (transform != null) {
                    final Transform newTransform = serpent.getBoneTransform(i);
                    transform.setPosition(newTransform.getPosition());
                    transform.setRotation(newTransform.getRotation());
                }
                continue;
            }
            final Transform transform = serpent.getBoneTransform(i);

            if (transform.getPosition().y < -32) {
                // Don't spawn the bone if it's below the world. See UpdateLocationSystems#updateLocation for reference.
                continue;
            }

            final long chunkIndex = ChunkUtil.indexChunkFromBlock(transform.getPosition().x, transform.getPosition().z);
            final WorldChunk chunk = store.getExternalData().getWorld().getChunkIfLoaded(chunkIndex);
            if (chunk == null) {
                // Don't spawn the bone if it's in an unloaded chunk.
                continue;
            }

            final Model model = Model.createUnitScaleModel(serpent.getBoneConfig(i).getModel());
            final Holder<EntityStore> holder = store.getRegistry().newHolder();
            holder.addComponent(SerpentBone.getComponentType(), new SerpentBone(serpentRef, i));
            holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
            holder.addComponent(UUIDComponent.getComponentType(), UUIDComponent.randomUUID());
            holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(transform.getPosition(), transform.getRotation()));
            holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
            holder.addComponent(store.getRegistry().getNonSerializedComponentType(), NonSerialized.get());
            // Spawn the bone and store its `Ref` inside the `Serpent`.
            serpent.bones[i] = commandBuffer.addEntity(holder, AddReason.LOAD);
        }
    }
}
