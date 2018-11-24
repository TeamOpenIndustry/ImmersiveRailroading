package cam72cam.immersiverailroading.items;

import java.util.List;

import javax.annotation.Nullable;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.ItemAugmentType;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.library.Augment;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.BlockUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemRailAugment extends Item {
	public static final String NAME = "item_augment";
	
	public ItemRailAugment() {
		super();
		setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
        setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
        this.setCreativeTab(ItemTabs.MAIN_TAB);
        this.setMaxStackSize(16);
	}
	
	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (BlockUtil.isIRRail(world, pos)) {
			TileRailBase te = TileRailBase.get(world, pos);
			if (te != null) {
				ItemStack stack = player.getHeldItem(hand);
				if (te.getAugment() == null && (player.isCreative() || Gauge.from(te.getTrackGauge()) == ItemGauge.get(stack))) {
					Augment augment = ItemAugmentType.get(stack);
					TileRail parent = te.getParentTile();
					if (parent == null) {
						return EnumActionResult.FAIL;
					}
					switch(augment) {
					case WATER_TROUGH:
						return EnumActionResult.FAIL;
						/*
						if (parent.getRotationQuarter() != 0) {
							return EnumActionResult.FAIL;
						}
						if (parent.getType() != TrackItems.STRAIGHT) {
							return EnumActionResult.FAIL; 
						}
						break;
						*/
					case SPEED_RETARDER:
						switch(parent.info.settings.type) {
						case SWITCH:
						case TURN:
							return EnumActionResult.FAIL; 
						default:
							break;
						}
					default:
						break;
					}

					if(!world.isRemote) {
						te.setAugment(augment);
						if (!player.isCreative()) {
							stack.setCount(stack.getCount()-1);;
						}
					}
					return EnumActionResult.SUCCESS;
				}
			}
		}
		return EnumActionResult.PASS;
	}
	
	@Override
	public String getUnlocalizedName(ItemStack stack)
    {
        return super.getUnlocalizedName() + "." + ItemAugmentType.get(stack).name();
    }
	
	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items)
    {
        if (this.isInCreativeTab(tab))
        {
        	for (Augment augment : Augment.values()) {
        		if (augment == Augment.WATER_TROUGH) {
        			continue;
        		}
        		ItemStack stack = new ItemStack(this, 1);
        		ItemAugmentType.set(stack, augment);
                items.add(stack);
        	}
        }
    }
	
	@SideOnly(Side.CLIENT)
	@Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn)
    {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(GuiText.GAUGE_TOOLTIP.toString(ItemGauge.get(stack)));
    }
}
