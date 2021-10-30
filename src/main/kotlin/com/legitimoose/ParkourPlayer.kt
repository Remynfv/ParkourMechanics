package com.legitimoose

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.network.player.PlayerConnection
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect
import net.minestom.server.timer.Task
import net.minestom.server.utils.Direction
import java.time.Duration
import java.util.*
import kotlin.math.absoluteValue
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

        //Add slow fall while wallrunning
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

        //Refresh everything when you're on the ground
        if (isOnGround)
        {
            canWallrun = true
            lastBonkDirection = null

            if (stopWallrunTimer != null)
            {
                stopWallrunTimer?.cancel()
                stopWallrunTimer = null
            }
        }

        //Reset walljumping value
        walljumping = false
    }

    //Runs when you first hit a wall.
    var lastBonkDirection: Direction? = null
    private fun smack(direction: Direction)
    {
        //Smack!
        if (!isOnGround)
        {
            //Don't make the funny noise if you are wallrunning
            if (startWallrun(direction))
                return
            else
            {
                bonk(direction)
            }
        }
        if (lastBonkDirection != direction)
        {
            if (!canWallrun)
                bonk(direction)

            //Smacky noise
            playSound(Sound.sound(Key.key("block.stone.fall"), Sound.Source.PLAYER, 3f, 1f), Sound.Emitter.self())
        }


    }

    private fun bonk(direction: Direction)
    {
        if (lastBonkDirection != direction)
        {
            //BONK
            sendMessage("bönk")
            wallJump(direction, 0.2, 0.0)
            lastBonkDirection = direction
            playSound(Sound.sound(Key.key("item.crossbow.hit"), Sound.Source.PLAYER, 0.5f, 2f), Sound.Emitter.self())
        }
    }

    private fun startWallrun(direction: Direction): Boolean
    {
        if (wallrunning || !canWallrun)
            return false

        val yaw = position.yaw
        sendMessage(yaw.toString())
        if (!when (direction)
            {
                Direction.NORTH -> (yaw.absoluteValue in 90f..135f)
                Direction.SOUTH -> (yaw.absoluteValue in 45f..90f)
                Direction.WEST -> (position.yaw in 135f..180f || position.yaw in 0f..45f)
                Direction.EAST -> (position.yaw in -180f..-135f || position.yaw in -45f..0f)
                else -> false
            }
        ) return false

        if (stopWallrunTimer == null)
        {
            stopWallrunTimer = schedulerManager.buildTask {

                //Little hop off the wall
                wallrunDirection?.opposite()?.let {

                    setVelocity(
                        Vec(it.normalX().toDouble() * jumpForce * 0.5, 1.0, it.normalZ() * jumpForce * 0.2)
                        .add(velocity.withY(0.0))
                    )

                }

                //Play the wall kick sound, a little different though
                if (wallrunning)
                {
//                    playSound(Sound.sound(Key.key("block.stone.break"), Sound.Source.PLAYER, 1.5f, 1.5f), Sound.Emitter.self())
                    playSound(Sound.sound(Key.key("item.crossbow.hit"), Sound.Source.PLAYER, 0.5f, 2f), Sound.Emitter.self())
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
        soundTick = true
        getVecFromYaw(position.yaw)
            .mul(wallrunSpeedMultiplier)
            .withY(
                min(velocity.y, 4.0))
            .let { wallrunVelocity = it; setVelocity(it)}

        return true
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
            if (gameMode != GameMode.CREATIVE)
                isAllowFlying = value
        }
    var wallrunning = false
    var wallrunVelocity: Vec? = null
    var wallrunDirection: Direction? = null
    var soundTick = true
    private fun wallrunTick()
    {
        //Check if wallrun is over
        if (isOnGround || !touchingWalls.contains(wallrunDirection))
        {
            stopWallrun()
            return
        }

        wallrunVelocity = wallrunVelocity?.withY { y -> y - 0.5 }

        //Keep goin' forward.
        wallrunVelocity?.let { setVelocity(it) }

        //Sound
        if (soundTick)
            playSound(Sound.sound(Key.key("block.stone.step"), Sound.Source.PLAYER, 2f, 2f), Sound.Emitter.self())
        soundTick = !soundTick
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

        touchingWalls = output
    }

    fun onMove()
    {
        parkourTick()
    }

    var stopWallrunTimer: Task? = null

    fun onStartFlying()
    {
        if (gameMode == GameMode.CREATIVE)
            return

        isFlying = false

        //Normal jumps stop wallrunning, wallrun jumps do not.
        if (!wallrunning)
            canWallrun = false

        stopWallrun()

        val wall = touchingWalls.firstOrNull()
        if (wall != null)
        {
            wallJump(wall, 1.0)
            playSound(Sound.sound(Key.key("block.stone.break"), Sound.Source.PLAYER, 2f, 1.2f), Sound.Emitter.self())
        }
    }

    var walljumping = false
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
