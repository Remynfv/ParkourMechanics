package com.legitimoose

import net.minestom.server.coordinate.Vec
import kotlin.math.cos
import kotlin.math.sin

//This is the REVERSE of knockback, because it gets the forwards direction instead of backwards. (???)
fun getVecFromYaw(yaw: Float): Vec
{
    return Vec(-sin(yaw * (Math.PI/180)), 0.0, cos(yaw * (Math.PI/180)))
}