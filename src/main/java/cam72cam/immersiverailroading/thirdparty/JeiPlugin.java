package cam72cam.immersiverailroading.thirdparty;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.items.ItemRollingStock;
import cam72cam.immersiverailroading.items.ItemTabs;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.IModIngredientRegistration;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import mezz.jei.ingredients.Ingredients;
import net.minecraft.client.Minecraft;import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;

@JEIPlugin
public class JeiPlugin implements IModPlugin{

	@Override
	public void registerItemSubtypes(ISubtypeRegistry subtypeRegistry) {

	}

	@Override
	public void registerIngredients(IModIngredientRegistration registry) {

	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registry) {

	}

	@Override
	public void register(IModRegistry registry) {
		
		NonNullList<ItemStack> stock = NonNullList.create();

        IRItems.ITEM_ROLLING_STOCK.getSubItems(ItemTabs.LOCOMOTIVE_TAB, stock);
        IRItems.ITEM_ROLLING_STOCK.getSubItems(ItemTabs.PASSENGER_TAB, stock);
        IRItems.ITEM_ROLLING_STOCK.getSubItems(ItemTabs.STOCK_TAB, stock);
        
		registry.addIngredientInfo(stock, ItemStack.class, "To make a rolling stock you need to place it's different components on a track. "
				+ "Right-click with the wrench on the components to see which parts are missing. "
				+ "For more details look at our Wiki.");
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {

	}

}
