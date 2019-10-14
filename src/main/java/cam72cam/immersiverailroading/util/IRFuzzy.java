package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.mod.item.Fuzzy;

public class IRFuzzy {
    public static final Fuzzy IR_CASTING_CASING = new Fuzzy("irCastingCasing");
    public static final Fuzzy IR_LIGHT_ENG = new Fuzzy("irLightEngineering");
    public static final Fuzzy IR_HEAVY_ENG = new Fuzzy("irHeavyEngineering");
    public static final Fuzzy IR_SCAFFOLDING = new Fuzzy("irScaffolding");
    public static final Fuzzy IR_STEEL_BLOCK = new Fuzzy("blockSteel");
    public static final Fuzzy IR_STEEL_INGOT = new Fuzzy("ingotSteel");
    public static final Fuzzy IR_TIE = new Fuzzy("irTie");
    public static final Fuzzy IR_RAIL = new Fuzzy("irRail");
    public static final Fuzzy IR_RAIL_BED = new Fuzzy("railBed")
            .addAll(Fuzzy.BRICK_BLOCK)
            .addAll(Fuzzy.COBBLESTONE)
            .addAll(Fuzzy.CONCRETE)
            .addAll(Fuzzy.DIRT)
            .addAll(Fuzzy.GRAVEL_BLOCK)
            .addAll(Fuzzy.HARDENED_CLAY)
            .addAll(Fuzzy.LOG_WOOD)
            .addAll(Fuzzy.NETHER_BRICK)
            .addAll(Fuzzy.WOOD_PLANK);

    public static void applyFallbacks() {
        if (IR_CASTING_CASING.example().isEmpty()) {
            IR_CASTING_CASING.addAll(Fuzzy.NETHER_BRICK);
        }
        if (IR_LIGHT_ENG.example().isEmpty()) {
            IR_LIGHT_ENG.addAll(Fuzzy.IRON_BLOCK);
        }
        if (IR_HEAVY_ENG.example().isEmpty()) {
            IR_HEAVY_ENG.addAll(Fuzzy.IRON_BLOCK);
        }
        if (IR_SCAFFOLDING.example().isEmpty()) {
            IR_SCAFFOLDING.addAll(Fuzzy.IRON_BARS);
        }
        if (IR_STEEL_INGOT.example().isEmpty()) {
            IR_STEEL_INGOT.addAll(Fuzzy.IRON_INGOT);
        }
        if (IR_STEEL_BLOCK.example().isEmpty()) {
            IR_STEEL_BLOCK.addAll(Fuzzy.IRON_BLOCK);
        }
        if (IR_TIE.example().isEmpty()) {
            IR_TIE.addAll(Fuzzy.WOOD_PLANK);
        }
        if (IR_RAIL.example().isEmpty()) {
            IR_RAIL.add(IRItems.ITEM_RAIL);
        }
        if (IR_RAIL_BED.example().isEmpty()) {
            IR_RAIL_BED.addAll(Fuzzy.GRAVEL_BLOCK);
        }
    }
}
