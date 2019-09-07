package cam72cam.immersiverailroading.entity;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EntitySmokeParticle extends Entity {

	public final double rot;
	public final int lifespan;
	public final float darken;
	public final float thickness;
	public final double diameter;
	public double radius;
	public float alpha;


	public EntitySmokeParticle(World worldIn, int lifespan, float darken, float thickness, double diameter) {
		super(worldIn);
		this.rot = Math.random() * 360;
		this.lifespan = lifespan;
		this.darken = darken;
		this.thickness = thickness;
		this.diameter = diameter;
	}
	
	@Override
	public boolean shouldRenderInPass(int pass)
    {
        return false;
    }
	
	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getRenderBoundingBox() {
		return new AxisAlignedBB(radius, radius, radius, -radius, -radius, -radius).offset(this.getPositionVector());
	}
	

	private static int hackID = -1; 
	@Override
	protected void entityInit() {
		hackID --;
		this.setEntityId(-31415 + hackID);
		if (hackID < -31415 * 10) {
			hackID = -31415;
		}
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound compound) {
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound compound) {
	}
	

	@Override
	public void onUpdate() {
		if (!world.isRemote) {
			return;
		}
		
		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;

		double life = ticksExisted  / (float)this.lifespan;
		
		double expansionRate = 16;
		
		this.radius = this.diameter * (Math.sqrt(life)*expansionRate+1) * 0.5;
		
		this.alpha = (thickness+0.2f) * (1-(float)Math.sqrt(life));

		// Slow down Y drift
		this.motionY *= 0.9f;
			
		this.motionX *= 0.97f;
		this.motionZ *= 0.97f;
		
		this.posX += this.motionX;
		this.posY += this.motionY;
		this.posZ += this.motionZ;
		
		if (this.motionX != 0 && world.getBlockState(new BlockPos(this.getPositionVector().addVector(0, this.motionX*2, 0))).causesSuffocation()) {
			this.motionX = 0;
		}
		if (this.motionY != 0 && world.getBlockState(new BlockPos(this.getPositionVector().addVector(0, this.motionY*2, 0))).causesSuffocation()) {
			this.motionY = 0;
		}
		if (this.motionZ != 0 && world.getBlockState(new BlockPos(this.getPositionVector().addVector(0, this.motionZ*2, 0))).causesSuffocation()) {
			this.motionZ = 0;
		}
		
		if (ticksExisted >= this.lifespan) {
			world.removeEntity(this);
		}
	}

}
