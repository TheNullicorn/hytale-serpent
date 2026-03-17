package me.nullicorn.serpentine.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public final class SerpentCommand extends AbstractCommandCollection {
    public SerpentCommand() {
        super("serpent", "server.commands.serpent.desc");
        this.addSubCommand(new SerpentAddCommand());
        this.addSubCommand(new SerpentMorphCommand());
    }
}
