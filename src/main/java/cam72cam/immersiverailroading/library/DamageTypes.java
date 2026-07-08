package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.entity.DamageType;
import cam72cam.mod.resource.Identifier;

public class DamageTypes {
    public static final DamageType HIT = DamageType.getOrCreate(new Identifier(ImmersiveRailroading.MODID,"hit_by_train"));
    public static final DamageType HIT_IN_DARKNESS = DamageType.getOrCreate(new Identifier(ImmersiveRailroading.MODID,"hit_by_train_in_darkness"));
    public static final DamageType CASTING = DamageType.getOrCreate(new Identifier(ImmersiveRailroading.MODID, "casting"));

    public static void register() {
        // loads static classes and ctrs
    }
}
