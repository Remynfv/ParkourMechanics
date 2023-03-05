package com.legitimoose.commands

import com.legitimoose.instanceContainer
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.EntityCreature
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.other.PaintingMeta
import net.minestom.server.utils.Direction

class SummonCommand : Command("summon")
{
    init
    {
        addSyntax({ sender: CommandSender, context: CommandContext ->
            if (sender is Player)
                EntityCreature(context.get("entity")).also { creature ->
                    if (creature.entityType == EntityType.PAINTING)
                    {
                        creature.setNoGravity(true)
                        (creature.entityMeta as PaintingMeta).let {
                            it.direction = listOf(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST).random()
                            sender.sendMessage(it.direction.toString())
                        }
                    }
                }.setInstance(instanceContainer, sender.position)
        }, ArgumentType.EntityType("entity"))
    }
}