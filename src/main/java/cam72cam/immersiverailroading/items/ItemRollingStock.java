package cam72cam.immersiverailroading.items;

import java.util.List;

import javax.annotation.Nullable;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.BlockUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Optional.Interface(iface = "mezz.jei.api.ingredients.ISlowRenderItem", modid = "jei")
public class ItemRollingStock extends BaseItemRollingStock {
	public static final String NAME = "item_rolling_stock";
	
	public ItemRollingStock() {
		super();
		setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
        setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
        this.setCreativeTab(ItemTabs.STOCK_TAB);
        this.setMaxStackSize(1);
	}
	
	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items)
    {
        if (this.isInCreativeTab(tab))
        {
        	for (String defID : DefinitionManager.getDefinitionNames()) {
        		ItemStack stack = new ItemStack(this);
				setDefinitionID(stack, defID);
				overrideStackDisplayName(stack);
                items.add(stack);
        	}
        }
    }
	
	@SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn)
    {
		overrideStackDisplayName(stack);
		
		Gauge gauge = getGauge(stack);
		
        super.addInformation(stack, worldIn, tooltip, flagIn);
        EntityRollingStockDefinition def = getDefinition(stack);
        if (def != null) {
        	tooltip.addAll(def.getTooltip(gauge));
        }
        tooltip.add(GuiText.STOCK_GAUGE.toString(gauge));
    }
	
	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (BlockUtil.isIRRail(worldIn, pos)) {
			TileRailBase te = TileRailBase.get(worldIn, pos);
			if (te.getAugment() != null) {
				switch(te.getAugment()) {
				case DETECTOR:
				case FLUID_LOADER:
				case FLUID_UNLOADER:
				case ITEM_LOADER:
				case ITEM_UNLOADER:
					if (!worldIn.isRemote) {
						boolean set = te.setAugmentFilter(getDefinitionID(player.getHeldItem(hand)));
						if (set) {
							player.sendMessage(ChatText.SET_AUGMENT_FILTER.getMessage(getDefinition(player.getHeldItem(hand)).name));
						} else {
							player.sendMessage(ChatText.RESET_AUGMENT_FILTER.getMessage());
						}
					}
					return EnumActionResult.SUCCESS;
				default:
					break;
				}
			}
		}
		return tryPlaceStock(player, worldIn, pos, hand, null);
	}
	
	@Override
	public boolean isValidArmor(ItemStack stack, EntityEquipmentSlot armorType, Entity entity) {
		return armorType == EntityEquipmentSlot.HEAD && Config.trainsOnTheBrain;
	}
}
