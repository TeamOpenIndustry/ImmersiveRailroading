package cam72cam.immersiverailroading.items;

import java.util.List;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.ItemMultiblockType;
import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import cam72cam.immersiverailroading.util.BlockUtil;
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

public class ItemMultiblockPlacer extends Item {
	public static final String NAME = "item_mb_placer";
	
	public ItemMultiblockPlacer() {
		super();
		
		setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
		setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
        this.setCreativeTab(ItemTabs.MAIN_TAB);
	}
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		if (!world.isRemote && player.isSneaking()) {
			ItemStack item = player.getHeldItem(hand);
			String current = ItemMultiblockType.get(item);
			List<String> keys = MultiblockRegistry.keys();
			current = keys.get((keys.indexOf(current) + 1) % (keys.size()));
			ItemMultiblockType.set(item, current);
			player.sendMessage(new TextComponentString("MB: " + current));
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
		return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
	}
}
