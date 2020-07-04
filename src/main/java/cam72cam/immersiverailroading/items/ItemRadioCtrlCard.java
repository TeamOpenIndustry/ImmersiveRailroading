package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.item.*;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.util.CollectionUtil;

import java.util.List;
import java.util.UUID;

public class ItemRadioCtrlCard extends ItemBase {
    public ItemRadioCtrlCard() {
        super(ImmersiveRailroading.MODID, "item_radio_control_card");
        Fuzzy transistor = new Fuzzy("oc:materialTransistor");
        Fuzzy dataCard = new Fuzzy("oc:dataCard1");
        Recipes.register(this, 3,
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
        return CollectionUtil.listOf(ItemTabs.MAIN_TAB);
    }


    @Override
    public List<String> getTooltip(ItemStack stack) {
        Data d = new Data(stack);
        return CollectionUtil.listOf(d.linked == null ? "Not linked to any locomotive" : "Linked to: " + d.linked);
    }

    public static class Data extends ItemData {
        @TagField("linked")
        public UUID linked;

        public Data(ItemStack stack) {
            super(stack);
        }
    }
}
