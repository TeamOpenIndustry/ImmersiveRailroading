package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemBase;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.item.Recipes;

import java.util.List;

public class ItemRadioCtrlCard extends ItemBase {
    public ItemRadioCtrlCard() {
        super(ImmersiveRailroading.MODID, "item_radio_control_card", 1, ItemTabs.MAIN_TAB);
        Fuzzy transistor = new Fuzzy("oc:materialTransistor");
        Fuzzy dataCard = new Fuzzy("oc:dataCard1");
        Recipes.register(this, 3,
                null, Fuzzy.IRON_BARS, null,
                transistor, Fuzzy.IRON_INGOT, transistor,
                null, dataCard, null
        );
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
