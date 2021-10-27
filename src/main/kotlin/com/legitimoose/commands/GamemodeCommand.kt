package com.legitimoose.commands

import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.GameMode

class GamemodeCommand : Command("gamemode")
{
    init
    {
        addSyntax({ sender: CommandSender, context: CommandContext ->
            sender.asPlayer().gameMode = context.get("gamemode")
        }, ArgumentType.Enum("gamemode", GameMode::class.java))
    }
}