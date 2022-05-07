package com.legitimoose

import com.legitimoose.blocks.DemoHandler
import com.legitimoose.commands.*
import com.legitimoose.entity.ZombieCreature
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityCreature
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.player.PlayerBlockPlaceEvent
import net.minestom.server.event.player.PlayerEntityInteractEvent
import net.minestom.server.event.server.ServerListPingEvent
import net.minestom.server.instance.AnvilLoader
import net.minestom.server.instance.InstanceManager
import net.minestom.server.instance.block.Block
import net.minestom.server.ping.ServerListPingType
import java.util.*


// Initialization
val minecraftServer: MinecraftServer = MinecraftServer.init()
val instanceManager: InstanceManager = MinecraftServer.getInstanceManager()
val schedulerManager = MinecraftServer.getSchedulerManager()
val globalEventHandler = MinecraftServer.getGlobalEventHandler()

// Create the instance
val anvilLoader = AnvilLoader("worlds/worlde")
@Suppress("UnstableApiUsage")
val instanceContainer = instanceManager.createInstanceContainer(anvilLoader)

private const val SERVER_ICON = "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsQAAA7EAZUrDhsAAAQfSURBVHhe7ZtNTBNBFMdnd9tCisoBTZCriCaScDChJpT4wU1vxuhFzp5MPPhx0KABEw968ObZo8YzB2OIUUjkYDh5kOCZgnrho0JLW/e9fW/pTnc6a7sNSOfXvPyntDtt9v+f3ZntYm3nf1eESyrdA6JmilTHbVINVncntTSc/0ON+ox/ooaGzcQotTxs0rbFcu3HBCg5oM4zJgHKBPyj81dOj3gNBdPLX6kVzuXjZ6kVzvTALLU8mnWeMQmoSUCDzk+sUAKkPE32zlHLQ04COx91+55cMAkqdM4zbZ+A3R0AzkdxH5wPO+KDc27Nih9YFj0mciNYWprdngDno7oPmGNAZUoedQo0R3t26bNYQgX3gKw4gSqPZZlmt28UkwBtAhTOs2PslE1akbrj18uaj2l2+0YxCVAmQOX8qvc8cbIDNSqVxSI1PKnBM1pYA0mvERFtvxpMAmoSIJ3j/ZkeOZ8c7kIdnLmPatnBfWg7weeVUhn1w4VHqKX5TVT/U8l5J+P1e2lmErXpfiNiEuAnQOE886T7IurQwkPU4Y1tVJn5I956P5kMjuVycQf1/dAD1PL3Aqp9KoU6tvAMNRNTv1Fp+wTY6LzkfjU8F4fzNBScj6vPySNd6UBl1rawisUiFmO5YxhKBZz/q+cAcfWrwySAVAnMxOBRWipgndvwip25lrKxXqaTWDrHZHaWtrHi7jcqJgGkWpz+FNaXQ17Nbeax3hXKWAw7JztWcs/bUDKJ/g6suPuNikkAqQ9ct6937d6xHSyZO/kiFjvHTsL5G8pxj9RQKuD8DsXv5+2b7VeHSQCp7zxcpa13jR6m7lCyU3Lx6+wszN15/h4Gv+44bsLcYprtV0fbJ8ASlc7A+sm/Tk9XfBzeR7Rq+5UpoY59fIqqWnzxuGR3eNW2o1gNJmg1GFu/ETEJUCXgcS6LytfqmBVrDdUa8FZxTPX6IIzyIq3SVG+jj7GlfmXk76PtV4NJQNQE8EptVayj8kbs/F3xBnVdbKHGzWHhzU1eiBuochIaxSQgagJWfOcDb/cTcEu8Rs2JPGrc9LkZAF6Jm6gmATGxuwPgnpy69+WA00H3DwImAXrn9wutSaBJAOl/ABz14znyV2MSQKpEvl5/0DAJIFWyKjaw9n4WYM4CLcHfAXDvbfj9t3vvfSsxCVA5D6s8qP3jv5kHtISaHQB3Y0NN9c5hfRPLWHufhNZ8A5MA0hryqRLWfpkJlukRNyYBpD58v3264GBFBa7aQsV9rOb+uP+4afsEWOOiggNc/i+LraPeuB/NHUM9I/pQVe6WaHzeE29R+fcB29/H8pZ8XAn/O493dv25uI7q/1YZEyYBVxPZ0EM8JyBLCRjUJIDR/UbYKHH9DiDT9gnQ7oDZ3p9YUWdi4FQrqlW0eQKE+Avg0iUCJBSwYQAAAABJRU5ErkJggg=="
private const val BLANK_ICON = "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAyNpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDYuMC1jMDA2IDc5LjE2NDY0OCwgMjAyMS8wMS8xMi0xNTo1MjoyOSAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIgeG1sbnM6c3RSZWY9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZVJlZiMiIHhtcDpDcmVhdG9yVG9vbD0iQWRvYmUgUGhvdG9zaG9wIDIyLjIgKFdpbmRvd3MpIiB4bXBNTTpJbnN0YW5jZUlEPSJ4bXAuaWlkOjAxNEE0NkRDMzlEOTExRUM5MEYyQUEwM0YwMjI1OTVFIiB4bXBNTTpEb2N1bWVudElEPSJ4bXAuZGlkOjAxNEE0NkREMzlEOTExRUM5MEYyQUEwM0YwMjI1OTVFIj4gPHhtcE1NOkRlcml2ZWRGcm9tIHN0UmVmOmluc3RhbmNlSUQ9InhtcC5paWQ6MDE0QTQ2REEzOUQ5MTFFQzkwRjJBQTAzRjAyMjU5NUUiIHN0UmVmOmRvY3VtZW50SUQ9InhtcC5kaWQ6MDE0QTQ2REIzOUQ5MTFFQzkwRjJBQTAzRjAyMjU5NUUiLz4gPC9yZGY6RGVzY3JpcHRpb24+IDwvcmRmOlJERj4gPC94OnhtcG1ldGE+IDw/eHBhY2tldCBlbmQ9InIiPz5H9fbNAAAAZUlEQVR42uzQAQEAAAQDMPTvfD3YIqyT1GdTzwkQIECAAAECBAgQIECAAAECBAgQIECAAAECBAgQIECAAAECBAgQIECAAAECBAgQIECAAAECBAgQIECAAAECBAgQIECAgAtWgAEAVbYDfaostxgAAAAASUVORK5CYII="
private const val MEDIUM_REEF_ICON = "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAMAAACdt4HsAAAABGdBTUEAALGPC/xhBQAAAAFzUkdCAK7OHOkAAACcUExURQAAAMsAeAD/CLUncP+DAMkAdy/q8LMAakoYOwAAAP/wABgACswnfcgAdl0A/7EAaWr1+gA+//z7+7QAav8AAPj4+Pv4+f39/bMnb/n4+bc4dVIEPPr4+coAd7MBaowAU7QBassBeBoADff29r77/Pr///n294sAUrEBacgBdiUAEsgCdhoADLQAa/v7+/j298onfM04gckDd7EDaa1u/pUAAAABdFJOUwBA5thmAAABjElEQVRYw+2W23KCMBCGTWmMVSMVJK2UclBABM++/7s1S8gQlHGmNcz0gu8u6/7fsDFEB4Oenp6enm54VRhXvCjMK7oQLDgyOqqQcbmW8VmFPsFi0Yxvt5QDERGv12p8Nvvi6BA04yCgNAgwhogQ1OvbeKnQImhuH6WbTRCIh25bN9EhUOOUYozQNwchXHG7bvK8QI0XxVQhilBJFLVVJToFRXE+p6nHsTiMuW4Y2nYYuu59tUaXAAY4ndJ0WOI4HxzGDIMQwzge76s1egTjMXyB0+l6PRy+c0SzZQlBW1WvAC6O0QjjywUa4zhJRLNovV7bqjoF4uoEgRwhSeIYmpdLdYRmtSuB54ntUh/WMCzLceptfDjCnwVwjEDAGLTKQyMPEmOHw21Vv4BSeG1d1/OWJeK18X1CfL+tqlcAVzdcXAiFocH5rMgysyTLZAU+bcZ1CMSPh7g6bZsQ08zzSSt5bprkDp2C/R4KpjmZvLWyWnUtgAH+t2C360ownz85wpMC+OMEAnGMfi/4ARHPgmVkI5vBAAAAAElFTkSuQmCC"
object MainDemo
{
    @JvmStatic
    fun main(args: Array<String>)
    {

        instanceContainer.enableAutoChunkLoad(true)

        // Set the ChunkGenerator
        instanceContainer.setGenerator() { it.modifier().fillHeight(0, 39, Block.STONE)}

        var tnt = Block.TNT
        // Create a new block with the specified handler.
        // Be aware that block objects can be reused, handlers should
        // therefore never assume to be assigned to a single block.
        // Create a new block with the specified handler.
        // Be aware that block objects can be reused, handlers should
        // therefore never assume to be assigned to a single block.
        tnt = tnt.withHandler(DemoHandler())

        //Commands
        MinecraftServer.getCommandManager().register(TestCommand())
        MinecraftServer.getCommandManager().register(GamemodeCommand())
        MinecraftServer.getCommandManager().register(SummonCommand())
        MinecraftServer.getCommandManager().register(PvpCommand())
        MinecraftServer.getCommandManager().register(FillCommand())
        MinecraftServer.getCommandManager().register(ParkourCommand())
        MinecraftServer.getCommandManager().register(TimeCommand())

        globalEventHandler.addListener(ServerListPingEvent::class.java, this::onServerListPing)

        registerCombatListeners()

        globalEventHandler.addListener(PlayerBlockPlaceEvent::class.java) { event: PlayerBlockPlaceEvent ->
            if (event.block == Block.TNT)
                event.block = tnt
        }

        MinecraftServer.getConnectionManager().setPlayerProvider(ParkourPlayerProvider())

        ZombieCreature().setInstance(instanceContainer, Pos(0.0, 42.0, 0.0))
        EntityCreature(EntityType.BOAT).setInstance(instanceContainer, Pos(5.0, 42.0, 0.0))

        // Start the server on port 25565
        minecraftServer.start("0.0.0.0", 25565)
    }

    //Registers left and right click attack events.
    private fun registerCombatListeners()
    {
        globalEventHandler.addListener(PlayerEntityInteractEvent::class.java) { event: PlayerEntityInteractEvent ->
            if (event.hand == Player.Hand.OFF)
                CombatUtils.hit(event.target, event.entity, true)
        }

        globalEventHandler.addListener(EntityAttackEvent::class.java) { event: EntityAttackEvent ->
            CombatUtils.hit(event.target, event.entity)
        }
    }

    private fun onServerListPing(event: ServerListPingEvent)
    {
        val responseData = event.responseData

        println(event.connection?.identifier)

        when (event.pingType)
        {
            (ServerListPingType.MODERN_FULL_RGB) ->
            {
                responseData.protocol = -1
                responseData.version = "§d*:･ﾟ✧ silly name ->                                                       §aONLINE"
                responseData.description = MiniMessage.miniMessage().deserialize("<rainbow>This is a test!</rainbow>")
                responseData.favicon = "data:image/png;base64," + MEDIUM_REEF_ICON
            }
            else ->
            {
            }
        }
    }


    var playersMovedLastTick = mutableListOf<UUID>()
}