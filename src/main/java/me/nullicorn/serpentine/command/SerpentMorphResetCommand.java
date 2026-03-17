package me.nullicorn.serpentine.command;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.serpentine.component.Serpent;

import javax.annotation.Nonnull;

public final class SerpentMorphResetCommand extends AbstractPlayerCommand {
    public SerpentMorphResetCommand() {
        super("reset", "server.commands.serpent.morph.reset.desc");
    }

    @Override
    protected void execute(
        @Nonnull final CommandContext context,
        @Nonnull final Store<EntityStore> store,
        @Nonnull final Ref<EntityStore> ref,
        @Nonnull final PlayerRef playerRef,
        @Nonnull final World world
    ) {
        store.removeComponentIfExists(ref, Serpent.getComponentType());
        resetPlayerScale(ref, store);
        resetPlayerModel(ref, store);
    }

    private static void resetPlayerScale(
        final Ref<EntityStore> ref,
        final ComponentAccessor<EntityStore> componentAccessor
    ) {
        final EntityScaleComponent scaleComponent = componentAccessor.getComponent(ref, EntityScaleComponent.getComponentType());
        if (scaleComponent == null) {
            return;
        }
        scaleComponent.setScale(1.0f);
    }

    private static void resetPlayerModel(
        final Ref<EntityStore> ref,
        final ComponentAccessor<EntityStore> componentAccessor
    ) {
        final PlayerSkinComponent playerSkinComponent = componentAccessor.getComponent(ref, PlayerSkinComponent.getComponentType());
        if (playerSkinComponent == null) {
            return;
        }

        // Mark the skin as needing resynced with clients.
        playerSkinComponent.setNetworkOutdated();

        // Validate the skin and make a player model for it.
        final Model playerModel = CosmeticsModule.get().createModel(playerSkinComponent.getPlayerSkin());
        if (playerModel == null) {
            return;
        }

        // Assign the new model to the player.
        componentAccessor.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(playerModel));
    }
}
