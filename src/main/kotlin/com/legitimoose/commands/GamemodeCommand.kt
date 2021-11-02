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
            val player = sender.asPlayer()
            val gamemode = context.get<GameMode>("gamemode")

            if (player.gameMode == GameMode.CREATIVE && gamemode == GameMode.CREATIVE)
            {
                player.isFlying = true
                player.velocity = player.velocity.withY(1.0)
            }
            player.gameMode = gamemode

        }, ArgumentType.Enum("gamemode", GameMode::class.java))
    }
}