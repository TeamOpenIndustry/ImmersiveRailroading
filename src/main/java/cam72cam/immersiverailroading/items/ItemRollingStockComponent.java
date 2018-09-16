package cam72cam.immersiverailroading.items;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import javax.annotation.Nullable;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.ItemComponent;
import cam72cam.immersiverailroading.items.nbt.ItemDefinition;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.items.nbt.ItemRawCast;
import cam72cam.immersiverailroading.library.CraftingType;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Optional.Interface(iface = "mezz.jei.api.ingredients.ISlowRenderItem", modid = "jei")
public class ItemRollingStockComponent extends BaseItemRollingStock {
	public static final String NAME = "item_rolling_stock_component";
	
	public ItemRollingStockComponent() {
		super();
		setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
        setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
        this.setCreativeTab(ItemTabs.COMPONENT_TAB);
	}
	
	@Override
	protected void overrideStackDisplayName(ItemStack stack) {
		EntityRollingStockDefinition def = ItemDefinition.get(stack);
		if (def != null) {
			stack.setStackDisplayName(TextFormatting.RESET + def.name() + " " + ItemComponent.getComponentType(stack).toString());
		}
	}
	
	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items)
    {
        if (this.isInCreativeTab(tab))
        {
        	for (String defID : DefinitionManager.getDefinitionNames()) {
        		EntityRollingStockDefinition def = DefinitionManager.getDefinition(defID);
        		for (ItemComponentType item : new LinkedHashSet<ItemComponentType>(def.getItemComponents())) {
	        		ItemStack stack = new ItemStack(this);
	        		ItemDefinition.setID(stack, defID);
	        		ItemComponent.setComponentType(stack, item);
	        		ItemRawCast.set(stack, false);
					overrideStackDisplayName(stack);
	                items.add(stack);	
        		}
        	}
        }
    }
	
	public static boolean requiresHammering(ItemStack stack) {
		return ItemComponent.getComponentType(stack).crafting == CraftingType.CASTING_HAMMER && ItemRawCast.get(stack); 
	}
	
	@Override
	@SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn)
    {
		overrideStackDisplayName(stack);
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(GuiText.GAUGE_TOOLTIP.toString(ItemGauge.get(stack)));
        if (requiresHammering(stack)) {        	
        	tooltip.add(TextFormatting.RED + GuiText.RAW_CAST_TOOLTIP.toString() + TextFormatting.RESET);
        }
    }
	
	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (ItemComponent.getComponentType(player.getHeldItem(hand)) != ItemComponentType.FRAME) {
			return EnumActionResult.FAIL;
		}
		
		List<ItemComponentType> frame = new ArrayList<ItemComponentType>();
		frame.add(ItemComponentType.FRAME);
		return tryPlaceStock(player, worldIn, pos, hand, frame);
	}
}
