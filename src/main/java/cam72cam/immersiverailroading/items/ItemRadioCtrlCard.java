package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.item.*;
import cam72cam.mod.serialization.TagField;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ItemRadioCtrlCard extends CustomItem {
    public ItemRadioCtrlCard() {
        super(ImmersiveRailroading.MODID, "item_radio_control_card");
        Fuzzy transistor = Fuzzy.get("oc:materialTransistor");
        Fuzzy dataCard = Fuzzy.get("oc:dataCard1");
        Recipes.shapedRecipe(this, 3,
                null, Fuzzy.IRON_BARS, null,
                transistor, Fuzzy.IRON_INGOT, transistor,
                null, dataCard, null
        );
    }

    @Override
    public int getStackSize() {
        return 1;
    }

    @Override
    public List<CreativeTab> getCreativeTabs() {
        return Collections.singletonList(ItemTabs.MAIN_TAB);
    }


    @Override
    public List<String> getTooltip(ItemStack stack) {
        Data d = new Data(stack);
        return Collections.singletonList(d.linked == null ? "Not linked to any locomotive" : "Linked to: " + d.linked);
    }

    public static class Data extends ItemDataSerializer {
        @TagField("linked")
        public UUID linked;

        public Data(ItemStack stack) {
            super(stack);
        }
    }
}
