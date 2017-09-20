package cam72cam.immersiverailroading.items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Nullable;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.util.SpawnUtil;
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

public class ItemRollingStockComponent extends Item {
	public static final String NAME = "item_rolling_stock_component";
	
	public ItemRollingStockComponent() {
		super();
		setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
        setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
        this.setCreativeTab(CreativeTabs.TRANSPORTATION);
	}
	
	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        String name = "Unknown";
        EntityRollingStockDefinition def = DefinitionManager.getDefinition(defFromStack(stack));
        if (def != null) {
        	name = def.name;
        }
        tooltip.add("Stock: " + name);
        tooltip.add("Part:  " + typeFromStack(stack).prettyString());
	}
	
	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items)
    {
        if (this.isInCreativeTab(tab))
        {
        	for (String defID : DefinitionManager.getDefinitionNames()) {
        		EntityRollingStockDefinition def = DefinitionManager.getDefinition(defID);
        		for (ItemComponentType item : new HashSet<ItemComponentType>(def.getItemComponents())) {
	        		ItemStack stack = new ItemStack(this);
					stack.setTagCompound(nbtFromDef(defID, item));
	                items.add(stack);	
        		}
        	}
        }
    }
	
	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		ItemStack stack = player.getHeldItem(hand);
		
		if (typeFromStack(stack) != ItemComponentType.FRAME) {
			return EnumActionResult.FAIL;
		}
		
		EntityRollingStockDefinition def = DefinitionManager.getDefinition(defFromStack(stack));
		
		List<ItemComponentType> frame = new ArrayList<ItemComponentType>();
		frame.add(ItemComponentType.FRAME);
		return SpawnUtil.placeStock(player, hand, worldIn, pos, def, frame);
	}
	
	public static String defFromStack(ItemStack stack) {
		if (stack.getTagCompound() != null){
			return stack.getTagCompound().getString("defID");
		}
		stack.setCount(0);
		return "BUG";
	}
	public static ItemComponentType typeFromStack(ItemStack stack) {
		if (stack.getTagCompound() != null){
			return ItemComponentType.values()[stack.getTagCompound().getInteger("componentType")];
		}
		stack.setCount(0);
		return ItemComponentType.values()[0];
	}
	public static NBTTagCompound nbtFromDef(String defID, ItemComponentType item) {
		NBTTagCompound val = new NBTTagCompound();
		val.setString("defID", defID);
		val.setInteger("componentType", item.ordinal());
		return val;
	}
}
