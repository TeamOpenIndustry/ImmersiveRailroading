package cam72cam.immersiverailroading.entity;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class EntitySmokeParticle extends Entity {

	public final double rot;
	public final int lifespan;
	public final float darken;
	public final float thickness;
	public final double size;


	public EntitySmokeParticle(World worldIn, int lifespan, float darken, float thickness, double size) {
		super(worldIn);
		this.rot = Math.random() * 360;
		this.lifespan = lifespan;
		this.darken = darken;
		this.thickness = thickness;
		this.size = size;
	}
	
	@Override
	public boolean shouldRenderInPass(int pass)
    {
        return pass == 1;
    }
	

	@Override
	protected void entityInit() {		
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound compound) {
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound compound) {
	}
	

	@Override
	public void onUpdate() {
		//super.onUpdate();
		
		if (!world.isRemote) {
			return;
		}
		
		//this.setPosition(this.posX, this.posY+0.2*(this.ticksExisted/20.0), this.posZ);
		//this.posY += this.posY+0.2*(this.ticksExisted/20.0);
		//this.setPosition(this.posX, this.posY+0.2, this.posZ);
		
		double calcY = 0.2*(this.ticksExisted/20.0);
		
		//this.motionY = 0.2 * (this.ticksExisted/20.0);
		this.motionX *= 0.97f;
		if (this.motionY > calcY) {
			this.motionY *= 0.97f;
		}
		this.motionZ *= 0.97f;
		
		this.posX += this.motionX;
		this.posY += this.motionY;
		this.posZ += this.motionZ;
		
		if (this.ticksExisted >= this.lifespan) {
			world.removeEntity(this);
		}
	}

}
