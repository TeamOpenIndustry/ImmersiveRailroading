package cam72cam.immersiverailroading.thirdparty.opencomputers;

import java.util.HashMap;
import java.util.Map;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.Freight;
import cam72cam.immersiverailroading.entity.FreightTank;
import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.library.Augment;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveDefinition;
import cam72cam.immersiverailroading.tile.TileRailBase;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.prefab.AbstractValue;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class AugmentDriver implements DriverBlock {

	@Override
	public ManagedEnvironment createEnvironment(World world, BlockPos pos, EnumFacing facing) {
		return new AugmentManager(world, pos);
	}

	@Override
	public boolean worksWith(World world, BlockPos pos, EnumFacing facing) {
		return TileRailBase.get(world, pos) != null;
	}
	
	public class AugmentManager extends AbstractManagedEnvironment implements NamedBlock {

		private final World world;
		private final BlockPos pos;

		public AugmentManager(World world, BlockPos pos) {
			this.world = world;
			this.pos = pos;
			setNode(Network.newNode(this, Visibility.Network).withComponent("ir_augment", Visibility.Network).create());
		}
		
		@Callback(doc = "function():string -- returns the current augment type")
		public Object[] getAugmentType(Context context, Arguments args)
		{
			Augment augment = TileRailBase.get(world, pos).getAugment();
			if (augment != null) {
				return new Object[]{augment.toString()};
			}
			return null;
		}
		
		@Callback(doc = "function():table -- returns the augment (if it exists)")
		public Object[] getAugment(Context context, Arguments args)
		{
			AbstractValue val = null;
			
			Augment augment = TileRailBase.get(world, pos).getAugment();
			
			if (augment == null) {
				return null;
			}
			
			switch (augment) {
			case DETECTOR:
				val = new DetectorAugment();
				break;
			case LOCO_CONTROL:
				val = new LocoControlAugment();
				break;
			default:
				break;
			}
			return new Object[] {val};
		}
		
		private FluidStack getFluid() {
			TileRailBase te = TileRailBase.get(world, pos);
			Capability<IFluidHandler> capability = CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
			EntityMoveableRollingStock stock = te.getStockNearBy(capability);
			if (stock != null) {
				IFluidHandler fh = stock.getCapability(capability, null);
				return fh.drain(Integer.MAX_VALUE, false);
			}
			return null;
		}
		
		public class DetectorAugment extends AbstractValue {
			@Callback(doc = "function():table -- returns an info dump about the current car")
			public Object[] carInfo(Context context, Arguments arguments) throws Exception {
				TileRailBase te = TileRailBase.get(world, pos);
				EntityMoveableRollingStock stock = te.getStockNearBy(null);
				if (stock != null) {
					Map<String, Object> info = new HashMap<String, Object>();
					EntityRollingStockDefinition def = stock.getDefinition();
					
					info.put("id", def.defID);
					info.put("name", def.name);
					
					info.put("passengers", stock.getPassengers().size());
					info.put("speed", stock.getCurrentSpeed().metric());
					info.put("weight", stock.getWeight());

					if (stock instanceof Locomotive) {
						LocomotiveDefinition locoDef = ((Locomotive)stock).getDefinition();
						info.put("horsepower", locoDef.getHorsePower(stock.gauge));
						info.put("traction", locoDef.getStartingTractionNewtons(stock.gauge));
						info.put("max_speed", locoDef.getMaxSpeed(stock.gauge).metric());
						
						Locomotive loco = (Locomotive)stock;
						info.put("brake", loco.getAirBrake());
						info.put("throttle", loco.getThrottle());
						
						if (stock instanceof LocomotiveSteam) {
							LocomotiveSteam steam = (LocomotiveSteam)stock;
							info.put("pressure", steam.getBoilerPressure());
							info.put("temperature", steam.getBoilerTemperature());
						}
					}
					
					FluidStack fluid = getFluid();
					if (fluid != null) {
						info.put("fluid_type", fluid.getFluid().getName());
						info.put("fluid_amount", fluid.amount);
					} else {
						info.put("fluid_type", null);
						info.put("fluid_amount", 0);
					}
					if (stock instanceof FreightTank) {
						info.put("fluid_max", ((FreightTank)stock).getTankCapacity().MilliBuckets());
					}
					
					if (stock instanceof Freight) {
						Freight freight = ((Freight)stock);
						info.put("cargo_percent", freight.getPercentCargoFull());
						info.put("cargo_size", freight.getInventorySize());
					}
					return new Object[] { info };
				}
				return null;
			}
		}
		
		public class LocoControlAugment extends AbstractValue {
			@Callback(doc = "function(double) -- sets the locomotive throttle")
			public Object[] setThrottle(Context context, Arguments arguments) throws Exception {
				TileRailBase te = TileRailBase.get(world, pos);
				Locomotive stock = te.getStockNearBy(Locomotive.class, null);
				if (stock != null) {
					//stock = (Locomotive) stock.findByUUID(stock.getPersistentID());
					stock.setThrottle((float) arguments.checkDouble(0));
				}
				return null;
			}
			
			@Callback(doc = "function(double) -- sets the locomotive brake")
			public Object[] setBrake(Context context, Arguments arguments) throws Exception {
				TileRailBase te = TileRailBase.get(world, pos);
				Locomotive stock = te.getStockNearBy(Locomotive.class, null);
				if (stock != null) {
					//stock = (Locomotive) stock.findByUUID(stock.getPersistentID());
					stock.setAirBrake((float) arguments.checkDouble(0));
				}
				return null;
			}
			
			@Callback(doc = "function() -- fires the locomotive horn")
			public Object[] horn(Context context, Arguments arguments) throws Exception {
				TileRailBase te = TileRailBase.get(world, pos);
				Locomotive stock = te.getStockNearBy(Locomotive.class, null);
				if (stock != null) {
					//stock = (Locomotive) stock.findByUUID(stock.getPersistentID());
					stock.setHorn(5);
				}
				return null;
			}
		}
		
		@Override
		public String preferredName()
		{
			return "ir_augment";
		}

		@Override
		public int priority() {
			return 0;
		}
	}
}