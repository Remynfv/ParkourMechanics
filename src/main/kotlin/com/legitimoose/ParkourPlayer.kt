package com.legitimoose

import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerTickEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockGetter
import net.minestom.server.network.PlayerProvider
import net.minestom.server.network.player.PlayerConnection
import net.minestom.server.utils.Direction
import java.util.*

class ParkourPlayer(uuid: UUID, username: String, playerConnection: PlayerConnection) : Player(uuid, username, playerConnection)
{
    var touchingWalls: Set<Direction> = setOf()
    override fun tick(time: Long)
    {
        super.tick(time)

        checkColliding()
    }

    private fun checkColliding()
    {
        val bb = boundingBox.expand(0.05, 0.05, 0.05)

        val output = mutableSetOf<Direction>()
        for (dir in Direction.HORIZONTAL)
        {
            val blockPos = position.add(dir.normalX().toDouble(), dir.normalY().toDouble(), dir.normalZ().toDouble()).asVec().apply(Vec.Operator.FLOOR)

            val block = instance.getBlock(blockPos)
            if (block.isSolid)
            {
                if (bb.intersectWithBlock(blockPos))
                {
                    output.add(dir)
                }
            }
        }
        val msg = MiniMessage.get().parse("<rainbow>${output}")
        sendMessage(msg)

        touchingWalls = output
    }
}
