package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.gui.components.ListSelector;
import cam72cam.immersiverailroading.multiblock.CustomCrafterMultiblock;
import cam72cam.immersiverailroading.multiblock.CustomTransporterMultiblock;
import cam72cam.immersiverailroading.registry.MultiblockDefinition;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.mod.entity.Player;
import cam72cam.mod.fluid.FluidStack;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.Button;
import cam72cam.mod.gui.screen.IScreen;
import cam72cam.mod.gui.screen.IScreenBuilder;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.opengl.RenderState;
import org.apache.commons.lang3.tuple.Pair;
import util.Matrix4;

import java.util.*;

public class CustomTransportMultiblockScreen implements IScreen {
    private final TileMultiblock tile;
    private final MultiblockDefinition def;
    private ListSelector<String> tankSelector;

    private String name;
    private String tankType;
    private String tankCapability;
    private final Map<String, Pair<ItemStack, Integer>> map = new HashMap<>();

    public CustomTransportMultiblockScreen(TileMultiblock tile) {
        this.tile = tile;
        MultiblockDefinition def1;
        try{
            def1 = ((CustomCrafterMultiblock.CrafterMbInstance) tile.getMultiblock()).def;
        }catch (ClassCastException e){
            def1 = ((CustomTransporterMultiblock.TransporterMbInstance) tile.getMultiblock()).def;
        }
        def = def1;
    }

    @Override
    public void init(IScreenBuilder screen) {
        screen.addButton(new Button(screen, (int) (-GUIHelpers.getScreenWidth() / 2.3d),GUIHelpers.getScreenHeight() / 4,
                                120,20,"Select output target") {
            @Override
            public void onClick(Player.Hand hand) {
                tankSelector.setVisible(!tankSelector.isVisible());
            }
        });
        screen.addButton(new Button(screen, (int) (-GUIHelpers.getScreenWidth() / 2.3d),GUIHelpers.getScreenHeight() / 4 + 20,
                120,20,"Delete fluid inside") {
            @Override
            public void onClick(Player.Hand hand) {
                tile.getFluidContainer().drain(new FluidStack(tile.getFluidContainer().getContents().getFluid(),
                        tile.getFluidContainer().getCapacity()), false);
            }
        });
        CustomTransporterMultiblock.MultiblockPackage pack = CustomTransporterMultiblock.packages.get(tile.getPos());
        tankSelector = new ListSelector<String>(screen, (int) (GUIHelpers.getScreenWidth() / 2d - GUIHelpers.getScreenWidth() / 2.3d + 130),
                180, 20, "null", pack.guiMap) {
            @Override
            public void onClick(String option) {
                pack.setTarget(option);
            }
        };
        name = ("Name: "+def.name+"#"+(String.valueOf(tile.hashCode()).substring(0,4)));
        String fluidType = tile.getFluidContainer().getContents().getFluid().ident;
        tankType = ("Fluid: "+(fluidType.equals("empty")?"null":fluidType));
        tankCapability = ("Tank: %d/"+def.tankCapability);
    }

    @Override
    public void onEnterKey(IScreenBuilder builder) {

    }

    @Override
    public void onClose() {

    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
        GUIHelpers.drawRect(0, 0, GUIHelpers.getScreenWidth(), GUIHelpers.getScreenHeight(), 0xAA000000);

        map.clear();
        for (int i = 0; i < tile.getContainer().getSlotCount(); i++) {
            ItemStack stack = tile.getContainer().get(i);
            String key = stack.getDisplayName();
            if (map.containsKey(key)) {
                map.put(key, Pair.of(stack, map.get(key).getRight() + tile.getContainer().get(i).getCount()));
            } else {
                map.put(key, Pair.of(stack, tile.getContainer().get(i).getCount()));
            }
        }
        List<Map.Entry<String, Pair<ItemStack, Integer>>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, (entry1, entry2) -> {
            //Item count
            if (entry1.getValue().getRight().intValue() != entry2.getValue().getRight().intValue())
                return entry2.getValue().getRight() - entry1.getValue().getRight();
            return entry1.getValue().getLeft().getDisplayName().compareTo(entry2.getValue().getLeft().getDisplayName());
        });

        Matrix4 matrix4 = state.model_view().copy();
        Matrix4 mat2 = matrix4.copy();
        mat2.translate(GUIHelpers.getScreenWidth() / 2d - GUIHelpers.getScreenWidth() / 2.3d + 60,
                GUIHelpers.getScreenHeight() / 4d,0.5);
        GUIHelpers.drawCenteredString(name, 0,0,0xFFFFFF, mat2);
        mat2.translate(0,20,0);
        GUIHelpers.drawCenteredString(tankType, 0,0,0xFFFFFF, mat2);
        mat2.translate(0,20,0);
        GUIHelpers.drawCenteredString(String.format(tankCapability, tile.getFluidContainer().getContents().getAmount()),
                0,0,0xFFFFFF, mat2);

        matrix4.translate(GUIHelpers.getScreenWidth() / 16d + 150,GUIHelpers.getScreenHeight() / 16d,0.5);
        Matrix4 matrix41 = matrix4.copy();
        if(!tankSelector.isVisible()){
            GUIHelpers.drawCenteredString("Inventory", 50, 0, 0xFFFFFF, matrix4);
            for (int i = 0; i<list.size(); i++) {
                if (map.get(list.get(i).getKey()).getRight() != 0) {
                    if (i % 10 == 0 && i != 0) {
                        matrix41.translate(70, -200, 0);
                        if (i == 30) {
                            break;
                        }
                    }
                    matrix41.translate(0, 20, 10);
                    GUIHelpers.drawItem(list.get(i).getValue().getLeft(), 0, 0, matrix41);
                    GUIHelpers.drawCenteredString(" : " + list.get(i).getValue().getRight(), 30, 3, 0xFFFFFF, matrix41);
                }
            }
        }
    }
}
