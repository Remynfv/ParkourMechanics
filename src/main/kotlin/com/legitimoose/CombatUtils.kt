package com.legitimoose

import net.minestom.server.entity.Entity
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.EntityDamage
import net.minestom.server.network.packet.server.play.EntityAnimationPacket
import org.jglrxavpok.hephaistos.mca.pack
import kotlin.math.cos
import kotlin.math.sin

object CombatUtils
{
    fun applyKnockback(target: Entity, attacker: Entity, inward: Boolean = false)
    {
        if (target is LivingEntity)
        {
            //Damage
            target.damage(EntityDamage(attacker), 0f)

            //Knockback
            val strength: Float = (0.4f * (if (attacker.isSprinting) 2f else 1f)).toFloat()

            if (!inward)
                target.takeKnockback(strength, sin(attacker.position.yaw * (Math.PI/180)), -cos(attacker.position.yaw * (Math.PI/180)))
            else if (inward)
            {
                target.takeKnockback(strength, -sin(attacker.position.yaw * (Math.PI / 180)), cos(attacker.position.yaw * (Math.PI / 180)))
                if (attacker is LivingEntity)
                {
                    attacker.sendPacketToViewersAndSelf(EntityAnimationPacket(attacker.entityId, EntityAnimationPacket.Animation.SWING_MAIN_ARM))
                }

            }

            if (attacker is Player)
                attacker.sendMessage("Stregnth: $strength")
        }
    }
}