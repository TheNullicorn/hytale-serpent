package me.nullicorn.hytale.serpent.command;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.hytale.serpent.asset.SerpentConfig;
import me.nullicorn.hytale.serpent.component.Serpent;

import javax.annotation.Nonnull;

public final class SerpentAddCommand extends AbstractPlayerCommand {
    private final RequiredArg<SerpentConfig> serpentConfigArg =
        this.withRequiredArg(
            "config",
            "server.commands.serpent.add.config.desc",
            SerpentConfig.SINGLE_ARGUMENT_TYPE
        );

    public SerpentAddCommand() {
        super("add", "server.commands.serpent.add.desc");
    }

    @Override
    protected void execute(
        @Nonnull final CommandContext context,
        @Nonnull final Store<EntityStore> store,
        @Nonnull final Ref<EntityStore> ref,
        @Nonnull final PlayerRef playerRef,
        @Nonnull final World world
    ) {
        // TODO: Almost identical to SerpentMorphCommand. Move this shared code elsewhere.
        final SerpentConfig config = context.get(this.serpentConfigArg);

        final Holder<EntityStore> holder = store.getRegistry().newHolder();
        holder.addComponent(Serpent.getComponentType(), new Serpent(new Transform(playerRef.getTransform().getPosition()), config.layout().chooseBones()));
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
        holder.addComponent(UUIDComponent.getComponentType(), UUIDComponent.randomUUID());
        holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(playerRef.getTransform().getPosition().clone(), new Vector3f()));
        holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(playerRef.getHeadRotation().clone()));
        store.addEntity(holder, AddReason.SPAWN);
    }
}
