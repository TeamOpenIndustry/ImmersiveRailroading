package cam72cam.immersiverailroading.items;

import java.util.HashSet;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.ItemAugmentType;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.library.Augment;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
//TODO buildcraft.api.tools.IToolWrench
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemLargeWrench extends ItemTool {
	public static final String NAME = "item_large_wrench";
	
	public ItemLargeWrench() {
		super(8, -3.2F, ToolMaterial.IRON, new HashSet<Block>());
		setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
		setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
        this.setCreativeTab(ItemTabs.MAIN_TAB);
	}
	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (BlockUtil.isIRRail(world, pos)) {
			TileRailBase te = TileRailBase.get(world, pos);
			if (te != null) {
				Augment augment = te.getAugment();
				if (augment != null) {
					te.setAugment(null);

					if(!world.isRemote) {
						ItemStack stack = new ItemStack(IRItems.ITEM_AUGMENT, 1);
						ItemAugmentType.set(stack, augment);
						ItemGauge.set(stack, Gauge.from(te.getTrackGauge()));
						world.spawnEntity(new EntityItem(world, pos.getX(), pos.getY(), pos.getZ(), stack));
					}
					return EnumActionResult.SUCCESS;
				}
				TileRail parent = te.getParentTile();
				if (!world.isRemote) {
					if (parent != null && parent.info.settings.type == TrackItems.TURNTABLE) {
						parent.nextTablePos(player.isSneaking());
					}
				}
			}
		} else {
			for (String key : MultiblockRegistry.keys()) {
				if (MultiblockRegistry.get(key).tryCreate(world, pos)) {
					return EnumActionResult.SUCCESS;
				}
			}
		}
		return EnumActionResult.PASS;
	}
}
