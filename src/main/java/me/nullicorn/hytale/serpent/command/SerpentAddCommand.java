package me.nullicorn.hytale.serpent.command;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.random.RandomExtra;
import com.hypixel.hytale.math.range.IntRange;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.AssetArgumentType;
import com.hypixel.hytale.server.core.command.system.arguments.types.SingleArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.hytale.serpent.asset.SerpentConfig;
import me.nullicorn.hytale.serpent.component.Serpent;

import javax.annotation.Nonnull;
import java.util.Objects;

public final class SerpentAddCommand extends AbstractPlayerCommand {
    public static final SingleArgumentType<SerpentConfig> SERPENT_ASSET_ARG_TYPE = new AssetArgumentType<>(
        "server.commands.parsing.argtype.asset.serpent-config.name", SerpentConfig.class, "server.commands.parsing.argtype.asset.serpent-config.usage"
    );

    private final RequiredArg<SerpentConfig> serpentConfigArg =
        this.withRequiredArg(
            "config",
            "server.commands.serpent.add.config.desc",
            SERPENT_ASSET_ARG_TYPE
        );

    private final OptionalArg<Integer> segmentCountArg =
        this.withOptionalArg(
            "segments",
            "server.commands.serpent.add.segments.desc",
            ArgTypes.INTEGER
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
        final SerpentConfig config = context.get(this.serpentConfigArg);

        final int segmentCount = Objects.requireNonNullElseGet(
            context.get(this.segmentCountArg),
            () -> {
                final IntRange range = config.getBodySegmentCount();
                return RandomExtra.randomRange(range.getInclusiveMin(), range.getInclusiveMax());
            });
        if (segmentCount < 0) {
            context.sendMessage(Message.translation("server.commands.serpent.arg.segments.error.mustBeAtLeastZero"));
            return;
        }

        final Vector3d[] joints = new Vector3d[segmentCount + 1];
        double distance = 0;
        for (int i = 0; i < joints.length; i++) {
            joints[i] = playerRef.getTransform().getPosition().clone().add(0, 0, -distance);
            if (i == 0) {
                distance += config.getHead().getLength();
            } else if (i < joints.length - 1) {
                distance += config.getBody().getLength();
            } else {
                distance += config.getTail().getLength();
            }
        }

        final Holder<EntityStore> holder = store.getRegistry().newHolder();
        holder.addComponent(Serpent.getComponentType(), new Serpent(joints, config));
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
        holder.addComponent(UUIDComponent.getComponentType(), UUIDComponent.randomUUID());
        store.addEntity(holder, AddReason.SPAWN);
    }
}
