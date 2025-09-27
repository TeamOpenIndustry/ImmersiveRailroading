package cam72cam.immersiverailroading.items;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.gui.overlay.GuiBuilder;
import cam72cam.immersiverailroading.net.WirelessRemotecontrolInputHandler;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.Player.Hand;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.CustomItem;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.GlobalRender;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.world.World;

public class ItemWirelessRemotecontrol extends CustomItem {
    private boolean isRendering = false;
    private boolean doubleClick = false;
    
    public ItemWirelessRemotecontrol() {
        super(ImmersiveRailroading.MODID, "item_wireless_remotecontrol");
        
    }

    @Override
    public List<CreativeTab> getCreativeTabs() {
        return Collections.singletonList(ItemTabs.MAIN_TAB);
    }

    @Override
    public int getStackSize() {
        return 1;
    }

    @Override
    public List<String> getTooltip(ItemStack stack) {
        Data d = new Data(stack);
        return Collections.singletonList(d.linked == null ? "Not linked to any locomotive" : "Linked to: " + d.linked);
    }

    @Override
    public void onClickAir(Player player, World world, Hand hand) {
        if (!world.isClient) {
            return;
        } 
        
        ItemStack stack = player.getHeldItem(Player.Hand.SECONDARY);
        Data data = new Data(stack);
        if (data.linked == null) {
            return;
        }
        // Sucht in der World eine Entity mit der UUID in form einer Diesellokomotive
        // und speichern das ab
        Locomotive loco = world.getEntity(data.linked, LocomotiveDiesel.class);
        if (loco == null) {
            return;
        }

        if (isRendering && !doubleClick) {
            isRendering = false;
        } else if (!doubleClick) {
            isRendering = true;
        }
        
        try {
            GuiBuilder gui = GuiBuilder.parse(new Identifier(ImmersiveRailroading.MODID, "gui/default/fbg.json"));
            GlobalRender.registerOverlay((state, pt) -> {
                if (isRendering && !doubleClick) {
                    gui.render(state, loco);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        if (doubleClick) {
            doubleClick = false;
        } else {
            doubleClick = true;
        }

        ItemWirelessRemotecontrol.Data data1 = new ItemWirelessRemotecontrol.Data(stack);
        if (isRendering && !doubleClick && data1.linked != null) {
            WirelessRemotecontrolInputHandler.setTarget(data1.linked);
        }

    }
    
    public boolean getControl() {
        return isRendering && !doubleClick;
    }

    public static class Data extends ItemDataSerializer {
        @TagField("linked")
        public UUID linked;

        public Data(ItemStack stack) {
            super(stack);
        }
    }

}
