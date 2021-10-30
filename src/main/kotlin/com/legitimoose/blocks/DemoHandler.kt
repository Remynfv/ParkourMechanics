package com.legitimoose.blocks

import net.minestom.server.entity.Player
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.instance.block.BlockHandler.Placement
import net.minestom.server.instance.block.BlockHandler.PlayerPlacement
import net.minestom.server.utils.NamespaceID

class DemoHandler : BlockHandler
{
    override fun onPlace(placement: Placement)
    {
        val block = placement.block
        if (placement is PlayerPlacement)
        {
            // A player placed the block
            placement.player.sendMessage("The block " + block.name() + " has been placed")
        }
        println("The block " + block.name() + " has been placed")
    }

    override fun getNamespaceId(): NamespaceID
    {
        // Namespace required for serialization purpose
        return NamespaceID.from("minestom:demo")
    }
}