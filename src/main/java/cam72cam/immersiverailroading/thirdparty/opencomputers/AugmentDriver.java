package cam72cam.immersiverailroading.thirdparty.opencomputers;

import java.util.*;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.Freight;
import cam72cam.immersiverailroading.entity.FreightTank;
import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.entity.LocomotiveSteam;
import cam72cam.immersiverailroading.library.Augment;
import cam72cam.immersiverailroading.physics.PhysicsAccummulator;
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
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
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
		TileRailBase te = TileRailBase.get(world, pos);
		if (te == null) {
			return null;
		}

		Augment augment = te.getAugment();

		if (augment == null) {
			return null;
		}

		switch (augment) {
			case DETECTOR:
				return new DetectorAugment(world, pos);
			case LOCO_CONTROL:
				return new LocoControlAugment(world, pos);
			default:
				return null;
		}
	}

	@Override
	public boolean worksWith(World world, BlockPos pos, EnumFacing facing) {
		TileRailBase te = TileRailBase.get(world, pos);

		if (te == null) {
			return false;
		}

		Augment augment = te.getAugment();

		if (augment == null) {
			return false;
		}

		switch (augment) {
			case DETECTOR:
			case LOCO_CONTROL:
				return true;
			default:
				return false;
		}
	}

	public abstract class AugmentManagerBase extends AbstractManagedEnvironment implements NamedBlock {
		protected final World world;
		protected final BlockPos pos;
		private int ticksAlive;
		private UUID wasOverhead;
		protected Class<? extends EntityRollingStock> typeFilter = EntityRollingStock.class;

		public AugmentManagerBase(World world, BlockPos pos) {
			this.world = world;
			this.pos = pos;
			setNode(Network.newNode(this, Visibility.Network).withComponent(preferredName(), Visibility.Network).create());
		}

		@Override
		public boolean canUpdate() {
			return true;
		}

		@Override
		public void update() {
			Node node = this.node();
			if (this.ticksAlive == 0) {
				TileRailBase te = TileRailBase.get(world, pos);
				EntityRollingStock nearby = te.getStockNearBy(typeFilter, null);
				wasOverhead = nearby != null ? nearby.getPersistentID() : null;
			}

			if (node != null && this.ticksAlive % Math.max(Config.ConfigDebug.ocPollDelayTicks, 1) == 0) {
				TileRailBase te = TileRailBase.get(world, pos);
				EntityRollingStock nearby = te.getStockNearBy(typeFilter, null);
				UUID isOverhead = nearby != null ? nearby.getPersistentID() : null;
				if (isOverhead != wasOverhead) {
					Node neighbor = node.neighbors().iterator().next();
					neighbor.sendToReachable("computer.signal", "ir_train_overhead", te.getAugment().toString(), isOverhead == null ? null : isOverhead.toString());
				}

				wasOverhead = isOverhead;
			}

			this.ticksAlive +=1;
		}

		@Override
		public int priority() {
			return 3;
		}
	}

	public class DetectorAugment extends AugmentManagerBase {

		public DetectorAugment(World world, BlockPos pos) {
			super(world, pos);
		}

		@Override
		public String preferredName() {
			return "ir_augment_detector";
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

		public Map<String, Object> loco_info(Locomotive car) {
			TileRailBase te = TileRailBase.get(world, pos);
			EntityMoveableRollingStock stock = te.getStockNearBy(null);
			Map<String, Object> info = new HashMap<String, Object>();
			EntityRollingStockDefinition def = car.getDefinition();

			info.put("id", def.defID);
			info.put("name", def.name());
			info.put("tag", car.tag);
			EnumFacing dir = EnumFacing.fromAngle(car.rotationYaw);
			if (car.getCurrentSpeed().metric() < 0) {
				dir = dir.getOpposite();
			}
			info.put("direction", dir.toString());

			info.put("passengers", car.getPassengers().size() + stock.staticPassengers.size());
			info.put("speed", car.getCurrentSpeed().metric());
			info.put("weight", car.getWeight());
			LocomotiveDefinition locoDef = car.getDefinition();
			info.put("horsepower", locoDef.getHorsePower(car.gauge));
			info.put("traction", locoDef.getStartingTractionNewtons(car.gauge));
			info.put("max_speed", locoDef.getMaxSpeed(car.gauge).metric());
			info.put("brake", car.getAirBrake());
			info.put("throttle", car.getThrottle());

			if (car instanceof LocomotiveSteam) {
				LocomotiveSteam steam = (LocomotiveSteam) car;
				info.put("pressure", steam.getBoilerPressure());
				info.put("temperature", steam.getBoilerTemperature());
			}
			if (car instanceof LocomotiveDiesel) {
				info.put("temperature", ((LocomotiveDiesel) car).getEngineTemperature());
			}
			return info;
		}

		@Callback(doc = "function():table -- returns an info dump about the current car")
		public Object[] info(Context context, Arguments arguments) {
			TileRailBase te = TileRailBase.get(world, pos);
			EntityMoveableRollingStock stock = te.getStockNearBy(null);
			if (stock != null) {
				Map<String, Object> info = new HashMap<String, Object>();
				EntityRollingStockDefinition def = stock.getDefinition();

				info.put("id", def.defID);
				info.put("name", def.name());
				info.put("tag", stock.tag);
				EnumFacing dir = EnumFacing.fromAngle(stock.rotationYaw);
				if (stock.getCurrentSpeed().metric() < 0) {
					dir = dir.getOpposite();
				}
				info.put("direction", dir.toString());

				info.put("passengers", stock.getPassengers().size() + stock.staticPassengers.size());
				info.put("speed", stock.getCurrentSpeed().metric());
				info.put("weight", stock.getWeight());

				if (stock instanceof Locomotive) {
					info.put("locomotive", loco_info((Locomotive) stock));
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
					info.put("fluid_max", ((FreightTank) stock).getTankCapacity().MilliBuckets());
				}

				if (stock instanceof Freight) {
					Freight freight = ((Freight) stock);
					info.put("cargo_percent", freight.getPercentCargoFull());
					info.put("cargo_size", freight.getInventorySize());
				}
				return new Object[] { info };
			}
			return null;
		}

		@Callback(doc = "function():table -- returns an info dump about the current consist")
		public Object[] consist(Context context, Arguments arguments) {
			TileRailBase te = TileRailBase.get(world, pos);
			EntityCoupleableRollingStock stock = te.getStockNearBy(EntityCoupleableRollingStock.class, null);
			if (stock != null) {
				int locomotives=1;
				PhysicsAccummulator acc = new PhysicsAccummulator(stock.getCurrentTickPosAndPrune());
				stock.mapTrain(stock, true, true, acc::accumulate);
				Map<String, Object> info = new HashMap<String, Object>();

				info.put("cars", acc.count);
				info.put("tractive_effort_N", acc.tractiveEffortNewtons);
				info.put("weight_kg", acc.massToMoveKg);
				info.put("speed_km", stock.getCurrentSpeed().metric());
				EnumFacing dir = EnumFacing.fromAngle(stock.rotationYaw);
				if (stock.getCurrentSpeed().metric() < 0) {
					dir = dir.getOpposite();
				}
				info.put("direction", dir.toString());

				//List<Object> locos = new ArrayList<Object>();
				for (EntityCoupleableRollingStock car : stock.getTrain()) {
					if (car instanceof Locomotive) {
						info.put("locomotive_" + locomotives, loco_info((Locomotive) car));
						locomotives++;
					}
				}

//				for (EntityCoupleableRollingStock car : stock.getTrain()) {
//					if (car instanceof Locomotive) {
//
//						EntityRollingStockDefinition def = car.getDefinition();
//
//						info.put("LOCO_"+locomotiveCount+"_id", def.defID);
//						info.put("LOCO_"+locomotiveCount+"_name", def.name());
//						info.put("LOCO_"+locomotiveCount+"_tag", car.tag);
//						EnumFacing dir2 = EnumFacing.fromAngle(car.rotationYaw);
//						if (car.getCurrentSpeed().metric() < 0) {
//							dir2 = dir2.getOpposite();
//						}
//						info.put("LOCO_"+locomotiveCount+"_direction", dir2.toString());
//
//						info.put("LOCO_"+locomotiveCount+"_passengers", car.getPassengers().size() + stock.staticPassengers.size());
//						info.put("LOCO_"+locomotiveCount+"_speed", car.getCurrentSpeed().metric());
//						info.put("LOCO_"+locomotiveCount+"_weight", car.getWeight());
//						LocomotiveDefinition locoDef = ((Locomotive) car).getDefinition();
//						info.put("LOCO_"+locomotiveCount+"_horsepower", locoDef.getHorsePower(car.gauge));
//						info.put("LOCO_"+locomotiveCount+"_traction", locoDef.getStartingTractionNewtons(car.gauge));
//						info.put("LOCO_"+locomotiveCount+"_max_speed", locoDef.getMaxSpeed(car.gauge).metric());
//
//						Locomotive loco = (Locomotive) car;
//						info.put("LOCO_"+locomotiveCount+"_brake", loco.getAirBrake());
//						info.put("LOCO_"+locomotiveCount+"_throttle", loco.getThrottle());
//
//						if (car instanceof LocomotiveSteam) {
//							LocomotiveSteam steam = (LocomotiveSteam) car;
//							info.put("LOCO_"+locomotiveCount+"_pressure", steam.getBoilerPressure());
//							info.put("LOCO_"+locomotiveCount+"_temperature", steam.getBoilerTemperature());
//						}
//						if (car instanceof LocomotiveDiesel) {
//							info.put("LOCO_"+locomotiveCount+"_temperature", ((LocomotiveDiesel) car).getEngineTemperature());
//						}
//						locomotiveCount++;
				return new Object[] { info };
			}
			return null;
		}

		@Callback(doc = "function():table -- gets the stock's tag")
		public Object[] getTag(Context context, Arguments arguments) {
			TileRailBase te = TileRailBase.get(world, pos);
			EntityMoveableRollingStock stock = te.getStockNearBy(null);
			if (stock != null) {
				return new Object[] {stock.tag};
			}
			return null;
		}

		@Callback(doc = "function():table -- sets the stock's tag")
		public Object[] setTag(Context context, Arguments arguments) {
			TileRailBase te = TileRailBase.get(world, pos);
			EntityMoveableRollingStock stock = te.getStockNearBy(null);
			if (stock != null) {
				stock.tag = arguments.checkString(0);
			}
			return null;
		}

		@Callback(doc = "function():string -- returns the current augment type")
		public Object[] getAugmentType(Context context, Arguments args) {
			Augment augment = TileRailBase.get(world, pos).getAugment();
			if (augment != null) {
				return new Object[] { augment.toString() };
			}
			return null;
		}

		@Callback(doc = "function():array -- returns the position of the augment")
		public Object[] getPos(Context context, Arguments args) {
			return new Object[] {this.pos.getX(), this.pos.getY(), this.pos.getZ()};
		}
	}

	public class LocoControlAugment extends AugmentManagerBase {
		public LocoControlAugment(World world, BlockPos pos) {
			super(world, pos);
			typeFilter = Locomotive.class;
		}

		@Override
		public String preferredName() {
			return "ir_augment_control";
		}

		@Callback(doc = "function(double) -- sets the locomotive throttle")
		public Object[] setThrottle(Context context, Arguments arguments) throws Exception {
			TileRailBase te = TileRailBase.get(world, pos);
			Locomotive stock = te.getStockNearBy(Locomotive.class, null);
			if (stock != null) {
				stock.setThrottle(normalize(arguments.checkDouble(0)));
			}
			return null;
		}

		private float normalize(double val) {
			if (val > 1) {
				return 1;
			}
			if (val < -1) {
				return -1;
			}
			return (float)val;
		}

		@Callback(doc = "function(double) -- sets the locomotive brake")
		public Object[] setBrake(Context context, Arguments arguments) throws Exception {
			TileRailBase te = TileRailBase.get(world, pos);
			Locomotive stock = te.getStockNearBy(Locomotive.class, null);
			if (stock != null) {
				stock.setAirBrake(normalize(arguments.checkDouble(0)));
			}
			return null;
		}

		@Callback(doc = "function() -- fires the locomotive horn")
		public Object[] horn(Context context, Arguments arguments) throws Exception {
			TileRailBase te = TileRailBase.get(world, pos);
			Locomotive stock = te.getStockNearBy(Locomotive.class, null);
			if (stock != null) {
				stock.setHorn(arguments.optInteger(0, 40), null);
			}
			return null;
		}

		@Callback(doc = "function():string -- returns the current augment type")
		public Object[] getAugmentType(Context context, Arguments args) {
			Augment augment = TileRailBase.get(world, pos).getAugment();
			if (augment != null) {
				return new Object[] { augment.toString() };
			}
			return null;
		}

		@Callback(doc = "function():array -- returns the position of the augment")
		public Object[] getPos(Context context, Arguments args) {
			return new Object[] {this.pos.getX(), this.pos.getY(), this.pos.getZ()};
		}
	}
}
