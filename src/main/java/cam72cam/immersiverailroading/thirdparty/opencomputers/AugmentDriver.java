package cam72cam.immersiverailroading.thirdparty.opencomputers;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.library.Augment;
import cam72cam.immersiverailroading.thirdparty.CommonAPI;
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

import java.util.UUID;

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
		
		@Callback(doc = "function():table -- returns an info dump about the current car")
		public Object[] info(Context context, Arguments arguments) {
			CommonAPI api = CommonAPI.create(world, pos);
			if (api != null) {
				return new Object[] {
						api.info()
				};
			}
			return null;
		}
		
		@Callback(doc = "function():table -- returns an info dump about the current consist")
		public Object[] consist(Context context, Arguments arguments) {
			CommonAPI api = CommonAPI.create(world, pos);
			if (api != null) {
				return new Object[] {
						api.consist(true)
				};
			}
			return null;
		}
		
		@Callback(doc = "function():table -- gets the stock's tag")
		public Object[] getTag(Context context, Arguments arguments) {
			CommonAPI api = CommonAPI.create(world, pos);
			if (api != null) {
				return new Object[] { api.getTag() };
			}
			return null;
		}
		
		@Callback(doc = "function():table -- sets the stock's tag")
		public Object[] setTag(Context context, Arguments arguments) {
			CommonAPI api = CommonAPI.create(world, pos);
			if (api != null) {
				api.setTag(arguments.checkString(0));
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
			CommonAPI api = CommonAPI.create(world, pos, Locomotive.class);
			if (api != null) {
				api.setThrottle(arguments.checkDouble(0));
			}
			return null;
		}

		@Callback(doc = "function(double) -- sets the locomotive brake")
		public Object[] setBrake(Context context, Arguments arguments) throws Exception {
			CommonAPI api = CommonAPI.create(world, pos, Locomotive.class);
			if (api != null) {
				api.setAirBrake(arguments.checkDouble(0));
			}
			return null;
		}

		@Callback(doc = "function() -- fires the locomotive horn")
		public Object[] horn(Context context, Arguments arguments) throws Exception {
			CommonAPI api = CommonAPI.create(world, pos, Locomotive.class);
			if (api != null) {
				api.setHorn(arguments.optInteger(0, 40));
			}
			return null;
		}
		@Callback(doc = "function() -- sets the locomotive bell")
		public Object[] bell(Context context, Arguments arguments) throws Exception {
			CommonAPI api = CommonAPI.create(world, pos, Locomotive.class);
			if (api != null) {
				api.setBell(arguments.optInteger(0, 40));
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
