package com.legitimoose

import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerTickEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockGetter
import net.minestom.server.network.PlayerProvider
import net.minestom.server.network.player.PlayerConnection
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect
import net.minestom.server.utils.Direction
import java.util.*

class ParkourPlayer(uuid: UUID, username: String, playerConnection: PlayerConnection) : Player(uuid, username, playerConnection)
{
    companion object
    {
        var jumpUpForce = 10.0
        var jumpForce = 7.0
    }
    var touchingWalls: Set<Direction> = setOf()
    override fun tick(time: Long)
    {
        super.tick(time)
        if (touchingWalls.isNotEmpty())
            parkourTick()
    }

    //Ticks when you move, or if something important is happening
    private fun parkourTick()
    {
        checkColliding()
        if (touchingWalls.isNotEmpty())
        {
            if (isSneaking)
            {
                addEffect(Potion(PotionEffect.LEVITATION, -1, 32767, false, true))
            }
            else
            {
                addEffect(Potion(PotionEffect.LEVITATION, -5, 32767, false, true))
            }
        }
        else
            removeEffect(PotionEffect.LEVITATION)
    }

    //Runs when you first hit a wall.
    private fun smack(direction: Direction)
    {
        //Smack!
        if (!isOnGround)
        {
            sendMessage("1: ${(getVecFromYaw(position.yaw))}")
            sendMessage("2: ${getVecFromYaw(position.yaw).mul(jumpForce)}")
            sendMessage("3: ${getVecFromYaw(position.yaw).mul(jumpForce * 3).withY(velocity.y)}")

            wallrun()
        }
    }

    private fun wallrun()
    {
        setVelocity((getVecFromYaw(position.yaw)).mul(jumpForce).withY(velocity.y))

    }

    private fun wallrunTick()
    {

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
                    if (!touchingWalls.contains(dir))
                        smack(dir)

                    output.add(dir)
                }
            }
        }
//        val msg = MiniMessage.get().parse("<rainbow>${output}")
//        sendMessage(msg)

        touchingWalls = output
    }

    fun onMove()
    {
        parkourTick()
    }

    fun onStartFlying()
    {
        isFlying = false

        val wall = touchingWalls.firstOrNull()
        if (wall != null)
        {
            val opposite = wall.opposite()
            val vel = Vec(opposite.normalX().toDouble() * jumpForce, jumpUpForce, opposite.normalZ() * jumpForce).add(velocity.withY(0.0))

            setVelocity(vel)
        }
    }
}
