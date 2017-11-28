package cam72cam.immersiverailroading.items;

import java.util.List;

import javax.annotation.Nullable;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Augment;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.GuiText;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
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
