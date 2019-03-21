package cam72cam.immersiverailroading.entity;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.proxy.ChunkManager;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

public class EntityArtilleryStrike extends Entity {

	@SuppressWarnings("unchecked")
	private static final Predicate<Entity> ARROW_TARGETS = Predicates.and(EntitySelectors.NOT_SPECTATING, EntitySelectors.IS_ALIVE, new Predicate<Entity>()
    {
        public boolean apply(@Nullable Entity p_apply_1_)
        {
            return p_apply_1_.canBeCollidedWith();
        }
    });
	private float projectileMass;
	private float projectileSpeed;
	private float projectileExplosive;
	private int impactTimeTicks;
	
    private int xTile;
    private int yTile;
    private int zTile;
    /** The owner of this arrow. */
    public Entity shootingEntity;

	
	public EntityArtilleryStrike(World worldIn) {
		super(worldIn);
		projectileMass = 0;
		projectileSpeed = 0;
		projectileExplosive = 0;
		impactTimeTicks = 0;
	}
	
	public EntityArtilleryStrike(World worldIn, Vec3d pos, float mass, float speed, float explosive, int timer) {
		super(worldIn);
        this.setPosition(pos.x, pos.y, pos.z);
		projectileMass = mass;
		projectileSpeed = speed;
		projectileExplosive = explosive;
		impactTimeTicks = timer;
	}

	@Override
	protected void entityInit() {}

	@Override
	public void onUpdate() {
		super.onUpdate();
		impactTimeTicks -= 1;
		ChunkManager.flagEntityPos(this.world, this.getPosition());
		
		if (impactTimeTicks <= -1) {
			if (this.ticksExisted % 20 == 0) ImmersiveRailroading.info("Incoming strike at %f,%f ; T%d", this.getPositionVector().x, this.getPositionVector().z, -impactTimeTicks);
			this.motionY -= 0.05000000074505806D;
	        this.setPosition(this.posX, this.posY, this.posZ);
	        this.doBlockCollisions();
			
	        BlockPos blockpos = new BlockPos(this.xTile, this.yTile, this.zTile);
	        IBlockState iblockstate = this.world.getBlockState(blockpos);
	        Block block = iblockstate.getBlock();

	        if (iblockstate.getMaterial() != Material.AIR)
	        {
	            AxisAlignedBB axisalignedbb = iblockstate.getCollisionBoundingBox(this.world, blockpos);

	            if (axisalignedbb != Block.NULL_AABB && axisalignedbb.offset(blockpos).contains(new Vec3d(this.posX, this.posY, this.posZ)))
	            {
	                impact(this.getPosition());
	            }
	        }

	        {
	            Vec3d vec3d1 = new Vec3d(this.posX, this.posY, this.posZ);
	            Vec3d vec3d = new Vec3d(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
	            RayTraceResult raytraceresult = this.world.rayTraceBlocks(vec3d1, vec3d, false, true, false);
	            vec3d1 = new Vec3d(this.posX, this.posY, this.posZ);
	            vec3d = new Vec3d(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);

	            if (raytraceresult != null)
	            {
	                vec3d = new Vec3d(raytraceresult.hitVec.x, raytraceresult.hitVec.y, raytraceresult.hitVec.z);
	            }

	            Entity entity = this.findEntityOnPath(vec3d1, vec3d);

	            if (entity != null)
	            {
	                raytraceresult = new RayTraceResult(entity);
	            }

	            if (raytraceresult != null && raytraceresult.entityHit instanceof EntityPlayer)
	            {
	                EntityPlayer entityplayer = (EntityPlayer)raytraceresult.entityHit;

	                if (this.shootingEntity instanceof EntityPlayer && !((EntityPlayer)this.shootingEntity).canAttackPlayer(entityplayer))
	                {
	                    raytraceresult = null;
	                }
	            }

	            if (raytraceresult != null && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, raytraceresult))
	            {
	                impact(raytraceresult.getBlockPos());
	            }

	            this.posX += this.motionX;
	            this.posY += this.motionY;
	            this.posZ += this.motionZ;
	            float f4 = MathHelper.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
	            this.rotationYaw = (float)(MathHelper.atan2(this.motionX, this.motionZ) * (180D / Math.PI));

	            for (this.rotationPitch = (float)(MathHelper.atan2(this.motionY, (double)f4) * (180D / Math.PI)); this.rotationPitch - this.prevRotationPitch < -180.0F; this.prevRotationPitch -= 360.0F)
	            {
	                ;
	            }

	            while (this.rotationPitch - this.prevRotationPitch >= 180.0F)
	            {
	                this.prevRotationPitch += 360.0F;
	            }

	            while (this.rotationYaw - this.prevRotationYaw < -180.0F)
	            {
	                this.prevRotationYaw -= 360.0F;
	            }

	            while (this.rotationYaw - this.prevRotationYaw >= 180.0F)
	            {
	                this.prevRotationYaw += 360.0F;
	            }

	            this.rotationPitch = this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * 0.2F;
	            this.rotationYaw = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw) * 0.2F;
	            float f1 = 0.99F;
	            float f2 = 0.05F;

	            if (this.isInWater())
	            {
	                for (int i = 0; i < 4; ++i)
	                {
	                    float f3 = 0.25F;
	                    this.world.spawnParticle(EnumParticleTypes.WATER_BUBBLE, this.posX - this.motionX * 0.25D, this.posY - this.motionY * 0.25D, this.posZ - this.motionZ * 0.25D, this.motionX, this.motionY, this.motionZ);
	                }

	                f1 = 0.6F;
	            }

	            if (this.isWet())
	            {
	                this.extinguish();
	            }

	            this.motionX *= (double)f1;
	            this.motionY *= (double)f1;
	            this.motionZ *= (double)f1;

	            if (!this.hasNoGravity())
	            {
	                this.motionY -= 0.05000000074505806D;
	            }

	            this.setPosition(this.posX, this.posY, this.posZ);
	            this.doBlockCollisions();
	        }
		} else {
			if (this.ticksExisted % 20 == 0) ImmersiveRailroading.info("Strike in flight to %f,%f ; T%d", this.getPositionVector().x, this.getPositionVector().z, -impactTimeTicks);
		}
	}
	
	protected void impact(BlockPos pos) {
		Vec3d posVec = new Vec3d(pos);
		ImmersiveRailroading.info("Impact at %f,%f,%f ; T%d", posVec.x, posVec.y, posVec.z, -impactTimeTicks);
		if (this.projectileExplosive > 0) {
			world.createExplosion(this, posVec.x, posVec.y, posVec.z,
					Config.ConfigDamage.explosionsEnabled ? projectileExplosive : 0f, 
					Config.ConfigDamage.explosionEnvDamageEnabled);
		} else {
			world.createExplosion(this, posVec.x, posVec.y, posVec.z,
				Config.ConfigDamage.explosionsEnabled ? (float)(0.5 * projectileMass * Math.pow(projectileSpeed, 2)) : 0f, 
				Config.ConfigDamage.explosionEnvDamageEnabled);
		}
		this.setDead();
	}
	
    @Nullable
    protected Entity findEntityOnPath(Vec3d start, Vec3d end)
    {
        Entity entity = null;
        List<Entity> list = this.world.getEntitiesInAABBexcluding(this, this.getEntityBoundingBox().expand(this.motionX, this.motionY, this.motionZ).grow(1.0D), ARROW_TARGETS);
        double d0 = 0.0D;

        for (int i = 0; i < list.size(); ++i)
        {
            Entity entity1 = list.get(i);

            if (entity1 != this.shootingEntity || this.impactTimeTicks <= -5)
            {
                AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().grow(0.30000001192092896D);
                RayTraceResult raytraceresult = axisalignedbb.calculateIntercept(start, end);

                if (raytraceresult != null)
                {
                    double d1 = start.squareDistanceTo(raytraceresult.hitVec);

                    if (d1 < d0 || d0 == 0.0D)
                    {
                        entity = entity1;
                        d0 = d1;
                    }
                }
            }
        }

        return entity;
    }
	
	@Override
	protected void readEntityFromNBT(NBTTagCompound compound) {
		projectileMass = compound.getFloat("mass");
		projectileSpeed = compound.getFloat("speed");
		projectileExplosive = compound.getFloat("explosive");
		impactTimeTicks = compound.getInteger("timer");
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound compound) {
		compound.setFloat("mass", projectileMass);
		compound.setFloat("speed", projectileSpeed);
		compound.setFloat("explosive", projectileExplosive);
		compound.setInteger("timer", impactTimeTicks);
	}

	
	/*
	@Override
    public boolean isInvisible()
    {
        return true;
    }
    */
}
