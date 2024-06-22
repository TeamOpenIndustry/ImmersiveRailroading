package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.multiblock.CustomCrafterMultiblock;
import cam72cam.immersiverailroading.registry.MultiblockDefinition;
import cam72cam.immersiverailroading.tile.TileMultiblock;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.Button;
import cam72cam.mod.gui.screen.IScreen;
import cam72cam.mod.gui.screen.IScreenBuilder;
import cam72cam.mod.render.opengl.RenderState;

public class CustomMultiblockScreen implements IScreen {
    private TileMultiblock tile;
    private final MultiblockDefinition def;
    private Button button;

    public CustomMultiblockScreen(TileMultiblock tile) {
        //In order to avoid the usage of IContainer, players shall click the mb to put in/out items
        this.tile = tile;
        def = ((CustomCrafterMultiblock.CrafterMbInstance) tile.getMultiblock()).def;
    }

    @Override
    public void init(IScreenBuilder screen) {
//        DataBlock selector = def.gui.getBlock("selector");
//        AlignType type = AlignType.getAlignType(selector.getValue("screen_x").asString(), selector.getValue("screen_y").asString());

//        button = new Button(screen,
//                selector.getValue("x").asInteger() + type.xAlign - GUIHelpers.getScreenWidth() / 2,
//                selector.getValue("y").asInteger() + type.yAlign,
//                selector.getValue("width").asInteger(),
//                selector.getValue("height").asInteger(),
//                "Selector") {
//            @Override
//            public void onClick(Player.Hand hand) {
//                CraftPicker.showCraftPicker(screen, ItemStack.EMPTY, def.craftingType, def.packs, item -> {
//                    if (item != null) {
//                        if (item.is(IRItems.ITEM_ROLLING_STOCK)) {
//                            ItemRollingStock.Data stock = new ItemRollingStock.Data(item);
//                            item = new ItemStack(IRItems.ITEM_PLATE, 1);
//                            ItemPlate.Data data = new ItemPlate.Data(item);
//                            data.def = stock.def;
//                            data.gauge = Gauge.from(Gauge.STANDARD);
//                            data.type = PlateType.BOILER;
//                            data.write();
//                        }
//
//                        ItemPlate.Data data = new ItemPlate.Data(item);
//                        EntityRollingStockDefinition def = data.def;
//                    }
//                });
//            }
//        };
    }

    @Override
    public void onEnterKey(IScreenBuilder builder) {

    }

    @Override
    public void onClose() {

    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
//        state.blend(new BlendMode(BlendMode.GL_SRC_COLOR, BlendMode.GL_DST_COLOR));
//        List<DataBlock> image = def.gui.getBlocks("image");
//        for (DataBlock block : image) {
//            AlignType blockAlign = AlignType.getAlignType(block.getValue("screen_x").asString(), block.getValue("screen_y").asString());
//            int width = block.getValue("width").asInteger();
//            int height = block.getValue("height").asInteger();
//            DirectDraw draw = new DirectDraw();
//            draw.vertex(0, 0, 0).uv(0, 0);
//            draw.vertex(0, height, 0).uv(0, 1);
//            draw.vertex(width, height, 0).uv(1, 1);
//            draw.vertex(width, 0, 0).uv(1, 0);
//            draw.draw(state.clone()
//                    .translate(blockAlign.xAlign, blockAlign.yAlign, 0)
//                    .texture(Texture.wrap(block.getValue("loc").asIdentifier()))
//                    .alpha_test(false)
//                    .blend(new BlendMode(BlendMode.GL_SRC_ALPHA, BlendMode.GL_ONE_MINUS_SRC_ALPHA))
//            );
//        }
//
//        DataBlock energy = def.gui.getBlock("energy");
//        AlignType energyAlign = AlignType.getAlignType(energy.getValue("screen_x").asString(), energy.getValue("screen_y").asString());
//        int width = energy.getValue("width").asInteger();
//        int height = energy.getValue("height").asInteger();
//        DirectDraw draw = new DirectDraw();
//        draw.vertex(0, 0, 0).uv(0, 0);
//        draw.vertex(0, height, 0).uv(0, 1);
//        draw.vertex(width, height, 0).uv(1, 1);
//        draw.vertex(width, 0, 0).uv(1, 0);
//        draw.draw(state.clone()
//                .translate(energyAlign.xAlign, energyAlign.yAlign, 0)
//                .scale((float) tile.getEnergyContainer().getCurrent() / tile.getEnergyContainer().getMax(),1,1)
//                .color(1,0,0,1)
//                .alpha_test(false)
//                .blend(new BlendMode(BlendMode.GL_SRC_ALPHA, BlendMode.GL_ONE_MINUS_SRC_ALPHA))
//        );
    }

    private enum AlignType{
        TOP_LEFT(0, 0),
        TOP_MIDDLE(GUIHelpers.getScreenWidth() / 2, 0),
        TOP_RIGHT(GUIHelpers.getScreenWidth() / 2, 0),
        MIDDLE_LEFT(0, GUIHelpers.getScreenHeight() / 2),
        MIDDLE(GUIHelpers.getScreenWidth() / 2,GUIHelpers.getScreenHeight() / 2),
        MIDDLE_RIGHT(GUIHelpers.getScreenWidth(), GUIHelpers.getScreenHeight() / 2),
        BOTTOM_LEFT(0, GUIHelpers.getScreenHeight()),
        BOTTOM_MIDDLE(GUIHelpers.getScreenWidth() / 2, GUIHelpers.getScreenHeight()),
        BOTTOM_RIGHT(GUIHelpers.getScreenWidth(), GUIHelpers.getScreenHeight());

        public final int xAlign;
        public final int yAlign;
        AlignType(int x, int y){
            this.xAlign = x;
            this.yAlign = y;
        }

        public static AlignType getAlignType(String x, String y){
            try{
                return AlignType.valueOf(y + "_" + x);
            }catch (Exception e){
                return MIDDLE;
            }
        }
    }
}
