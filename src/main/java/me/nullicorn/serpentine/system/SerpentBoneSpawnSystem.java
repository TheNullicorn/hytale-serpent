package me.nullicorn.serpentine.system;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.serpentine.component.*;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

/**
 * Runs each tick to spawn any missing {@link SerpentBone} entities for each {@link Serpent} entity.
 * <p>
 * Bones can be opted out of this behaviour using {@link Serpent.Bone#setAutoSpawn(boolean)}.
 */
public final class SerpentBoneSpawnSystem extends EntityTickingSystem<EntityStore> {
    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(
            new SystemDependency<>(Order.AFTER, SerpentSolverSystem.class)
        );
    }

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

        for (int i = 0; i < serpent.bones().size(); i++) {
            final Serpent.Bone bone = serpent.bones().get(i);
            if (!bone.isAutoSpawn() || (bone.ref() != null && bone.ref().isValid())) {
                continue;
            }

            final Vector3d position = bone.transform().getPosition();
            if (position.y < -32) {
                // Don't spawn the bone if it's below the world.
                continue;
            }

            final long chunkIndex = ChunkUtil.indexChunkFromBlock(position.x, position.z);
            final WorldChunk chunk = store.getExternalData().getWorld().getChunkIfLoaded(chunkIndex);
            if (chunk == null) {
                // Don't spawn the bone if it's in an unloaded chunk.
                continue;
            }

            final Holder<EntityStore> holder = store.getRegistry().newHolder();
            holder.addComponent(SerpentBone.getComponentType(), new SerpentBone(serpentRef, i));
            holder.addComponent(SerpentBoneAutoApplyTransform.getComponentType(), SerpentBoneAutoApplyTransform.get());
            holder.addComponent(SerpentBoneAutoApplyScale.getComponentType(), SerpentBoneAutoApplyScale.get());
            holder.addComponent(SerpentBoneAutoApplyModel.getComponentType(), SerpentBoneAutoApplyModel.get());
            holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
            holder.addComponent(UUIDComponent.getComponentType(), UUIDComponent.randomUUID());
            holder.addComponent(store.getRegistry().getNonSerializedComponentType(), NonSerialized.get());

            final Ref<EntityStore> boneRef = commandBuffer.addEntity(holder, AddReason.SPAWN);
            bone.setRef(boneRef);
        }
    }
}
