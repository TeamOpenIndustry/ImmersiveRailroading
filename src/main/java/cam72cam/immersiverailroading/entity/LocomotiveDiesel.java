package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;

import blusunrize.immersiveengineering.api.energy.DieselHandler;
import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.library.RenderComponentType;
import cam72cam.immersiverailroading.model.RenderComponent;
import cam72cam.immersiverailroading.registry.LocomotiveDieselDefinition;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fluids.*;

public class LocomotiveDiesel extends Locomotive {

	public LocomotiveDiesel(World world) {
		this(world, null);
	}

	public LocomotiveDiesel(World world, String defID) {
		super(world, defID);
		//runSound.setDynamicPitch();
	}

	public LocomotiveDieselDefinition getDefinition() {
		return super.getDefinition(LocomotiveDieselDefinition.class);
	}
	
	@Override
	public GuiTypes guiType() {
		return GuiTypes.DIESEL_LOCOMOTIVE;
	}
	
	
	/*
	 * Sets the throttle or brake on all connected diesel locomotives if the throttle or brake has been changed
	 */
	@Override
	public void handleKeyPress(Entity source, KeyTypes key) {
		super.handleKeyPress(source, key);
		
		this.mapTrain(this, true, false, this::setThrottleMap);
	}
	
	private void setThrottleMap(EntityRollingStock stock, boolean direction) {
		if (stock instanceof LocomotiveDiesel) {
			((LocomotiveDiesel) stock).setThrottle(this.getThrottle() * (direction ? 1 : -1));
			((LocomotiveDiesel) stock).setAirBrake(this.getAirBrake());
		}
	}
	
	@Override
	protected int getAvailableHP() {
		if (!Config.isFuelRequired(gauge)) {
			return this.getDefinition().getHorsePower(gauge);
		}
		return this.getLiquidAmount() > 0 ? this.getDefinition().getHorsePower(gauge) : 0;
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		
		if (world.isRemote) {
			if (!Config.particlesEnabled) {
				return;
			}
			
			Vec3d fakeMotion = new Vec3d(this.motionX, this.motionY, this.motionZ);//VecUtil.fromYaw(this.getCurrentSpeed().minecraft(), this.rotationYaw);
			
			List<RenderComponent> exhausts = this.getDefinition().getComponents(RenderComponentType.DIESEL_EXHAUST_X, gauge);
			float throttle = Math.abs(this.getThrottle());
			if (exhausts != null && throttle > 0 && (this.getLiquidAmount() > 0 || !Config.isFuelRequired(gauge))) {
				for (RenderComponent exhaust : exhausts) {
					Vec3d particlePos = this.getPositionVector().add(VecUtil.rotateYaw(exhaust.center(), this.rotationYaw + 180)).addVector(0, 0.35 * gauge.scale(), 0);
					
					double smokeMod = (1 + Math.min(1, Math.max(0.2, Math.abs(this.getCurrentSpeed().minecraft())*2)))/2;
					
					EntitySmokeParticle sp = new EntitySmokeParticle(world, (int) (40 * (1+throttle) * smokeMod), throttle, throttle, exhaust.width());
					
					particlePos = particlePos.subtract(fakeMotion);
					
					sp.setPosition(particlePos.x, particlePos.y, particlePos.z);
					sp.setVelocity(fakeMotion.x, fakeMotion.y + 0.4 * gauge.scale(), fakeMotion.z);
					world.spawnEntity(sp);
				}
			}
			return;
		}
		
		if (this.getLiquidAmount() > 0 && getThrottle() != 0) {
			int burnTime = DieselHandler.getBurnTime(this.getLiquid());
			if (burnTime == 0) {
				burnTime = 200; //Default to 200 for unregistered liquids
			}
			burnTime *= getDefinition().getFuelEfficiency()/100f;
			burnTime /= Math.abs(getThrottle())*10;
			burnTime *= 1/gauge.scale();
			if (this.ticksExisted % burnTime == 0) {
				theTank.drain(1, true);
			}
		}
	}
	
	@Override
	public List<Fluid> getFluidFilter() {
		ArrayList<Fluid> filter = new ArrayList<Fluid>();
		filter.add(FluidRegistry.getFluid("oil"));
		filter.add(FluidRegistry.getFluid("fuel"));
		filter.add(FluidRegistry.getFluid("diesel"));
		filter.add(FluidRegistry.getFluid("ethanol"));
		filter.add(FluidRegistry.getFluid("biofuel"));
		filter.add(FluidRegistry.getFluid("biodiesel"));
		return filter;
	}

	@Override
	public FluidQuantity getTankCapacity() {
		return this.getDefinition().getFuelCapacity(gauge);
	}
}