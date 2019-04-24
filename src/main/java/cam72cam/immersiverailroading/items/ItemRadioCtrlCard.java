package cam72cam.immersiverailroading.items;

import java.util.List;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.ItemStack;

public class ItemRadioCtrlCard extends ItemBase {
    public ItemRadioCtrlCard() {
        super(ImmersiveRailroading.MODID, "item_radio_control_card", 1, ItemTabs.MAIN_TAB);
    }

    @Override
    public void addInformation(ItemStack stack, List<String> tooltip) {
        if (!stack.getTagCompound().hasKey("linked_uuid")) {
            tooltip.add("Not linked to any locomotive");
        } else {
            tooltip.add("Linked to: " + stack.getTagCompound().getString("linked_uuid"));
        }
    }
}
