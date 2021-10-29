package com.legitimoose

import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.utils.player.PlayerUtils
import net.minestom.server.utils.time.TimeUnit
import java.util.*

val previousPositions: MutableMap<UUID, Pos> = mutableMapOf()
val realVelocity: MutableMap<UUID, Vec> = mutableMapOf()


fun Player.velocityTick()
{
    val isSocketClient: Boolean = PlayerUtils.isSocketClient(this)
    if (isSocketClient && isOnGround)
    {
        if (previousPositions[uuid]?.let { position.samePoint(it) } == true)
        {
            realVelocity[uuid] = Vec(0.0)  // Didn't move since last tick
            previousPositions[uuid] = position

            return
        }
    }

}

fun Player.getRealVelocity(): Vec
{
    realVelocity[uuid]?.let { return it }
    return velocity
}