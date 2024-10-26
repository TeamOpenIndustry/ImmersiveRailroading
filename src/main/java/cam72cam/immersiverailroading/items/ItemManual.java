package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.thirdparty.CompatLoader;
import cam72cam.immersiverailroading.util.IRFuzzy;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.*;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.world.World;

import java.util.Collections;
import java.util.List;

public class ItemManual extends CustomItem {
    public ItemManual() {
        super(ImmersiveRailroading.MODID, "item_manual");

        Fuzzy steel = Fuzzy.STEEL_INGOT;
        IRFuzzy.registerSteelRecipe(this, 3,
                steel, null, steel,
                steel, Fuzzy.BOOK, steel,
                steel, null, steel);
    }

    @Override
    public int getStackSize() {
        return 1;
    }

    @Override
    public List<CreativeTab> getCreativeTabs() {
        return Collections.singletonList(ItemTabs.MAIN_TAB);
    }

    //TODO: rewrite to native wiki
    @Override
    public void onClickAir(Player player, World world, Player.Hand hand) {
        if (world.isClient) {
            if (!CompatLoader.openWiki()) {
                player.sendMessage(PlayerMessage.url("https://github.com/cam72cam/ImmersiveRailroading/wiki"));
            }
        }
    }
}
