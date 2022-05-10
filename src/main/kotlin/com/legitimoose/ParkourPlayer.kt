package com.legitimoose

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.minestom.server.MinecraftServer
import net.minestom.server.attribute.Attribute
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.network.packet.server.play.TimeUpdatePacket
import net.minestom.server.network.player.PlayerConnection
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import net.minestom.server.utils.Direction
import net.minestom.server.utils.time.TimeUnit
import java.time.Duration
import java.time.Instant
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
        var slideTime = 20L
        var maxSlideSpeed: Double = 5.0
    }


    private val fallDamageSlowTime: Int = 40

    /**
     * Maximum time between pressing sneak and landing to execute a roll, in milliseconds.
     */
    private val rollTime: Int = 250

    /**
     * If null: roll has not been pressed since falling
     * Otherwise: Instant when sneak was pressed, to count
     */
    private var lastTriedToRoll: Instant? = null
    var touchingWalls: Set<Direction> = setOf()
    var wasOnGroundLastTick = false
    private var lastPos = position

    var velocityTick = Vec.ZERO

    /**
     * Y position when player last left the ground
     */
    private var fallStartHeight: Double = 0.0

    override fun tick(time: Long)
    {
        super.tick(time)

        /*
        Old velocity calculation, because the new minestom update broke it.
         */
        getVelocity()
        velocityTick = position.asVec().sub(lastPos).mul(MinecraftServer.TICK_PER_SECOND.toDouble())

        if (touchingWalls.isNotEmpty())
            parkourTick()

        if (wallrunning)
            wallrunTick()

        if (sliding)
            slideTick()

        if (wallClimbDirection != null)
            if (!touchingWalls.contains(wallClimbDirection))
            {
                removeEffect(PotionEffect.SLOW_FALLING)
                wallClimbDirection = null
            }

        if (!isOnGround && position.y > fallStartHeight)
        {
            fallStartHeight = position.y
        }
        if (wasOnGroundLastTick && !isOnGround)
        {
            onLeaveGround()
        }
        else if (!wasOnGroundLastTick && isOnGround)
        {
            onLand()
        }

        //If you have slow falling (aka, are wall sliding) reset fall height
        if (activeEffects.any { it.potion.effect == PotionEffect.SLOW_FALLING })
            fallStartHeight = position.y //Update fall height

        wasOnGroundLastTick = isOnGround
        lastPos = position //There exists "lastPosition" but I don't know what it does (it behaves unusually). So I have this for myself.
    }

    private fun onLand()
    {
        val distanceFallen = fallStartHeight - position.y

//        sendMessage("Fell $distanceFallen blocks")
        if (distanceFallen >= 5)
        {
            if (lastTriedToRoll != null)
            {
                val timeSincePress = Duration.between(lastTriedToRoll, Instant.now())
                if (timeSincePress.toMillis() <= rollTime)
                {
                    lastTriedToRoll = null
                    return roll()
                }
            }
            //"Fall damage"
            fallDamage()
        }
        else if (isSneaking)
                startSlide()
        lastTriedToRoll = null
    }

    /**
     * Slow the player and prevent jumping for a short time. AKA, take fall damage.
     */
    private fun fallDamage()
    {
        addEffect(Potion(PotionEffect.SLOWNESS, 5, fallDamageSlowTime))
        getAttribute(Attribute.MOVEMENT_SPEED).baseValue = 0.01f
        addEffect(Potion(PotionEffect.JUMP_BOOST, -127, fallDamageSlowTime))
        scheduler.scheduleTask(
            {getAttribute(Attribute.MOVEMENT_SPEED).baseValue = 0.1f},
            TaskSchedule.tick(fallDamageSlowTime),
            TaskSchedule.stop()
        )
        damage(DamageType.GRAVITY, 0f)
    }

    //Jank, not quite onJump function because I don't think we can detect jumps.
    private fun onLeaveGround()
    {
        fallStartHeight = lastPos.y //Reset fall height when we first leave the ground.

        val insideBlock = instance.getBlock(lastPos).name()
        if (insideBlock.contains("stairs") || insideBlock.contains("slab") )
            springboard()
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
                addEffect(Potion(PotionEffect.LEVITATION, -1, 32767))
            }
            else
            {
                addEffect(Potion(PotionEffect.LEVITATION, -5, 32767))
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
            else if (velocityTick.y > 0 && climbs < maxClimbs)
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
        val runForce = velocityTick.withY(0.0).lengthSquared()

        wallClimbDirection = direction

        //Only do the small jump if youre moving REAL slow. This is buggy af.
        if (runForce < 4)
        {
            setVelocity(Vec(0.0, 7.0, 0.0))
            addEffect(Potion(PotionEffect.SLOW_FALLING, 0, 20))
        }
        else
        {
            setVelocity(Vec(0.0, 12.0, 0.0))
            addEffect(Potion(PotionEffect.SLOW_FALLING, 0, 30))
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
                        .add(velocityTick.withY(0.0))
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
                min(velocityTick.y, 4.0))
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

            //Skip stairs n slabs, they aint walls >:(
            //This will definitely not cause any issues \s
            //This was added to fix bonking on the INSIDE of slabs when springboarding.
            val blockname = block.name()
            if (blockname.contains("stairs") || blockname.contains("slab"))
                continue

            if (block.isSolid) //Excludes stairs n stuff for some reason
            {
                val relativePosition = position.asVec().sub(blockPos)
//                val relativeStart = boundingBox.relativeStart()
//                val relativeEnd = boundingBox.relativeEnd()
                @Suppress("UnstableApiUsage") val collisionCheck = block.registry().collisionShape().intersectBox(relativePosition, bb) //Doesn't work with non-cubic blocks!
//                sendMessage(block.registry().collisionShape().relativeStart().toString())
//                val relativeStart = block.registry().collisionShape().relativeStart().add(blockPos)
//                val relativeEnd = block.registry().collisionShape().relativeEnd().add(blockPos)
//                ParticleCreator.createParticlePacket(
//                    Particle.FLAME, relativeStart.x(), relativeStart.y(), relativeStart.z(),
//                0f, 0f, 0f, 1).let { sendPacket(it) }
//                ParticleCreator.createParticlePacket(Particle.SOUL_FIRE_FLAME, relativeEnd.x(), relativeEnd.y(), relativeEnd.z(),
//                    0f, 0f, 0f, 1).let { sendPacket(it) }
                if (collisionCheck)
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
                    .add(
                        getVecFromYaw(position.yaw)
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
                velocityTick
                    .withY(0.0)
                    .mul(momentumConservation)
            )
    }

    fun onSneak()
    {
        //Sliiiiide
        startSlide()

        if (!isOnGround && lastTriedToRoll == null)
        {
            lastTriedToRoll = Instant.now()
        }
    }

    var slideVelocity: Vec? = null
    private val sliding: Boolean
        get() = slideVelocity != null
    private var stopSlideTimer: Task? = null
    private fun startSlide()
    {
        //For reasons unknown to me, if the velocity equals zero when we start,
        // it causes array overflow exceptions in the Player collision code.
        var vel = velocityTick.withY(0.0)
        if (!isOnGround || sliding || !standingOnSolidBlock || vel == Vec.ZERO || activeEffects.any {it.potion.effect == PotionEffect.JUMP_BOOST})
            return

        val length = vel.length()

        //If speed is large, make it a liiiitle larger, otherwise, just do it normally (and cap it just to be safe i guess)
        if (length > 5.5)
            vel = vel.normalize().mul(maxSlideSpeed)
        else
            vel = vel.normalize().mul(min(length, maxSlideSpeed))

        slideVelocity = vel

        isFlyingWithElytra = true

        stopSlideTimer = schedulerManager.buildTask { stopSlide() }.delay(slideTime, TimeUnit.CLIENT_TICK).schedule()
    }

    private fun stopSlide()
    {
        stopSlideTimer?.cancel()
        slideVelocity = null
        isFlyingWithElytra = false
    }

    /**
        Sets GameTime to 0 to play a roll animation
     */
    private fun roll() {
        @Suppress("UnstableApiUsage")
        sendPacket(TimeUpdatePacket(0, 6000))
    }

    private fun slideTick()
    {
        if (!isOnGround)
        {
//            stopSlide()
            isFlyingWithElytra = false
            return
        }
        if (!standingOnSolidBlock)
        {
            slideVelocity?.let { teleport(position.add(it.normalize().mul(0.2))) }
//            slideVelocity?.let { setVelocity(it.mul(2.0).withY(5.0)); }
            slideVelocity?.let { setVelocity(it.mul(1.0).withY(0.0)); }
            stopSlide()
            return
        }

        //Springboard!!!
        val insideBlock = instance.getBlock(position).name()
        if (insideBlock.contains("stairs") || insideBlock.contains("slab") )
        {
            springboard()
            return
        }

        slideVelocity?.let { setVelocity(it) }

        isFlyingWithElytra = true
    }

    private fun springboard()
    {
        val vel = velocityTick.mul(1.5).withY(jumpUpForce)

        if (sliding)
            stopSlide()

        setVelocity(vel)
    }

    private val standingOnSolidBlock: Boolean
        get()
        {
            return instance.getBlock(position.withY { y -> y - 1 }).isSolid
        }

    fun onStopSneak()
    {
        if (sliding)
            stopSlide()
    }
}
