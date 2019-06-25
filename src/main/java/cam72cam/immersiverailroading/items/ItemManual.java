package cam72cam.immersiverailroading.items;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.ItemMultiblockType;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.world.World;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Rotation;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;

public class ItemManual extends ItemBase {
	public ItemManual() {
		super(ImmersiveRailroading.MODID, "item_manual", 1, ItemTabs.MAIN_TAB);
	}
	
	@Override
	public void addInformation(ItemStack stack, List<String> tooltip) {
		tooltip.add(GuiText.SELECTOR_TYPE.toString(ItemMultiblockType.get(stack)));
	}

	@Override
	public void onClickAir(Player player, World world, Hand hand) {
		if (player.isCrouching()) {
			if (world.isServer) {
				ItemStack item = player.getHeldItem(hand);
				String current = ItemMultiblockType.get(item);
				List<String> keys = MultiblockRegistry.keys();
				current = keys.get((keys.indexOf(current) + 1) % (keys.size()));
				ItemMultiblockType.set(item, current);
				player.sendMessage(PlayerMessage.direct("Placing: " + current));
			}
		} else {
			if (world.isClient) {
				if (Loader.isModLoaded("igwmod")) {
					// This is lousy code...
					try {
						Class<?> cls = Class.forName("igwmod.gui.GuiWiki");
						Object wiki = cls.newInstance();
						FMLCommonHandler.instance().showGuiScreen(wiki);
						Method scf = cls.getMethod("setCurrentFile", String.class, Object[].class);
						scf.invoke(wiki, "immersiverailroading:home", new Object[] {});
					} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException e) {
						e.printStackTrace();
					}
				} else {
					player.sendMessage(PlayerMessage.url("https://github.com/cam72cam/ImmersiveRailroading/wiki"));
				}
			}
		}
	}
	
	@Override
	public ClickResult onClickBlock(Player player, World world, Vec3i pos, Hand hand, Facing facing, Vec3d hit) {
		if (world.isServer) {
			ItemStack item = player.getHeldItem(hand);
			String current = ItemMultiblockType.get(item);
			Vec3i realPos = pos;
			if (facing == Facing.DOWN) {
				realPos = realPos.down();
			}
			if (facing == Facing.UP) {
				realPos = realPos.up();
			}
			MultiblockRegistry.get(current).place(world.internal, player, realPos.internal, Rotation.from(Facing.fromAngle(player.getYawHead()+180)).internal);
		}
		return ClickResult.ACCEPTED;
	}
}
