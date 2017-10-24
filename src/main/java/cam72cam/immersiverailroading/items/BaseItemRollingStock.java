package cam72cam.immersiverailroading.items;

import java.util.List;

import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.util.SpawnUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public abstract class BaseItemRollingStock extends Item {
	
	protected void overrideStackDisplayName(ItemStack stack) {
		EntityRollingStockDefinition def = getDefinition(stack);
		if (def != null) {
			stack.setStackDisplayName(TextFormatting.RESET + def.name);
		}
	}
	
	@Override
	public String getUnlocalizedName(ItemStack stack) {
		overrideStackDisplayName(stack);
		return super.getUnlocalizedName(stack);
	}
	
	public static void setDefinitionID(ItemStack stack, String def) {
		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setString("defID", def);
	}
	
	public static String getDefinitionID(ItemStack stack) {
		if (stack.getTagCompound() != null){
			return stack.getTagCompound().getString("defID");
		}
		stack.setCount(0);
		return "BUG";
	}
	
	public static EntityRollingStockDefinition getDefinition(ItemStack stack) {
		return DefinitionManager.getDefinition(getDefinitionID(stack));
	}
	
	public static EnumActionResult tryPlaceStock(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, List<ItemComponentType> parts) {
		ItemStack stack = player.getHeldItem(hand);
		
		EntityRollingStockDefinition def = getDefinition(stack);
		if (def == null) {
			player.sendMessage(new TextComponentString("Error: Rolling stock does not exist on the server.  Can not place"));
			return EnumActionResult.FAIL;
		}
		
		if (parts == null) {
			parts = def.getItemComponents();
		}
		
		return SpawnUtil.placeStock(player, hand, worldIn, pos, def, parts);
	}
}
