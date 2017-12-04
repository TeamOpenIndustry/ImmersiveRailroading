package cam72cam.immersiverailroading.items;

import java.util.List;

import javax.annotation.Nullable;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Augment;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.BlockUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
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
        this.setMaxStackSize(1);
	}
	
	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if(!world.isRemote) {
			if (BlockUtil.isIRRail(world, pos)) {
				TileRailBase te = TileRailBase.get(world, pos);
				if (te != null) {
					ItemStack stack = player.getHeldItem(hand);
					if (te.getAugment() == null && (player.isCreative() || Gauge.from(te.getTrackGauge()) == getGauge(stack))) {
						Augment augment = getAugment(stack);
						switch(augment) {
						case SPEED_RETARDER:
						case WATER_TROUGH:
							TileRail parent = te.getParentTile();
							if (parent == null) {
								return EnumActionResult.FAIL;
							}
							if (parent.getRotationQuarter() != 0) {
								return EnumActionResult.FAIL;
							}
							if (parent.getType() != TrackItems.STRAIGHT) {
								return EnumActionResult.FAIL; 
							}
							break;
						default:
							break;
						}
						te.setAugment(augment);
						if (!player.isCreative()) {
							stack.setCount(0);
						}
						return EnumActionResult.SUCCESS;
					}
				}
			}
		}
		return EnumActionResult.PASS;
	}
	
	@Override
	public String getUnlocalizedName(ItemStack stack)
    {
        return super.getUnlocalizedName() + "." + getAugment(stack).name();
    }
	
	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items)
    {
        if (this.isInCreativeTab(tab))
        {
        	for (Augment augment : Augment.values()) {
        		ItemStack stack = new ItemStack(this);
        		setAugment(stack, augment);
                items.add(stack);
        	}
        }
    }
	
	@SideOnly(Side.CLIENT)
	@Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn)
    {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(GuiText.STOCK_GAUGE.toString(getGauge(stack)));
    }

	public static void setAugment(ItemStack stack, Augment augment) {
		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setInteger("augment", augment.ordinal());
	}
	
	public static Augment getAugment(ItemStack stack) {
		if (stack.getTagCompound() != null){
			return Augment.values()[stack.getTagCompound().getInteger("augment")];
		}
		return Augment.WATER_TROUGH;
	}
	
	public static void setGauge(ItemStack stack, Gauge gauge) {
		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setDouble("gauge", gauge.value());
	}
	
	public static Gauge getGauge(ItemStack stack) {
		if (stack.getTagCompound() != null && stack.getTagCompound().hasKey("gauge")){
			return Gauge.from(stack.getTagCompound().getDouble("gauge"));
		}
		return Gauge.STANDARD;
	}
	
}
