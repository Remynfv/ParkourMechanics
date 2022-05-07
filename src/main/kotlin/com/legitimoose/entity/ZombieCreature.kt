package com.legitimoose.entity

import com.legitimoose.instanceContainer
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityCreature
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.ai.EntityAIGroupBuilder
import net.minestom.server.entity.ai.TargetSelector

class ZombieCreature : EntityCreature(EntityType.ZOMBIE)
{
    init
    {
        addAIGroup(EntityAIGroupBuilder()
//            .addGoalSelector(MeleeAttackGoal(this, 1.0, Duration.ofSeconds(1)))
            .addTargetSelector(PlayerSelector(this))
            .build())
    }
}

class PlayerSelector(entityCreature: EntityCreature) : TargetSelector(entityCreature)
{
    override fun findTarget(): Entity?
    {
        if (instanceContainer.players.isEmpty())
            return null
        return instanceContainer.players.first()
    }

}