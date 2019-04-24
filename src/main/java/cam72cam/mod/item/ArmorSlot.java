package cam72cam.mod.item;

import net.minecraft.inventory.EntityEquipmentSlot;

public enum ArmorSlot {
    MAINHAND(EntityEquipmentSlot.MAINHAND),
    OFFHAND(EntityEquipmentSlot.OFFHAND),
    FEET(EntityEquipmentSlot.FEET),
    LEGS(EntityEquipmentSlot.LEGS),
    CHEST(EntityEquipmentSlot.CHEST),
    HEAD(EntityEquipmentSlot.HEAD);
    public final EntityEquipmentSlot internal;

    ArmorSlot(EntityEquipmentSlot slot) {
        this.internal = slot;
    }

    public static ArmorSlot from(EntityEquipmentSlot armorType) {
        switch (armorType) {
            case MAINHAND:
                return MAINHAND;
            case OFFHAND:
                return OFFHAND;
            case FEET:
                return FEET;
            case LEGS:
                return LEGS;
            case CHEST:
                return CHEST;
            case HEAD:
                return HEAD;
            default:
                return null;
        }
    }
}
