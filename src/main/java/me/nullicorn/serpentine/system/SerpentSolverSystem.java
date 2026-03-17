package me.nullicorn.serpentine.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.systems.SteeringSystem;
import me.nullicorn.serpentine.component.Serpent;
import me.nullicorn.serpentine.solver.SerpentBoneSolver;
import me.nullicorn.serpentine.solver.SerpentJointSolver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

/**
 * Runs each tick to solve for the new positions of {@link Serpent#joints() joints} and {@link Serpent#bones() bones} in
 * each {@link Serpent} entity.
 */
public final class SerpentSolverSystem extends EntityTickingSystem<EntityStore> {
    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        final NPCPlugin npcPlugin = NPCPlugin.get();
        if (npcPlugin != null && npcPlugin.isEnabled()) {
            return Collections.singleton(
                // Run after `SteeringSystem` so that NPCs are in their new positions by the time we run. Otherwise, if
                // the solver reads the head entity's position, it will be 1 tick old, causing the body to lag behind
                // the head.
                new SystemDependency<>(Order.AFTER, SteeringSystem.class) // The system that moves NPCs.
            );
        }

        return Collections.emptySet();
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

        final SerpentJointSolver jointSolver = serpent.jointSolver();
        if (jointSolver != null) {
            jointSolver.tick(serpent, serpentRef, dt, commandBuffer);
        }

        final SerpentBoneSolver boneSolver = serpent.boneSolver();
        if (boneSolver != null) {
            boneSolver.tick(serpent, serpentRef, dt, commandBuffer);
        }
    }
}
