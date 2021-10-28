package com.legitimoose.commands

import com.legitimoose.instanceContainer
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.*

class SummonCommand : Command("summon")
{
    init
    {
        addSyntax({ sender: CommandSender, context: CommandContext ->
            if (sender is Player)
                EntityCreature(context.get("entity")).setInstance(instanceContainer, sender.asPlayer().position)
        }, ArgumentType.EntityType("entity"))
    }
}