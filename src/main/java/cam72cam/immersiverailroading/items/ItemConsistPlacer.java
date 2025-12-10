package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.net.ConsistPlacePacket;
import cam72cam.immersiverailroading.registry.ConsistDefinition;
import cam72cam.immersiverailroading.registry.ConsistDefinitionManager;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.CustomItem;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;

import java.util.*;
import java.util.List;

public class ItemConsistPlacer extends CustomItem {
    public ItemConsistPlacer() {
        super(ImmersiveRailroading.MODID, "item_consist_placer");
    }

    @Override
    public List<CreativeTab> getCreativeTabs() {
        return Collections.singletonList(ItemTabs.MAIN_TAB);
    }

    @Override
    public ClickResult onClickBlock(Player player, World world, Vec3i pos, Player.Hand hand, Facing facing, Vec3d hit) {
        //We handle it at client
        if (world.isServer) {
            return ClickResult.ACCEPTED;
        }

        if (!player.hasPermission(Permissions.STOCK_ASSEMBLY)) {
            return ClickResult.REJECTED;
        }

        ItemStack stack = player.getHeldItem(hand);

        ConsistDefinition def = ConsistDefinitionManager.getConsistDefinition(stack.getTagCompound().getString("multi_unit"));
        if (def == null) {
            player.sendMessage(PlayerMessage.direct("Invalid consist"));
            return ClickResult.REJECTED;
        }

        new ConsistPlacePacket(def, pos).sendToServer();

        return ClickResult.ACCEPTED;
    }

    @Override
    public void onClickAir(Player player, World world, Player.Hand hand) {
        super.onClickAir(player, world, hand);
        if (world.isClient) {
            GuiTypes.CONSIST_PLACER_MAIN.open(player);
        }
    }
}
