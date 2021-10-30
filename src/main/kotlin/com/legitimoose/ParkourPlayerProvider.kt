package com.legitimoose

import net.minestom.server.entity.Player
import net.minestom.server.network.PlayerProvider
import net.minestom.server.network.player.PlayerConnection
import java.util.*

class ParkourPlayerProvider: PlayerProvider
{
    override fun createPlayer(uuid: UUID, username: String, connection: PlayerConnection): ParkourPlayer
    {
        return ParkourPlayer(uuid, username, connection)
    }
}