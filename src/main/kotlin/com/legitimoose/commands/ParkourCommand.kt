package com.legitimoose.commands

import com.legitimoose.ParkourPlayer
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.arguments.ArgumentType

//TODO Reimplement command with delegates or reflection or some magic
class ParkourCommand : Command("pk")
{
    enum class ParkourArgs { jump_force, jump_up_force, max_climbs, wallrun_time }
    init
    {
        addSyntax({ sender: CommandSender, context: CommandContext ->
            val value = context.get<Int>("value")
            val arg: ParkourArgs = context.get<ParkourArgs>("pvpargs")

            when (arg)
            {
                ParkourArgs.jump_force -> ParkourPlayer.jumpForce = value.toDouble()
                ParkourArgs.jump_up_force -> ParkourPlayer.jumpUpForce = value.toDouble()
                ParkourArgs.max_climbs -> ParkourPlayer.maxClimbs = value
                ParkourArgs.wallrun_time -> ParkourPlayer.wallrunTime = value.toLong()
            }
            sender.sendMessage("${arg.name} set to $value")

        }, ArgumentType.Enum("pvpargs", ParkourArgs::class.java), ArgumentType.Integer("value"))
    }
}