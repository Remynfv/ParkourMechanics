package com.legitimoose

import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.network.player.PlayerConnection
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect
import net.minestom.server.timer.Task
import net.minestom.server.utils.Direction
import java.time.Duration
import java.util.*
import kotlin.math.min

class ParkourPlayer(uuid: UUID, username: String, playerConnection: PlayerConnection) : Player(uuid, username, playerConnection)
{
    companion object
    {
        var jumpUpForce = 10.0
        var jumpForce = 7.0
        var wallrunSpeedMultiplier = 7.0
        var walljumpMomentumMultiplier = 0.2
    }
    var touchingWalls: Set<Direction> = setOf()
    override fun tick(time: Long)
    {
        super.tick(time)
        if (touchingWalls.isNotEmpty())
            parkourTick()

        if (wallrunning)
            wallrunTick()
    }

    //Ticks when you move, or if something important is happening
    private fun parkourTick()
    {
        //Detect walls and smacks
        checkColliding()

        //Add slowfalling
        if (touchingWalls.isNotEmpty() && wallrunning)
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

        if (isOnGround)
        {
            canWallrun = true

            if (stopWallrunTimer != null)
            {
                stopWallrunTimer?.cancel()
                stopWallrunTimer = null
                sendMessage("timer stopped")
            }
        }
    }

    //Runs when you first hit a wall.
    private fun smack(direction: Direction)
    {
        //Smack!
        if (!isOnGround)
        {
            startWallrun(direction)
        }
    }

    private fun startWallrun(direction: Direction)
    {
        if (wallrunning || !canWallrun)
            return

        val lookingVec = getVecFromYaw(position.yaw).mul(2.0)
            sendMessage((position.yaw / 45).toString())

        if (stopWallrunTimer == null)
        {
            stopWallrunTimer = schedulerManager.buildTask {

                //Little hop off the wall
                wallrunDirection?.opposite()?.let {

                    setVelocity(
                        Vec(it.normalX().toDouble() * jumpForce * 0.2, 1.0, it.normalZ() * jumpForce * 0.2)
                        .add(velocity.withY(0.0))
                    )

                }

                //Stop the wallrun
                stopWallrun()
                canWallrun = false
                stopWallrunTimer = null
                sendMessage("Your wallrunning days are over!")
            }.delay(Duration.ofSeconds(1)).schedule()
        }
        wallrunning = true
        wallrunDirection = direction
        getVecFromYaw(position.yaw)
            .mul(wallrunSpeedMultiplier)
            .withY(
                min(velocity.y, 4.0))
            .let { wallrunVelocity = it; setVelocity(it); sendMessage(it.toString())}
    }

    private fun stopWallrun()
    {
        wallrunning = false
        wallrunVelocity = null
        wallrunDirection = null
    }

    var canWallrun = true
        set(value)
        {
            field = value
            isAllowFlying = value
        }
    var wallrunning = false
    var wallrunVelocity: Vec? = null
    var wallrunDirection: Direction? = null
    private fun wallrunTick()
    {
        //Check if wallrun is over
        if (isOnGround || !touchingWalls.contains(wallrunDirection))
        {
            stopWallrun()
            return
        }

        wallrunVelocity = wallrunVelocity?.withY { y -> y - 0.5 }
        sendMessage("newvelocity ${wallrunVelocity?.y}")

        //Keep goin' forward.
        wallrunVelocity?.let { setVelocity(it) }
    }

    private fun checkColliding()
    {
        val bb = boundingBox.expand(0.05, 0.05, 0.05)

        val output = mutableSetOf<Direction>()
        for (dir in Direction.HORIZONTAL)
        {
            val blockPos = position.add(dir.vec()).asVec().apply(Vec.Operator.FLOOR)

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

    var stopWallrunTimer: Task? = null

    fun onStartFlying()
    {
        isFlying = false

        //Normal jumps stop wallrunning, wallrun jumps do not.
        if (!wallrunning)
            canWallrun = false

        stopWallrun()

        val wall = touchingWalls.firstOrNull()
        if (wall != null)
        {
            wallJump(wall, 1.0)
        }
    }

    private fun wallJump(wall: Direction, strength: Double, momentumConservation: Double = walljumpMomentumMultiplier)
    {
        setVelocity(getWallJump(wall, strength, momentumConservation))
    }

    private fun getWallJump(wall: Direction, strength: Double, momentumConservation: Double = walljumpMomentumMultiplier): Vec
    {
        val opposite = wall.opposite()

        return Vec(opposite.normalX().toDouble() * jumpForce, jumpUpForce, opposite.normalZ() * jumpForce)
            .mul(strength)
            .add(
                velocity
                    .withY(0.0)
                    .mul(momentumConservation)
            )
    }
}
