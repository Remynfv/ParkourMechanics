package com.legitimoose.commands

import com.legitimoose.MainDemo
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.CommandExecutor
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.Player

class TestCommand() : Command("test")
{
    init
    {
        // Executed if no other executor can be used
        defaultExecutor = CommandExecutor { sender: CommandSender, context: CommandContext? ->
            (sender as Player).tick(0)
            sender.sendMessage("${sender.velocity}")
        }

        // All default arguments are available in the ArgumentType class
        // Each argument has an identifier which should be unique. It is used internally to create the nodes
        val numberArgument = ArgumentType.Integer("my-number")

        // Finally, create the syntax with the callback, and an infinite number of arguments
        addSyntax({ sender: CommandSender, context: CommandContext ->
            val number = context.get(numberArgument)
            sender.sendMessage("You typed the number $number")
        }, numberArgument)


    }
}