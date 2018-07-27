package cam72cam.immersiverailroading.registry.villager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import cam72cam.immersiverailroading.items.ItemRollingStock;
import cam72cam.immersiverailroading.items.ItemTabs;
import cam72cam.immersiverailroading.items.nbt.ItemDefinition;
import cam72cam.immersiverailroading.registry.CarFreightDefinition;
import cam72cam.immersiverailroading.registry.CarPassengerDefinition;
import cam72cam.immersiverailroading.registry.CarTankDefinition;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveDefinition;
import cam72cam.immersiverailroading.registry.TenderDefinition;
import it.unimi.dsi.fastutil.Stack;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;

public class IRTrades implements EntityVillager.ITradeList {
	private static Collection<EntityRollingStockDefinition> locoDefs;
	private static Collection<EntityRollingStockDefinition> stockDefs;
	
	@Override
	public void addMerchantRecipe(IMerchant merchant, MerchantRecipeList recipeList, Random random) {
		ItemRollingStock stockItem = new ItemRollingStock();
		NonNullList<ItemStack> stockList = NonNullList.create();
		stockItem.getSubItems(ItemTabs.LOCOMOTIVE_TAB, stockList);
		for(ItemStack stack : stockList) {
			recipeList.add(new MerchantRecipe(new ItemStack(Items.EMERALD, 1), stack));
		}
	}
}
