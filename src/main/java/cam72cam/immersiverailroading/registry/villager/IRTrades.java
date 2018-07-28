package cam72cam.immersiverailroading.registry.villager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import cam72cam.immersiverailroading.items.ItemRollingStock;
import cam72cam.immersiverailroading.items.ItemTabs;
import cam72cam.immersiverailroading.items.nbt.ItemDefinition;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.registry.CarFreightDefinition;
import cam72cam.immersiverailroading.registry.CarPassengerDefinition;
import cam72cam.immersiverailroading.registry.CarTankDefinition;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveDefinition;
import cam72cam.immersiverailroading.registry.TenderDefinition;
import it.unimi.dsi.fastutil.Stack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.registry.RegistryNamespaced;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import trackapi.lib.Gauges;

public class IRTrades implements EntityVillager.ITradeList {
	private static Collection<EntityRollingStockDefinition> locoDefs;
	private static Collection<EntityRollingStockDefinition> stockDefs;
	
	@Override
	public void addMerchantRecipe(IMerchant merchant, MerchantRecipeList recipeList, Random random) {
		ItemRollingStock stockItem = new ItemRollingStock();
		NonNullList<ItemStack> stockList = NonNullList.create();
		//WARNING! DO NOT UNCOMMENT THIS ItemStack stack = new ItemStack(stockItem);
		//OR THIS ItemDefinition.setID(stack, "rolling_stock/locomotives/f59phi.json");
		//OR THIS Minecraft.getMinecraft().player.inventory.addItemStackToInventory(stack);
		
		
		//Adding the ItemStack to an inventory or MerchantRecipe makes the ItemRollingStock into an air item
		stockItem.getSubItems(ItemTabs.LOCOMOTIVE_TAB, stockList);
		System.out.println("List: " + stockList.get(0)); //immersiverailroading:item_rolling_stock  Quantity: 1
		
		Minecraft.getMinecraft().player.inventory.addItemStackToInventory(stockList.get(0)); //THIS LINE AFFECTS THE ITEM
		
		System.out.println("List: " + stockList.get(0)); //minecraft:tile_air  Quantity: 0
		
		
		/*
		for(ItemStack stack : stockList) {
			recipeList.add(new MerchantRecipe(new ItemStack(Items.EMERALD, 1), stack));
			Minecraft.getMinecraft().player.inventory.addItemStackToInventory(stack);
		}
		*/
	}
}
