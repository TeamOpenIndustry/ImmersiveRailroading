package cam72cam.immersiverailroading.items;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.Nullable;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.ItemMultiblockType;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import cam72cam.immersiverailroading.util.BlockUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemManual extends Item {
	public static final String NAME = "item_manual";
	
	public ItemManual() {
		super();
		
		setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
		setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
        this.setCreativeTab(ItemTabs.MAIN_TAB);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn)
    {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(GuiText.SELECTOR_TYPE.toString(ItemMultiblockType.get(stack)));
    }
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		if (player.isSneaking()) {
			if (!world.isRemote) {
				ItemStack item = player.getHeldItem(hand);
				String current = ItemMultiblockType.get(item);
				List<String> keys = MultiblockRegistry.keys();
				current = keys.get((keys.indexOf(current) + 1) % (keys.size()));
				ItemMultiblockType.set(item, current);
				player.sendMessage(new TextComponentString("Placing: " + current));
			}
		} else {
			if (world.isRemote) {
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
					player.sendMessage(ForgeHooks.newChatWithLinks("https://github.com/cam72cam/ImmersiveRailroading/wiki"));
				}
			}
		}
		return super.onItemRightClick(world, player, hand);
	}
	
	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (!world.isRemote) {
			ItemStack item = player.getHeldItem(hand);
			String current = ItemMultiblockType.get(item);
			BlockPos realPos = pos;
			if (facing == EnumFacing.DOWN) {
				realPos = realPos.down();
			}
			if (facing == EnumFacing.UP) {
				realPos = realPos.up();
			}
			MultiblockRegistry.get(current).place(world, player, realPos, BlockUtil.rotFromFacing(EnumFacing.fromAngle(player.rotationYawHead+180)));
		}
		return EnumActionResult.SUCCESS;
	}
}
