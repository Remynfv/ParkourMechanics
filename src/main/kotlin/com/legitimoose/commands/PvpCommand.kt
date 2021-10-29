package com.legitimoose.commands

import com.legitimoose.CombatUtils
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.arguments.ArgumentType

class PvpCommand : Command("pvp")
{
    enum class PvpArgs { iframes, fist_damage }
    init
    {
        addSyntax({ sender: CommandSender, context: CommandContext ->
            val value = context.get<Int>("value")
            val arg: PvpArgs = context.get<PvpArgs>("pvpargs")

            when (arg)
            {
                PvpArgs.iframes -> CombatUtils.iframes = value.toLong()
                PvpArgs.fist_damage -> CombatUtils.fistDamage = value.toFloat()
            }
            sender.sendMessage("${arg.name} set to $value")

        }, ArgumentType.Enum("pvpargs", PvpArgs::class.java), ArgumentType.Integer("value"))

        addSyntax({ sender, context ->
            when (context.get<PvpArgs>("pvpargs"))
            {
                PvpArgs.iframes -> sender.sendMessage("Currently set to ${CombatUtils.iframes} frames of invincibility.")
                PvpArgs.fist_damage -> sender.sendMessage("Currently set to ${CombatUtils.fistDamage}")
            }
        }, ArgumentType.Enum("pvpargs", PvpArgs::class.java))
    }
}