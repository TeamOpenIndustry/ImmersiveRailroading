package cam72cam.immersiverailroading.entity.locomotives;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.Speed;
import cam72cam.immersiverailroading.entity.SteamLocomotive;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class Shay extends Entity {

	public Shay(World world) {
		super(world);
		System.out.println("CONSTRUCT");
		
	}
	
	@Override
	public void onUpdate() {
		//System.out.println("ALIVE!");
	}
/*
	@Override
	public int getWaterConsumption() {
		return 100;
	}

	@Override
	public void updatePassenger(Entity passenger) {
		//TODO
	}

	@Override
	public int getDefaultFuelConsumption() {
		return 100;
	}

	@Override
	public int getDefaultPower() {
		return 100;
	}

	@Override
	public double getDefaultAccel() {
		return 100;
	}

	@Override
	public double getDefaultBrake() {
		return 100;
	}

	@Override
	public Speed getMaxSpeed() {
		return Speed.fromMetric(100);
	}

	@Override
	public int getTankCapacity() {
		return 1000;
	}

	@Override
	public double getMountedYOffset() {
		return 0;
	}*/

	@Override
	protected void entityInit() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound compound) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound compound) {
		// TODO Auto-generated method stub
		
	}

}
