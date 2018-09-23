package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistryModifiable;

public class RecipeUtil {
	public static void removeRecipe (IForgeRegistryModifiable<IRecipe> modRegistry, ResourceLocation recipe) {
		modRegistry.remove(recipe);
		ImmersiveRailroading.info("[%s] Removing Recipie: %s",ImmersiveRailroading.MODID, modRegistry.getValue(recipe));
	}
}
