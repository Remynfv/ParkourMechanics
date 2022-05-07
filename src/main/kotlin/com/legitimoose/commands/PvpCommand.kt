package com.legitimoose.commands

import com.legitimoose.CombatUtils
import com.legitimoose.ParkourPlayer
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.arguments.ArgumentType

class PvpCommand : Command("pvp")
{
    enum class PvpArgs { iframes, fist_damage, jump_force, jump_up_force }
    init
    {
        addSyntax({ sender: CommandSender, context: CommandContext ->
            val value = context.get<Int>("value")
            val arg: PvpArgs = context.get<PvpArgs>("pvpargs")

            when (arg)
            {
                PvpArgs.iframes -> CombatUtils.iframes = value.toLong()
                PvpArgs.fist_damage -> CombatUtils.fistDamage = value.toFloat()
                PvpArgs.jump_force -> ParkourPlayer.jumpForce = value.toDouble()
                PvpArgs.jump_up_force -> ParkourPlayer.jumpUpForce = value.toDouble()
            }
            sender.sendMessage("${arg.name} set to $value")

        }, ArgumentType.Enum("pvpargs", PvpArgs::class.java), ArgumentType.Integer("value"))

        addSyntax({ sender, context ->
            when (context.get<PvpArgs>("pvpargs"))
            {
                PvpArgs.iframes -> sender.sendMessage("Currently set to ${CombatUtils.iframes} frames of invincibility.")
                PvpArgs.fist_damage -> sender.sendMessage("Currently set to ${CombatUtils.fistDamage}")
                PvpArgs.jump_force -> sender.sendMessage("Currently set to ${ParkourPlayer.jumpForce}")
                PvpArgs.jump_up_force -> sender.sendMessage("Currently set to ${ParkourPlayer.jumpUpForce}")
            }
        }, ArgumentType.Enum("pvpargs", PvpArgs::class.java))
    }
}