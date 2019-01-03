package cam72cam.immersiverailroading.thirdparty.opencomputers;

import java.util.*;
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
					if (train.getPersistentID()
							.equals(UUID.fromString(stack.getTagCompound().getString("linked_uuid")))) {
						return new RadioCtrlCardManager(train);
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
		protected Locomotive linkedLoco;

		public RadioCtrlCardManager(Locomotive loco) {
			linkedLoco = loco;
			setNode(Network.newNode(this, Visibility.Network).withComponent("ir_remote_control", Visibility.Network)
					.create());
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
			if (linkedLoco != null) {
				linkedLoco.setThrottle(normalize(arguments.checkDouble(0)));
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
			return (float) val;
		}

		@Callback(doc = "function(double) -- sets the locomotive brake")
		public Object[] setBrake(Context context, Arguments arguments) throws Exception {
			if (linkedLoco != null) {
				linkedLoco.setAirBrake(normalize(arguments.checkDouble(0)));
			}
			return null;
		}

		@Callback(doc = "function() -- fires the locomotive horn")
		public Object[] horn(Context context, Arguments arguments) throws Exception {
			if (linkedLoco != null) {
				linkedLoco.setHorn(arguments.optInteger(0, 40), null);
			}
			return null;
		}

		@Callback(doc = "function():array -- returns the XYZ position of the locomotive")
		public Object[] getPos(Context context, Arguments args) {
			if (linkedLoco != null) {
				return new Object[] { linkedLoco.posX, linkedLoco.posY, linkedLoco.posZ };
			}
			return null;
		}

		@Callback(doc = "function():araray -- returns the UUID of the bound loco")
		public Object[] isWork(Context context, Arguments args) {
			if (linkedLoco == null)
				return new Object[] { null };

			return new Object[] { linkedLoco.getUniqueID() };
		}
	}
}
