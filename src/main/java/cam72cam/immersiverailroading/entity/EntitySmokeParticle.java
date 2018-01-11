package cam72cam.immersiverailroading.entity;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class EntitySmokeParticle extends Entity {

	public final double rot;


	public EntitySmokeParticle(World worldIn) {
		super(worldIn);
		this.rot = Math.random() * 360;
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
		super.onUpdate();
		
		if (!world.isRemote) {
			return;
		}
		
		this.setPosition(this.posX, this.posY+0.2*(this.ticksExisted/20.0), this.posZ);
		//this.setPosition(this.posX, this.posY+0.2, this.posZ);
		
		if (this.ticksExisted % 160 == 0) {
			world.removeEntity(this);
		}
	}

}
