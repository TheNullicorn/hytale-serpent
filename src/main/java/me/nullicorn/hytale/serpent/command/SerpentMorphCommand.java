package me.nullicorn.hytale.serpent.command;

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
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.hytale.serpent.asset.SerpentConfig;
import me.nullicorn.hytale.serpent.component.Serpent;

import javax.annotation.Nonnull;
import java.util.Objects;

public final class SerpentMorphCommand extends AbstractPlayerCommand {
    private final RequiredArg<SerpentConfig> serpentConfigArg =
        this.withRequiredArg(
            "config",
            "server.commands.serpent.add.config.desc",
            SerpentConfig.SINGLE_ARGUMENT_TYPE
        );

    private final OptionalArg<Integer> boneCountArg =
        this.withOptionalArg(
            "bones",
            "server.commands.serpent.add.bones.desc",
            ArgTypes.INTEGER
        );

    public SerpentMorphCommand() {
        super("morph", "server.commands.serpent.morph.desc");
        this.addSubCommand(new SerpentMorphResetCommand());
    }

    @Override
    protected void execute(
        @Nonnull final CommandContext context,
        @Nonnull final Store<EntityStore> store,
        @Nonnull final Ref<EntityStore> ref,
        @Nonnull final PlayerRef playerRef,
        @Nonnull final World world
    ) {
        // TODO: Almost identical to SerpentAddCommand. Move this shared code elsewhere.
        final SerpentConfig config = context.get(this.serpentConfigArg);

        final int boneCount = Objects.requireNonNullElseGet(
            context.get(this.boneCountArg),
            () -> {
                final IntRange range = config.getBodyBoneCount();
                return RandomExtra.randomRange(range.getInclusiveMin(), range.getInclusiveMax());
            });
        if (boneCount < 0) {
            context.sendMessage(Message.translation("server.commands.serpent.bones.mustBeAtLeastZero"));
            return;
        }

        final Vector3d[] joints = new Vector3d[boneCount + 1];
        double distance = 0;
        for (int i = 0; i < joints.length; i++) {
            joints[i] = playerRef.getTransform().getPosition().clone().add(0, 0, -distance);
            if (i == 0) {
                distance += config.getHead().getLength();
            } else if (i < joints.length - 2) {
                distance += config.getBody().getLength();
            } else {
                distance += config.getTail().getLength();
            }
        }

        store.putComponent(ref, Serpent.getComponentType(), new Serpent(joints, config));
    }
}
