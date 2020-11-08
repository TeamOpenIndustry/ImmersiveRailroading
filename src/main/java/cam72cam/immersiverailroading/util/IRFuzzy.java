package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.mod.item.CustomItem;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.Recipes;

public class IRFuzzy {
    public static final Fuzzy IR_CASTING_CASING = Fuzzy.get("irCastingCasing");
    public static final Fuzzy IR_LIGHT_ENG = Fuzzy.get("irLightEngineering");
    public static final Fuzzy IR_HEAVY_ENG = Fuzzy.get("irHeavyEngineering");
    public static final Fuzzy IR_SCAFFOLDING = Fuzzy.get("irScaffolding");
    public static final Fuzzy IR_TIE = Fuzzy.get("irTie");
    public static final Fuzzy IR_RAIL = Fuzzy.get("irRail");
    public static final Fuzzy IR_RAIL_BED = Fuzzy.get("railBed")
            .include(Fuzzy.BRICK_BLOCK)
            .include(Fuzzy.COBBLESTONE)
            .include(Fuzzy.CONCRETE)
            .include(Fuzzy.DIRT)
            .include(Fuzzy.GRAVEL_BLOCK)
            .include(Fuzzy.HARDENED_CLAY)
            .include(Fuzzy.LOG_WOOD)
            .include(Fuzzy.NETHER_BRICK)
            .include(Fuzzy.WOOD_PLANK);

    public static Fuzzy steelIngotOrFallback() {
        return Fuzzy.STEEL_INGOT.isEmpty() ? Fuzzy.IRON_INGOT : Fuzzy.STEEL_INGOT;
    }

    public static Fuzzy steelBlockOrFallback() {
        return Fuzzy.STEEL_BLOCK.isEmpty() ? Fuzzy.IRON_BLOCK : Fuzzy.STEEL_BLOCK;
    }

    public static void registerSteelRecipe(CustomItem item, int width, Fuzzy... ingredients) {
        Fuzzy[] fallback = ingredients.clone();
        for (int i = 0; i < fallback.length; i++) {
            if (fallback[i] == Fuzzy.STEEL_INGOT) {
                fallback[i] = Fuzzy.IRON_INGOT;
            }
        }

        Recipes.shapedRecipe(item, width, ingredients).require(Fuzzy.STEEL_INGOT);
        Recipes.shapedRecipe(item, width, fallback).conflicts(Fuzzy.STEEL_INGOT);
    }

    public static void applyFallbacks() {
        // These will not work with crafting recipes!
        // TODO change over to proper setup
        if (IR_TIE.isEmpty()) {
            IR_TIE.include(Fuzzy.WOOD_PLANK);
        }
        if (IR_RAIL.isEmpty()) {
            IR_RAIL.add(IRItems.ITEM_RAIL);
        }
    }
}
