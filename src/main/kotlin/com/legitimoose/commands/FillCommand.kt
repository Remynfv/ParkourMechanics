package com.legitimoose.commands

import com.legitimoose.instanceContainer
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.command.builder.arguments.minecraft.ArgumentBlockState
import net.minestom.server.command.builder.arguments.relative.ArgumentRelativeBlockPosition
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.instance.block.Block
import net.minestom.server.utils.location.RelativeVec
import javax.swing.text.Position

class FillCommand : Command("fill")
{
    init
    {
        addSyntax({ sender: CommandSender, context: CommandContext ->

            if (sender !is Player)
                return@addSyntax

            val pos1 = context.get<RelativeVec>("pos1").fromSender(sender)
            val pos2 = context.get<RelativeVec>("pos2").fromSender(sender)

            val block = context.get<Block>("block")

            val instance = sender.instance

            //TODO make it work if starting at high number going to low.
            for (x in pos1.blockX() .. pos2.blockX())
                for (y in pos1.blockY() .. pos2.blockY())
                    for (z in pos1.blockZ() .. pos2.blockZ())
                        instance?.setBlock(Vec(x.toDouble(), y.toDouble(), z.toDouble()), block)

            sender.sendMessage("Filled area with ${block.name()}")
        }, ArgumentType.RelativeBlockPosition("pos1"),
            ArgumentType.RelativeBlockPosition("pos2"),
            ArgumentType.BlockState("block")
        )
    }
}