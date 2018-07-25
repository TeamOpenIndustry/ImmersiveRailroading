package cam72cam.immersiverailroading.entity.ai;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import cam72cam.immersiverailroading.Config.ConfigBalance;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.Vec3d;

public class EntityAIAvoidTrain<T extends Entity> extends EntityAIBase
{
    private final Predicate<Entity> canBeSeenSelector;
    /** The entity we are attached to */
    protected EntityCreature entity;
    private final double farSpeed;
    private final double nearSpeed;
    protected T closestLivingEntity;
    private final float avoidDistance;
    /** The PathEntity of our entity */
    private Path path;
    /** The PathNavigate of our entity */
    private final PathNavigate navigation;
    /** Class of entity this behavior seeks to avoid */
    private final Class<T> classToAvoid;
    private final Predicate <? super T > avoidTargetSelector;

    public EntityAIAvoidTrain(EntityCreature entityIn, Class<T> classToAvoidIn, float avoidDistanceIn, double farSpeedIn, double nearSpeedIn)
    {
        this(entityIn, classToAvoidIn, Predicates.alwaysTrue(), avoidDistanceIn, farSpeedIn, nearSpeedIn);
    }

    public EntityAIAvoidTrain(EntityCreature entityIn, Class<T> classToAvoidIn, Predicate <? super T > avoidTargetSelectorIn, float avoidDistanceIn, double farSpeedIn, double nearSpeedIn)
    {
        this.canBeSeenSelector = new Predicate<Entity>()
        {
            public boolean apply(@Nullable Entity p_apply_1_)
            {
                return p_apply_1_.isEntityAlive() && EntityAIAvoidTrain.this.entity.getEntitySenses().canSee(p_apply_1_) && !EntityAIAvoidTrain.this.entity.isOnSameTeam(p_apply_1_);
            }
        };
        this.entity = entityIn;
        this.classToAvoid = classToAvoidIn;
        this.avoidTargetSelector = avoidTargetSelectorIn;
        this.avoidDistance = avoidDistanceIn;
        this.farSpeed = farSpeedIn;
        this.nearSpeed = nearSpeedIn;
        this.navigation = entityIn.getNavigator();
        this.setMutexBits(8);
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    @SuppressWarnings("unchecked")
	public boolean shouldExecute()
    {
        List<T> list = this.entity.world.<T>getEntitiesWithinAABB(this.classToAvoid, this.entity.getEntityBoundingBox().grow((double)this.avoidDistance, 3.0D, (double)this.avoidDistance), Predicates.and(EntitySelectors.CAN_AI_TARGET, this.canBeSeenSelector, this.avoidTargetSelector));
        if (list.isEmpty())
        {
            return false;
        }
        else
        {
        	this.closestLivingEntity = list.get(0);
            if(this.closestLivingEntity instanceof EntityRollingStock) {
            	EntityRollingStock stock = (EntityRollingStock) this.closestLivingEntity;
            	if(stock.shouldEntitiesFlee()) {
	            	Vec3d vec3d = RandomPositionGenerator.findRandomTargetBlockAwayFrom(this.entity, 16, 7, new Vec3d(this.closestLivingEntity.posX, this.closestLivingEntity.posY, this.closestLivingEntity.posZ));
	
	                if (vec3d == null)
	                {
	                    return false;
	                }
	                else if (this.closestLivingEntity.getDistanceSq(vec3d.x, vec3d.y, vec3d.z) < this.closestLivingEntity.getDistanceSq(this.entity))
	                {
	                    return false;
	                }
	                else
	                {
	                    this.path = this.navigation.getPathToXYZ(vec3d.x, vec3d.y, vec3d.z);
	                    return this.path != null;
	                }
            	} else {
            		return false;
            	}
            } else {
            	return false;
            }
        }
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    public boolean shouldContinueExecuting()
    {
    	@SuppressWarnings("unchecked")
		List<T> list = this.entity.world.<T>getEntitiesWithinAABB(this.classToAvoid, this.entity.getEntityBoundingBox().grow((double)this.avoidDistance, 3.0D, (double)this.avoidDistance), Predicates.and(EntitySelectors.CAN_AI_TARGET, this.canBeSeenSelector, this.avoidTargetSelector));
        if (list.isEmpty())
        {
            return false;
        }
        else
        {
        	this.closestLivingEntity = list.get(0);
            if(this.closestLivingEntity instanceof EntityRollingStock) {
            	EntityRollingStock stock = (EntityRollingStock) this.closestLivingEntity;
            	if(stock.shouldEntitiesFlee()) {
	            	Vec3d vec3d = RandomPositionGenerator.findRandomTargetBlockAwayFrom(this.entity, ConfigBalance.mobFleeDistance, 7, new Vec3d(this.closestLivingEntity.posX, this.closestLivingEntity.posY, this.closestLivingEntity.posZ));
	
	                if (vec3d == null)
	                {
	                    return false;
	                }
	                else if (this.closestLivingEntity.getDistanceSq(vec3d.x, vec3d.y, vec3d.z) < this.closestLivingEntity.getDistanceSq(this.entity))
	                {
	                    return false;
	                }
	                else
	                {
	                    this.path = this.navigation.getPathToXYZ(vec3d.x, vec3d.y, vec3d.z);
	                    return this.path != null;
	                }
            	} else {
            		return false;
            	}
            } else {
            	return false;
            }
        }
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void startExecuting()
    {
        this.navigation.setPath(this.path, this.farSpeed);
    }

    /**
     * Reset the task's internal state. Called when this task is interrupted by another one
     */
    public void resetTask()
    {
        this.closestLivingEntity = null;
    }

    /**
     * Keep ticking a continuous task that has already been started
     */
    public void updateTask()
    {
    	if (this.entity.getDistanceSq(this.closestLivingEntity) < 49.0D)
        {
            this.entity.getNavigator().setSpeed(this.nearSpeed);
        }
        else
        {
            this.entity.getNavigator().setSpeed(this.farSpeed);
        }
    }
}