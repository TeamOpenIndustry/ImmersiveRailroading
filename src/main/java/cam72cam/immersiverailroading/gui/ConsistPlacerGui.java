package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.gui.components.ListSelector;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.registry.ConsistDefinition;
import cam72cam.immersiverailroading.registry.ConsistDefinitionManager;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.Button;
import cam72cam.mod.gui.screen.IScreen;
import cam72cam.mod.gui.screen.IScreenBuilder;
import cam72cam.mod.gui.screen.TextField;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.net.Packet;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.serialization.TagField;

import java.util.*;
import java.util.stream.Collectors;

public class ConsistPlacerGui implements IScreen {
    public static Context context;
    private ConsistDefinition current;

    //Main Panel
    private Button selection;
    private Button newConsist;
    private Button editCurrent;
    private Button deleteCurrent;
    private ListSelector<ConsistDefinition> definitionsSelector;

    public ConsistPlacerGui() {
        this(MinecraftClient.getPlayer().getHeldItem(Player.Hand.PRIMARY));
    }

    private ConsistPlacerGui(ItemStack stack) {
        stack = stack.copy();
        current = ConsistDefinitionManager.getConsistDefinition(stack.getTagCompound().getString("multi_unit"));
        if (context == null) {
            context = new Context();
        }
        context.currentEditing = -1;
        context.player = MinecraftClient.getPlayer();
    }

    @Override
    public void init(IScreenBuilder screen) {
        context.target = Panel.MAIN;
        int xtop = -GUIHelpers.getScreenWidth() / 2;
        int ytop = -GUIHelpers.getScreenHeight() / 4;
        if(context.panel == Panel.EDIT) {
            current = context.current;
            context.current = null;
            context.currentName = null;
            context.currentAdditionPage = 0;
        }

        selection = new Button(screen, xtop, ytop, 150, 20, "Select Unit Def") {
            @Override
            public void onClick(Player.Hand hand) {
                definitionsSelector.setVisible(!definitionsSelector.isVisible());
            }
        };
        ytop += 20;
        newConsist = new Button(screen, xtop, ytop, 150, 20, "New MultiUnit") {
            @Override
            public void onClick(Player.Hand hand) {
                context.currentPage = 0;
                openEditPanel();
            }
        };
        ytop += 20;
        editCurrent = new Button(screen, xtop, ytop, 150, 20, "Edit Current") {
            @Override
            public void onClick(Player.Hand hand) {
                context.current = current;
                context.currentName = current.getName();
                openEditPanel();
            }
        };
        ytop += 20;
        deleteCurrent = new Button(screen, xtop, ytop, 150, 20, "Delete Current") {
            @Override
            public void onClick(Player.Hand hand) {
                if ("Confirm Deletion".equals(this.getText())){
                    ConsistDefinitionManager.removeConsist(current);
                    this.setText("Delete Current");
                    current = null;
                    boolean flag = false;
                    if(definitionsSelector.isVisible()){
                        definitionsSelector.setVisible(false);
                        flag = true;
                    }

                    definitionsSelector = new ListSelector<ConsistDefinition>(screen, 150, 200, 20, null,
                                                                              ConsistDefinitionManager.getValidConsists()) {
                        @Override
                        public void onClick(ConsistDefinition option) {
                            current = option;
                            if(current != null) {
                                editCurrent.setVisible(true);
                                deleteCurrent.setVisible(true);
                                editCurrent.setEnabled(current.isEditable());
                                deleteCurrent.setEnabled(current.isEditable());
                                deleteCurrent.setText("Delete Current");
                            }
                        }
                    };
                    if(flag) {
                        definitionsSelector.setVisible(true);
                    }
                    deleteCurrent.setVisible(false);
                    editCurrent.setVisible(false);
                } else {
                    this.setText("Confirm Deletion");
                }
            }
        };

        definitionsSelector = new ListSelector<ConsistDefinition>(screen, 150, 200, 20, current,
                                                                  ConsistDefinitionManager.getValidConsists()) {
            @Override
            public void onClick(ConsistDefinition option) {
                current = option;
                if(current != null) {
                    editCurrent.setVisible(true);
                    deleteCurrent.setVisible(true);
                    editCurrent.setEnabled(current.isEditable());
                    deleteCurrent.setEnabled(current.isEditable());
                    deleteCurrent.setText("Delete Current");
                }
            }
        };

        definitionsSelector.setVisible(false);

        if(context.panel == Panel.EDIT) {
            definitionsSelector.search(context.mainSearchingName);
            definitionsSelector.setPage(context.currentMainPage);
            definitionsSelector.setVisible(true);
        }

        if(current == null){
            editCurrent.setVisible(false);
            deleteCurrent.setVisible(false);
        }
        context.panel = Panel.MAIN;
    }

    private void openEditPanel() {
        context.target = Panel.EDIT;
        context.currentMainPage = definitionsSelector.getPage();
        context.mainSearchingName = definitionsSelector.getSearching();
        GuiTypes.CONSIST_EDIT.open(context.player);
    }

    @Override
    public void onEnterKey(IScreenBuilder iScreenBuilder) {
        iScreenBuilder.close();
    }

    @Override
    public void onClose() {
        if (context.target != Panel.MAIN) {
            return;
        }
        if (current != null){
            new ConsistItemChangePacket(current.getName()).sendToServer();
        }
        context = null;
    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
        IScreen.super.draw(builder, state);
        GUIHelpers.drawRect(0, 0, 150, GUIHelpers.getScreenHeight(), 0xEE000000);
        GUIHelpers.drawRect(150, 0, GUIHelpers.getScreenWidth() - 150, GUIHelpers.getScreenHeight(), 0xCC000000);
    }

    public static class Context {
        public int currentEditing;
        public Panel panel;
        public Panel target;
        public Player player;
        public List<ConsistDefinition.Stock> stockListBuilder;
        public int currentPage = 0;
        public ConsistDefinition.Stock building;
        public ConsistDefinition current;
        public String currentName;
        public int currentMainPage;
        public int currentAdditionPage;
        public String mainSearchingName;
        public String additionSearchingName;

        public void swapFront(int index) {
            if(index - 1 >= 0 && index < stockListBuilder.size()) {
                ConsistDefinition.Stock s = stockListBuilder.get(index);
                stockListBuilder.set(index, stockListBuilder.get(index - 1));
                stockListBuilder.set(index - 1, s);
            }
        }

        public void swapBack(int index) {
            if(index >= 0 && index + 1 < stockListBuilder.size()) {
                ConsistDefinition.Stock s = stockListBuilder.get(index);
                stockListBuilder.set(index, stockListBuilder.get(index + 1));
                stockListBuilder.set(index + 1, s);
            }
        }

        public void duplicate(int index) {
            ConsistDefinition.Stock s = stockListBuilder.get(index);
            stockListBuilder.add(index, s.copy());
        }

        public int index(int i) {
            return currentPage * 7 + i;
        }

        public Context() {
            this.panel = Panel.MAIN;
            this.stockListBuilder = new LinkedList<>();
        }
    }

    public static class EditPanel implements IScreen {
        private List<Button> num;
        private List<Button> stockDef;
        private List<Button> swapPrev;
        private List<Button> swapNext;
        private List<Button> copy;
        private List<Button> delete;

        private Button prevPage;
        private Button nextPage;
        private Button addStock;
        private Button saveConsist;
        private TextField name;

        @Override
        public void init(IScreenBuilder screen) {
            context.target = Panel.EDIT;

            num = new ArrayList<>(7);
            stockDef = new ArrayList<>(7);
            swapPrev = new ArrayList<>(7);
            swapNext = new ArrayList<>(7);
            copy = new ArrayList<>(7);
            delete = new ArrayList<>(7);

            int xtop = -170;
            int ytop = GUIHelpers.getScreenHeight() / 4 - 70;
            int currX = xtop;
            int spacing = 4;
            for (int i = 0; i < 7; i++) {
                Button button = new Button(screen, currX, ytop, 25, 20, "No." + i) {
                    @Override
                    public void onClick(Player.Hand hand) {}
                };
                button.setEnabled(false);
                num.add(button);
                currX += 25 + spacing;
                int finalI = i;
                stockDef.add(new Button(screen, currX, ytop, 200, 20, "") {
                    @Override
                    public void onClick(Player.Hand hand) {
                        openAdditionPanel(context.index(finalI));
                    }
                });
                currX += 200 + spacing;
                swapPrev.add(new Button(screen, currX, ytop, 20, 20, "↑") {
                    @Override
                    public void onClick(Player.Hand hand) {
                        context.swapFront(context.index(finalI));
                        updatePage();
                    }
                });
                currX += 20 + spacing;
                swapNext.add(new Button(screen, currX, ytop, 20, 20, "↓") {
                    @Override
                    public void onClick(Player.Hand hand) {
                        context.swapBack(context.index(finalI));
                        updatePage();
                    }
                });
                currX += 20 + spacing;
                copy.add(new Button(screen, currX, ytop, 30, 20, "Dup") {
                    @Override
                    public void onClick(Player.Hand hand) {
                        context.duplicate(context.index(finalI));
                        updatePage();
                    }
                });
                currX += 30 + spacing;
                delete.add(new Button(screen, currX, ytop, 30, 20, "Del") {
                    @Override
                    public void onClick(Player.Hand hand) {
                        context.stockListBuilder.remove(context.index(finalI));
                        updatePage();
                    }
                });
                ytop += 20 + spacing;
                currX = xtop;
            }
            currX = -120;
            prevPage = new Button(screen, currX, ytop, 60, 20, "Prev Page") {
                @Override
                public void onClick(Player.Hand hand) {
                    if(context.currentPage > 0) {
                        context.currentPage -= 1;
                        updatePage();
                    }
                }
            };
            currX += 80;
            addStock = new Button(screen, currX, ytop, 100, 20, "Add Stock") {
                @Override
                public void onClick(Player.Hand hand) {
                    openAdditionPanel();
                }
            };
            currX += 120;
            nextPage = new Button(screen, currX, ytop, 60, 20, "Next Page") {
                @Override
                public void onClick(Player.Hand hand) {
                    if(context.currentPage < context.stockListBuilder.size() / 7) {
                        context.currentPage += 1;
                        updatePage();
                    }
                }
            };
            xtop = -GUIHelpers.getScreenWidth() / 2;
            ytop = -GUIHelpers.getScreenHeight() / 4;
            name = new TextField(screen, xtop, ytop, 199, 20);
            name.setValidator(s -> {
                if(s == null || s.isEmpty()) {
                    saveConsist.setText("Set a name before save");
                } else {
                    saveConsist.setText("Save");
                }
                context.currentName = name.getText();
                return true;
            });
            ytop += 24;
            saveConsist = new Button(screen, xtop, ytop, 200, 20, "Save") {
                @Override
                public void onClick(Player.Hand hand) {
                    if(name.getText().isEmpty()) {
                        //Reject
                        return;
                    }
                    ConsistDefinition.ConsistDefBuilder builder1 = ConsistDefinition.ConsistDefBuilder.of(name.getText());
                    for (ConsistDefinition.Stock stock1 : context.stockListBuilder) {
                        builder1.appendStock(stock1);
                    }
                    ConsistDefinition build = builder1.build();
                    ConsistDefinitionManager.addConsist(build);
                    context.stockListBuilder.clear();
                    context.current = ConsistDefinitionManager.getConsistDefinition(build.getName());
                    GuiTypes.CONSIST_PLACER_MAIN.open(context.player);
                }
            };

            if(context.panel == Panel.MAIN){
                if (context.current == null) {
                    context.stockListBuilder = new ArrayList<>();
                } else {
                    context.stockListBuilder = new ArrayList<>(context.current.getStocks());
                    name.setText(context.currentName);
                }
            }
            if(context.panel == Panel.ADDITION) {
                name.setText(context.currentName);
            }
            if(name.getText().isEmpty()) {
                saveConsist.setText("Set a name before save");
            }

            updatePage();
            context.panel = Panel.EDIT;
        }

        private void updatePage() {
            if (context.stockListBuilder.isEmpty()) {
                for (int i = 0; i < 7; i++) {
                    this.num.get(i).setVisible(false);
                    this.stockDef.get(i).setVisible(false);
                    this.swapPrev.get(i).setVisible(false);
                    this.swapNext.get(i).setVisible(false);
                    this.copy.get(i).setVisible(false);
                    this.delete.get(i).setVisible(false);
                }
            } else {
                for (int i = 0; i < 7; i++) {
                    if (context.index(i) < context.stockListBuilder.size()) {
                        this.num.get(i).setVisible(true);
                        this.stockDef.get(i).setVisible(true);
                        this.stockDef.get(i).setText(
                                context.stockListBuilder.get(context.index(i)).definition.name());
                        this.num.get(i).setText("#" + context.index(i));
                        this.swapPrev.get(i).setVisible(true);
                        this.swapNext.get(i).setVisible(true);
                        this.copy.get(i).setVisible(true);
                        this.delete.get(i).setVisible(true);
                    } else {
                        this.num.get(i).setVisible(false);
                        this.stockDef.get(i).setVisible(false);
                        this.swapPrev.get(i).setVisible(false);
                        this.swapNext.get(i).setVisible(false);
                        this.copy.get(i).setVisible(false);
                        this.delete.get(i).setVisible(false);
                    }
                    if (context.index(i) == 0) {
                        this.swapPrev.get(i).setVisible(false);
                    }
                    if (context.index(i) == context.stockListBuilder.size() - 1) {
                        this.swapNext.get(i).setVisible(false);
                    }
                }
            }

            prevPage.setEnabled(context.currentPage != 0);
            nextPage.setEnabled(context.currentPage != ((context.stockListBuilder.size() - 1) / 7));
        }

        private void openAdditionPanel() {
            context.currentEditing = -1;
            context.target = Panel.ADDITION;
            context.currentName = name.getText();
            GuiTypes.CONSIST_ADD_STOCK.open(context.player);
        }

        private void openAdditionPanel(int num) {
            context.currentEditing = num;
            context.target = Panel.ADDITION;
            GuiTypes.CONSIST_ADD_STOCK.open(context.player);
        }

        @Override
        public void onEnterKey(IScreenBuilder builder) {
            builder.close();
        }

        @Override
        public void onClose() {
            //Auto-save
            if(!name.getText().isEmpty()) {
                ConsistDefinition.ConsistDefBuilder builder1 = ConsistDefinition.ConsistDefBuilder.of(name.getText());
                for (ConsistDefinition.Stock stock1 : context.stockListBuilder) {
                    builder1.appendStock(stock1);
                }
                ConsistDefinition build = builder1.build();
                ConsistDefinitionManager.addConsist(build);
                context.stockListBuilder.clear();
                context.current = ConsistDefinitionManager.getConsistDefinition(build.getName());
            }
            GuiTypes.CONSIST_PLACER_MAIN.open(context.player);
        }

        @Override
        public void draw(IScreenBuilder builder, RenderState state) {
            IScreen.super.draw(builder, state);
            GUIHelpers.drawRect(0, 0, GUIHelpers.getScreenWidth(), GUIHelpers.getScreenHeight(), 0xCC000000);
        }
    }

    public static class AdditionPanel implements IScreen {
        //Stock Addition Panel
        private Button stock;
        private ListSelector<EntityRollingStockDefinition> stockSelector;
        private Button texture;
        private ListSelector<String> textureSelector;
        private Button direction;
        private Button finish;

        @Override
        public void init(IScreenBuilder screen) {
            context.target = Panel.ADDITION;

            String finishStr = "Select a Stock Before Save";
            EntityRollingStockDefinition def1 = null;
            if (context.currentEditing != -1) {
                context.building = context.stockListBuilder.get(context.currentEditing);
                finishStr = "Finish";
                def1 = context.building.definition;
            } else {
                context.building = new ConsistDefinition.Stock();
                context.building.direction = ConsistDefinition.Direction.FORWARD;
            }


            int xtop = -GUIHelpers.getScreenWidth() / 2;
            int ytop = -GUIHelpers.getScreenHeight() / 4;
            stock = new Button(screen, xtop, ytop, 200, 20, def1 == null ? "Select Stock" : def1.name()) {
                @Override
                public void onClick(Player.Hand hand) {
                    if(textureSelector != null && textureSelector.isVisible()){
                        stockSelector.setVisible(true);
                        textureSelector.setVisible(false);
                    }else{
                        stockSelector.setVisible(!stockSelector.isVisible());
                    }
                }
            };
            //We should have a better way to do this...
            HashSet<String> deduplicate = new HashSet<>();
            HashSet<String> d = new HashSet<>();
            for(EntityRollingStockDefinition definition : DefinitionManager.getDefinitions()){
                if(deduplicate.contains(definition.name())) {
                    d.add(definition.name());
                }
                deduplicate.add(definition.name());
            }
            Map<String, EntityRollingStockDefinition> definitionMap = DefinitionManager.getDefinitions().stream().collect(
                    Collectors.toMap(definition -> {
                        if(d.contains(definition.name())) {
                            return String.format("%s(%s)", definition.name(), definition.defID.split("/")[2].replace(".json", ""));
                        }
                        return definition.name();
                    }, def -> def, (old, new1) -> old, TreeMap::new));
            stockSelector = new ListSelector<EntityRollingStockDefinition>(screen, 200, 200, 20, def1, definitionMap) {
                @Override
                public void onClick(EntityRollingStockDefinition option) {
                    finish.setText("Finish");
                    if (context.building != null) {
                        context.building.defID = option.defID;
                        context.building.definition = option;
                        stock.setText(option.name());
                        textureSelector = new ListSelector<String>(screen, 200, 200, 20, context.building.texture,
                                                                   context.building.definition.textureNames.entrySet().stream()
                                                                                                           .collect(Collectors.toMap(
                                                                                                           Map.Entry::getValue, Map.Entry::getKey,
                                                                                                           (u, v) -> u, LinkedHashMap::new))
                        ) {
                            @Override
                            public void onClick(String option) {
                                context.building.texture = option;
                                if(option.isEmpty()) {
                                    texture.setText("Tex Variant:" + "default");
                                } else {
                                    texture.setText("Tex Variant:" + option);
                                }
                            }
                        };
                        if(context.building.definition.textureNames.keySet().stream().findFirst().isPresent()){
                            context.building.texture = context.building.definition.textureNames.keySet().stream().findFirst().get();
                        } else {
                            context.building.texture = null;
                        }
                    }
                }
            };
            if(context.additionSearchingName != null) {
                stockSelector.search(context.additionSearchingName);
            }

            stockSelector.setPage(context.currentAdditionPage);
            stockSelector.setVisible(true);
            ytop += 25;
            texture = new Button(screen, xtop, ytop, 200, 20, "Tex Variant:default") {
                @Override
                public void onClick(Player.Hand hand) {
                    if(textureSelector == null) {
                        return;
                    }

                    if(stockSelector.isVisible()) {
                        stockSelector.setVisible(false);
                        textureSelector.setVisible(true);
                    } else {
                        textureSelector.setVisible(!textureSelector.isVisible());
                    }
                }
            };
            if(context.building.definition != null){
                textureSelector = new ListSelector<String>(screen, 200, 200, 20, context.building.texture,
                                                           context.building.definition.textureNames.entrySet().stream()
                                                                                                   .collect(
                                                                                                                        Collectors.toMap(
                                                                                                                                Map.Entry::getValue,
                                                                                                                                Map.Entry::getKey,
                                                                                                                                (u, v) -> u,
                                                                                                                                LinkedHashMap::new))
                ) {
                    @Override
                    public void onClick(String option) {
                        context.building.texture = option;
                        if (option.isEmpty()) {
                            texture.setText("Tex Variant:" + "default");
                        } else {
                            texture.setText("Tex Variant:" + option);
                        }
                    }
                };
            }
            ytop += 25;
            direction = new Button(screen, xtop, ytop, 200, 20, "") {
                @Override
                public void onClick(Player.Hand hand) {
                    context.building.direction = ClickListHelper.next(context.building.direction, hand);
                    this.setText("Direction:"+ context.building.direction);
                }
            };
            ytop += 25;
            finish = new Button(screen, xtop, ytop, 200, 20, finishStr) {
                @Override
                public void onClick(Player.Hand hand) {
                    if (context.building != null && context.building.definition != null) {
                        if (context.currentEditing == -1) {
                            context.stockListBuilder.add(context.building);
                        } else {
                            context.stockListBuilder.set(context.currentEditing, context.building);
                            context.currentEditing = -1;
                        }
                        context.building = null;
                        context.currentAdditionPage = stockSelector.getPage();
                        context.additionSearchingName = stockSelector.getSearching();
                        GuiTypes.CONSIST_EDIT.open(context.player);
                    }
                }
            };
            direction.setText("Direction:"+ context.building.direction);
            if(context.building.texture != null && !context.building.texture.isEmpty()){
                texture.setText("Tex Variant:" + context.building.texture);
            }
            context.panel = Panel.ADDITION;
        }

        @Override
        public void onEnterKey(IScreenBuilder builder) {
            builder.close();
        }

        @Override
        public void onClose() {
            context.currentAdditionPage = stockSelector.getPage();
            context.additionSearchingName = stockSelector.getSearching();
            GuiTypes.CONSIST_EDIT.open(context.player);
        }

        @Override
        public void draw(IScreenBuilder builder, RenderState state) {
            IScreen.super.draw(builder, state);
            GUIHelpers.drawRect(0, 0, 200, GUIHelpers.getScreenHeight(), 0xEE000000);
            GUIHelpers.drawRect(200, 0, GUIHelpers.getScreenWidth() - 150, GUIHelpers.getScreenHeight(), 0xCC000000);
        }
    }

    public enum Panel {
        MAIN, EDIT, ADDITION
    }

    public static class ConsistItemChangePacket extends Packet {
        @TagField
        private String def;

        public ConsistItemChangePacket() {
        }

        public ConsistItemChangePacket(String def) {
            this.def = def;
        }

        @Override
        protected void handle() {
            Player player = this.getPlayer();
            ItemStack stack = player.getHeldItem(Player.Hand.PRIMARY);
            if(stack.is(IRItems.ITEM_CONSIST_PLACER)) {
                stack.setTagCompound(stack.getTagCompound().setString("multi_unit", def));
            }
            player.setHeldItem(Player.Hand.PRIMARY, stack);
        }
    }
}
