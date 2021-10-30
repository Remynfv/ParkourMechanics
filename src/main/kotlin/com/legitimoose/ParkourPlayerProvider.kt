package com.legitimoose

import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.player.PlayerLoginEvent
import net.minestom.server.event.player.PlayerMoveEvent
import net.minestom.server.event.player.PlayerStartFlyingEvent
import net.minestom.server.network.PlayerProvider
import net.minestom.server.network.player.PlayerConnection
import java.util.*

class ParkourPlayerProvider: PlayerProvider
{
    init
    {
        //Update players collisions on move. Possibly big laggy, we'll see.
        globalEventHandler.addListener(PlayerMoveEvent::class.java) { event: PlayerMoveEvent ->
            if (!event.newPosition.samePoint(event.player.position))
            (event.player as ParkourPlayer).onMove()
        }

        globalEventHandler.addListener(PlayerMoveEvent::class.java) { event: PlayerMoveEvent ->
            if (!event.newPosition.samePoint(event.player.position))
                (event.player as ParkourPlayer).onMove()
        }

        // Add an event callback to specify the spawning instance (and the spawn position)
        globalEventHandler.addListener(PlayerLoginEvent::class.java) { event: PlayerLoginEvent ->
            val player = event.player
            event.setSpawningInstance(instanceContainer)
            player.respawnPoint = Pos(0.0, 42.0, 0.0)

            player.isAllowFlying = true
        }

        // Add an event callback to specify the spawning instance (and the spawn position)
        globalEventHandler.addListener(PlayerStartFlyingEvent::class.java) { event: PlayerStartFlyingEvent ->
            (event.player as ParkourPlayer).onStartFlying()
        }
    }
    override fun createPlayer(uuid: UUID, username: String, connection: PlayerConnection): ParkourPlayer
    {
        return ParkourPlayer(uuid, username, connection)
    }
}