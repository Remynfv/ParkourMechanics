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
import net.minestom.server.utils.time.TimeUnit
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
        var maxClimbs = 2
        var wallrunTime = 40L
    }
    var touchingWalls: Set<Direction> = setOf()
    override fun tick(time: Long)
    {
        super.tick(time)
        if (touchingWalls.isNotEmpty())
            parkourTick()

        if (wallrunning)
            wallrunTick()

        if (wallClimbDirection != null)
            if (!touchingWalls.contains(wallClimbDirection))
            {
                removeEffect(PotionEffect.SLOW_FALLING)
                wallClimbDirection = null
            }

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
            wallClimbDirection = null
            climbs = 0

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
    private var lastBonkDirection: Direction? = null
    private var climbs = 0

    private fun smack(direction: Direction)
    {
        //Smack!
        if (!isOnGround)
        {
            //Don't make the funny noise if you are wallrunning
            if (startWallrun(direction) == 0)
                return
            else if (climbs >= maxClimbs)
                bonk(direction)
            else if (velocity.y > 0 && climbs < maxClimbs)
            {
                startWallClimb(direction)
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

    private var wallClimbDirection: Direction? = null
        set(value)
        {
            field = value
            isAllowFlying = value != null
        }

    private fun startWallClimb(direction: Direction): Int
    {
        val yaw = position.yaw
        if (!when (direction)
            {
                Direction.NORTH -> (yaw.absoluteValue in 135f..180f  || position.yaw in -180f..-135f)
                Direction.SOUTH -> (position.yaw in -45f..45f)
                Direction.WEST -> (position.yaw in 45f..135f)
                Direction.EAST -> (position.yaw in -135f..-45f)
                else -> false
            }
        ) {
            bonk(direction) //Bonk if you hit a climb the wrong way
            return 2
        }


        //Wall climb
        val runForce = velocity.withY(0.0).lengthSquared()

        wallClimbDirection = direction

        //Only do the small jump if youre moving REAL slow. This is buggy af.
        if (runForce < 4)
        {
            setVelocity(Vec(0.0, 7.0, 0.0))
            addEffect(Potion(PotionEffect.SLOW_FALLING, 0, 20, false, true))
        }
        else
        {
            setVelocity(Vec(0.0, 12.0, 0.0))
            addEffect(Potion(PotionEffect.SLOW_FALLING, 0, 30, false, true))
        }

        climbs++
        return 0
    }

    private fun bonk(direction: Direction)
    {
        if (lastBonkDirection != direction)
        {
            //BONK
            wallJump(direction, 0.2, 0.0)
            lastBonkDirection = direction
            playSound(Sound.sound(Key.key("item.crossbow.hit"), Sound.Source.PLAYER, 0.5f, 2f), Sound.Emitter.self())

            //Clear slow falling from climb
            removeEffect(PotionEffect.SLOW_FALLING)

            //No more climbing if you bonk >:)
            climbs = maxClimbs
        }
    }

    //Returns int:
    //0: WALLRUN SUCCEEDED
    //1: CAN'T START WALLRUN
    //2: FACING WRONG WAY
    //3: OUT OF CLIMBS
    private fun startWallrun(direction: Direction): Int
    {
        if (wallrunning || !canWallrun)
            return 1

        val yaw = position.yaw
        if (!when (direction)
            {
                Direction.NORTH -> (yaw.absoluteValue in 90f..135f)
                Direction.SOUTH -> (yaw.absoluteValue in 45f..90f)
                Direction.WEST -> (position.yaw in 135f..180f || position.yaw in 0f..45f)
                Direction.EAST -> (position.yaw in -180f..-135f || position.yaw in -45f..0f)
                else -> false
            }
        ) return 2

        if (climbs >= maxClimbs)
            return 3

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
            }.delay(wallrunTime, TimeUnit.CLIENT_TICK).schedule()
        }
        wallrunning = true
        wallrunDirection = direction
        soundTick = true
        climbs++
        getVecFromYaw(position.yaw)
            .mul(wallrunSpeedMultiplier)
            .withY(
                min(velocity.y, 4.0))
            .let { wallrunVelocity = it; setVelocity(it)}

        return 0
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
        set(value)
        {
            field = value
            isAllowFlying = value
        }
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

//        //Normal jumps stop wallrunning, wallrun jumps do not.
//        if (!wallrunning)
//            canWallrun = false

        //Walljump if touching a wall and climbing or wallrunning.
        val wall = touchingWalls.firstOrNull()
        if (wall != null && (wallClimbDirection != null || wallrunning))
        {
            setVelocity(
                getWallJump(wall, 1.0)
                    .add(getVecFromYaw(position.yaw)
                        .mul(20.0)
                    )
            )

            wallJump(wall, 1.0)
            playSound(Sound.sound(Key.key("block.stone.break"), Sound.Source.PLAYER, 2f, 1.2f), Sound.Emitter.self())
        }

        stopWallrun()

        //Remove slow falling from wall climbs when you jump off the wall.
        removeEffect(PotionEffect.SLOW_FALLING)


        if (climbs >= maxClimbs)
            isAllowFlying = false
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

    fun onSneak()
    {
        //TODO Sliiiiide
    }
}
