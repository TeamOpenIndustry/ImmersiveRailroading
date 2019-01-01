package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.registry.CarFreightDefinition;
import cam72cam.immersiverailroading.sound.ISound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class CarFreight extends Freight {
	private ISound squealsound;
	
	public CarFreight(World world) {
		this(world, null);
	}
	
	public CarFreight(World world, String defID) {
		super(world, defID);
	}
	
	@Override
	public CarFreightDefinition getDefinition() {
		return super.getDefinition(CarFreightDefinition.class);
	}

	@Override
	public int getInventorySize() {
		return this.getDefinition().getInventorySize(gauge);
	}
	
	public int getInventoryWidth() {
		return this.getDefinition().getInventoryWidth(gauge);
	}
	@Override
	public void onUpdate() {
		super.onUpdate();	
		
		if(this.getCurrentSpeed().metric() >= 35) {
			if (this.squealsound == null) {
				squealsound = ImmersiveRailroading.proxy.newSound(new ResourceLocation("immersiverailroading:sounds/misc/squeal_1.ogg"), true, 40, gauge);
				squealsound.setPitch((float) (0.2*this.getCurrentSpeed().metric()));
			}
						
		}
	}
	
	//TODO filter inventory
}
