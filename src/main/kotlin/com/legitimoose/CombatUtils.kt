package com.legitimoose

import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.EntityDamage
import kotlin.math.cos
import kotlin.math.sin

object CombatUtils
{
    fun applyKnockback()
    {
        if (event.target is LivingEntity)
        {
            val attacker = event.entity
            val target = event.target as LivingEntity

            //Damage
            target.damage(EntityDamage(event.entity), 0f)

            //Knockback
            val strength: Float = (0.4f * (if (attacker.isSprinting) 1.5f else 1f)).toFloat()

            target.takeKnockback(strength, sin(attacker.position.yaw * (Math.PI/180)), -cos(attacker.position.yaw * (Math.PI/180)))
            if (attacker is Player)
                attacker.sendMessage("Stregnth: $strength")
        }
    }
}