package me.nullicorn.serpentine.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.nullicorn.serpentine.asset.SerpentConfig;
import me.nullicorn.serpentine.component.Serpent;

import javax.annotation.Nonnull;

public final class SerpentMorphCommand extends AbstractPlayerCommand {
    private final RequiredArg<SerpentConfig> serpentConfigArg =
        this.withRequiredArg(
            "config",
            "server.commands.serpent.add.config.desc",
            SerpentConfig.SINGLE_ARGUMENT_TYPE
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
        // TODO: Almost identical to SerpentMorphCommand. Move this shared code elsewhere.
        final SerpentConfig config = context.get(this.serpentConfigArg);

        store.putComponent(ref, Serpent.getComponentType(), new Serpent(new Transform(playerRef.getTransform().getPosition()), config.layout().chooseBones()));
    }
}
