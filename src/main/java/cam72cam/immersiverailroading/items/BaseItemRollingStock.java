package cam72cam.immersiverailroading.items;

import java.util.List;

import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.items.nbt.ItemDefinition;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.util.SpawnUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public abstract class BaseItemRollingStock extends Item {
	
	protected void overrideStackDisplayName(ItemStack stack) {
		EntityRollingStockDefinition def = ItemDefinition.get(stack);
		if (def != null) {
			stack.setStackDisplayName(TextFormatting.RESET + def.name());
		}
	}
	
	@Override
	public String getUnlocalizedName(ItemStack stack) {
		overrideStackDisplayName(stack);
		return super.getUnlocalizedName(stack);
	}
	
	public static EnumActionResult tryPlaceStock(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, List<ItemComponentType> parts) {
		ItemStack stack = player.getHeldItem(hand);
		
		EntityRollingStockDefinition def = ItemDefinition.get(stack);
		if (def == null) {
			player.sendMessage(ChatText.STOCK_INVALID.getMessage());
			return EnumActionResult.FAIL;
		}
		
		if (parts == null) {
			parts = def.getItemComponents();
		}
		
		return SpawnUtil.placeStock(player, hand, worldIn, pos, def, parts);
	}
}
