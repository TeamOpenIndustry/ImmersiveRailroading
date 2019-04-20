package cam72cam.immersiverailroading.thirdparty.opencomputers;

import java.util.*;
import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.entity.Locomotive;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;

public class RadioCtrlCardDriver implements DriverItem {

	@Override
	public boolean worksWith(ItemStack stack) {
		if (stack != null && stack.getItem() == IRItems.ITEM_RADIO_CONTROL_CARD) {
			return true;
		}
		return false;
	}

	@Override
	public ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
		World hostWorld = host.world();
		if (stack != null && stack.getItem() == IRItems.ITEM_RADIO_CONTROL_CARD) {
			for (Object e : hostWorld.loadedEntityList) {
				if (e instanceof Locomotive) {
					Locomotive train = (Locomotive) e;
					if (train.getPersistentID().equals(UUID.fromString(stack.getTagCompound().getString("linked_uuid")))) {
						return new RadioCtrlCardManager(train, host.xPosition(), host.yPosition(), host.zPosition());
					}
				}
			}
		}
		return null;
	}

	@Override
	public String slot(ItemStack stack) {
		return Slot.Card;
	}

	@Override
	public int tier(ItemStack stack) {
		// Should we bump the tier up? This component is quite powerful
		return 0;
	}

	@Override
	public NBTTagCompound dataTag(ItemStack stack) {
		return null;
	}

	public class RadioCtrlCardManager extends AbstractManagedEnvironment {
		protected double[] cardPosition = {0,0,0};
		protected Locomotive linkedLoco;
		protected ComponentConnector node;

		public RadioCtrlCardManager(Locomotive loco, double x, double y, double z) {
			cardPosition[0] = x;
			cardPosition[1] = y;
			cardPosition[2] = z;
			linkedLoco = loco;
			node = Network.newNode(this, Visibility.Network).withComponent("ir_remote_control", Visibility.Network).withConnector().create();
			setNode(node);
		}

		@Override
		public boolean canUpdate() {
			return false;
		}

		@Override
		public void update() {
			// Node node = this.node();
		}

		@Callback(doc = "function(double) -- sets the locomotive throttle")
		public Object[] setThrottle(Context context, Arguments arguments) throws Exception {
			if (radioDrain()) {
				linkedLoco.setThrottle(normalize(arguments.checkDouble(0)));
			}
			return null;
		}

		private float normalize(double val) {
			if (Double.isNaN(val)) {
				return 0;
			}
			if (val > 1) {
				return 1;
			}
			if (val < -1) {
				return -1;
			}
			return (float) val;
		}
		
		private boolean radioDrain() 
		{
			if(linkedLoco == null) {
				return false;
			}
			double distance = 0;
			double[] currentLocoPosition  = {linkedLoco.posX, linkedLoco.posY, linkedLoco.posZ};
			double[] relativeDistanceVector = {0,0,0};
			for(int i = 0; i <= 2; i++) {
				relativeDistanceVector[i] = currentLocoPosition[i] - cardPosition[i];
				distance += (relativeDistanceVector[i] * relativeDistanceVector[i]);
			}
			distance = Math.sqrt( distance );
			if( distance > Config.ConfigBalance.RadioRange) {
				return false;
			}
			if(node.tryChangeBuffer(-Config.ConfigBalance.RadioCostPerMetre * distance)) {
				return true;
			}
			return false;
		}

		@Callback(doc = "function(double) -- sets the locomotive brake")
		public Object[] setBrake(Context context, Arguments arguments) throws Exception {
			if (radioDrain()) {
				linkedLoco.setAirBrake(normalize(arguments.checkDouble(0)));
			}
			return null;
		}

		@Callback(doc = "function() -- fires the locomotive horn")
		public Object[] horn(Context context, Arguments arguments) throws Exception {
			if (radioDrain()) {
				linkedLoco.setHorn(arguments.optInteger(0, 40), null);
			}
			return null;
		}
		@Callback(doc = "function() -- sets the locomotive bell")
		public Object[] bell(Context context, Arguments arguments) throws Exception {
			if (radioDrain()) {
				linkedLoco.setBell(arguments.optInteger(0, 40));
			}
			return null;
		}

		@Callback(doc = "function():array -- returns the XYZ position of the locomotive")
		public Object[] getPos(Context context, Arguments args) {
			if (radioDrain()) {
				return new Object[] { linkedLoco.posX, linkedLoco.posY, linkedLoco.posZ };
			}
			return null;
		}

		@Callback(doc = "function():araray -- returns the UUID of the bound loco")
		public Object[] getLinkUUID(Context context, Arguments args) {
			if (radioDrain()) {
				return new Object[] { linkedLoco.getUniqueID() };
			}
			return new Object[] { null };
		}
	}
}
