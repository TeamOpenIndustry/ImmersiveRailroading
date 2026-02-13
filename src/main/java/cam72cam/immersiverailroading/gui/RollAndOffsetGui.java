package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.net.ItemRailUpdatePacket;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.util.RollAndOffsetInfo;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui.screen.*;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.opengl.RenderState;
import net.minecraftforge.fml.client.config.GuiSlider;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class RollAndOffsetGui implements IScreen {
    long frame;
    //to be modified
    private RailSettings.Mutable settings;
    private List<Double> xs = new ArrayList<>();
    private List<Vec3d> rolls = new ArrayList<>();
    private List<Vec3d> rollCtrls = new ArrayList<>();
    private List<Vec3d> offsets = new ArrayList<>();
    private List<Vec3d> offsetCtrls = new ArrayList<>();
    //not modified
    private TileRailPreview te;
    private int selectedWay;

    //components
    //TODO:先用普通滚动条，可能需要pair判断此处x是否有插入点，另外要实现一个可视化曲线
    // 另外需要在曲线上实现点击选取的功能，现在的组件没办法显示点列表
    private Button insertPoint;
    private Button deletePoint;
    private Slider tSlider;
    private boolean existedInMap = false;
    //roll
    private TextField rollValueInput;
    private TextField rollDerivativeInput;
    private TextField rollCtrlInput;
    //height offset
    private TextField yOffsetValueInput;
    private TextField yOffsetDerivativeInput;
    private TextField yOffsetCtrlInput;
    public RollAndOffsetGui() {//GUI
        this(MinecraftClient.getPlayer().getHeldItem(Player.Hand.PRIMARY));
    }
    private RollAndOffsetGui(ItemStack stack) {
        stack = stack.copy();
        settings = RailSettings.from(stack).mutable();

        if(settings.rollAndOffsetInfo != null) {
            RollAndOffsetInfo rollAndOffsetInfo = settings.rollAndOffsetInfo;
            xs = rollAndOffsetInfo.xs;
            rolls = rollAndOffsetInfo.rolls;
            rollCtrls = rollAndOffsetInfo.rollCtrls;
            offsets = rollAndOffsetInfo.offsets;
            offsetCtrls = rollAndOffsetInfo.offsetCtrls;
        }
    }
    public RollAndOffsetGui(TileRailPreview te) {//BLOCK GUI
        this(te.getItem());
        this.te = te;
    }

    public void init(IScreenBuilder screen) {
        int width = 200;
        int height = 20;
        int xtop = -GUIHelpers.getScreenWidth() / 2;
        int ytop = -GUIHelpers.getScreenHeight() / 4;

        ytop += height * 2;
        //(IScreenBuilder builder, int x, int y, int width, int height, String text, double min, double max, double start, boolean doublePrecision, Consumer<Slider> handler)
        tSlider = new Slider(screen, xtop, ytop, width * 2 - 50, height, "", 0.0, 1.0, 0.0, false, (slider) -> {}) {
            @Override
            public void onSlider() {
                tSlider.setText(String.format("%.2f", tSlider.getValue()));

                int idx = updateExistedInMap();

                if (insertPoint != null) insertPoint.setVisible(!existedInMap);
                if (deletePoint != null) deletePoint.setVisible(existedInMap);

                if(existedInMap) {
                    if(rollValueInput != null) rollValueInput.setText(String.format("%.2f", rolls.get(idx).z));
//                    if(rollDerivativeInput != null) rollCtrlInput.setText("");
//                    if(rollCtrlInput != null) rollCtrlInput.setText("");
                    if(yOffsetValueInput != null) yOffsetValueInput.setText(String.format("%.2f", offsets.get(idx).z));
//                    if(yOffsetDerivativeInput != null) yOffsetCtrlInput.setText("");
//                    if(yOffsetCtrlInput != null) yOffsetCtrlInput.setText("");
                }else {
                    if(rollValueInput != null) rollValueInput.setText("");
                    if(rollDerivativeInput != null) rollCtrlInput.setText("");
                    if(rollCtrlInput != null) rollCtrlInput.setText("");
                    if(yOffsetValueInput != null) yOffsetValueInput.setText("");
                    if(yOffsetDerivativeInput != null) yOffsetCtrlInput.setText("");
                    if(yOffsetCtrlInput != null) yOffsetCtrlInput.setText("");
                }
            }

            @Override
            public double getValue() {
                try {
                    return Double.parseDouble(String.format("%.2f",((GuiSlider) button).getValue()));
                } catch (NumberFormatException e) {
                    ImmersiveRailroading.warn("invalid text");//所见即索引
                    return 0d;
                }
            }
        };
        tSlider.onSlider();
        insertPoint = new Button(screen, xtop + width * 2 - 50, ytop, 50, height, "Insert Point") {
            @Override
            public void onClick(Player.Hand hand) {
                if(xs.isEmpty()) {
                    xs.add(0d); xs.add(1d);
                    rolls.add(new Vec3d(0, 0, 0)); rolls.add(new Vec3d(1, 0, 0));
                    rollCtrls.add(new Vec3d(0.25, 0, 0)); rollCtrls.add(new Vec3d(1.25, 0, 0));
                    offsets.add(new Vec3d(0, 0, 0)); offsets.add(new Vec3d(1, 0, 0));
                    offsetCtrls.add(new Vec3d(0.25, 0, 0)); offsetCtrls.add(new Vec3d(1.25, 0, 0));
                }

                updateExistedInMap();

                if(!existedInMap) {
                    double tSliderValue = tSlider.getValue();

                    List<Pair<Double, Double>> divider = new ArrayList<>();
                    divider.add(Pair.of(0d, tSliderValue));
                    divider.add(Pair.of(tSliderValue, 1d));

                    RollAndOffsetInfo rollAndOffsetInfo = new RollAndOffsetInfo(xs, rolls, rollCtrls, offsets, offsetCtrls);
                    List<RollAndOffsetInfo> res = rollAndOffsetInfo.subSplit(divider, false);//will divide it manually here, should be faster

                    xs = res.get(0).xs;//todo:还没做到保留x
                    xs.addAll(res.get(1).xs);

                    rolls = res.get(0).rolls;
                    rolls.addAll(res.get(1).rolls);
                    rollCtrls = res.get(0).rollCtrls;
                    rollCtrls.addAll(res.get(1).rollCtrls);
                    offsets = res.get(0).offsets;
                    offsets.addAll(res.get(1).offsets);
                    offsetCtrls = res.get(0).offsetCtrls;
                    offsetCtrls.addAll(res.get(1).offsetCtrls);
                }
            }
        };


        deletePoint = new Button(screen, xtop + width * 2 - 50, ytop, 50, height, "Delete Point") {
            @Override
            public void onClick(Player.Hand hand) {
                int idx = updateExistedInMap();

                if(existedInMap) {
                    if(idx == 0 || idx == xs.size()-1) return;

                    xs.remove(idx - 1);
                    xs.remove(idx - 1);
                    rolls.remove(idx - 1);
                    rolls.remove(idx - 1);
                    rollCtrls.remove(idx - 1);
                    rollCtrls.remove(idx - 1);
                    offsets.remove(idx - 1);
                    offsets.remove(idx - 1);
                    offsetCtrls.remove(idx - 1);
                    offsetCtrls.remove(idx - 1);

                    updateExistedInMap();
                }
            }
        };
        insertPoint.setVisible(!existedInMap);
        deletePoint.setVisible(existedInMap);
        ytop += height;

        rollValueInput = new TextField(screen, xtop, ytop, width, height);
        rollValueInput.setText("");
        rollValueInput.setValidator(s -> {
            if (s == null || s.length() == 0) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                if(s.equals(".") || s.equals("-"))return true;
                return false;
            }
            Float max = 90f;
            if (Math.abs(val) < max) {
                if(!existedInMap) return false;

                int idx = updateExistedInMap();
                Vec3d newValue = new Vec3d(rolls.get(idx).x, rolls.get(idx).y, val);
                rolls.set(idx, newValue);
                if(idx > 0 && idx < xs.size() - 1) {
                    rolls.set(idx - 1, newValue);
                }
                return true;
            }

            return false;
        });
        rollValueInput.setFocused(true);

        yOffsetValueInput = new TextField(screen, xtop + width, ytop, width, height);
        yOffsetValueInput.setText("");
        yOffsetValueInput.setValidator(s -> {
            if (s == null || s.length() == 0) {
                return true;
            }
            float val;
            try {
                val = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                if(s.equals(".") || s.equals("-"))return true;
                return false;
            }
            Float max = 90f;
            if (Math.abs(val) < max) {
                if(!existedInMap) return false;

                int idx = updateExistedInMap();
                Vec3d newValue = new Vec3d(offsets.get(idx).x, offsets.get(idx).y, val);
                offsets.set(idx, newValue);
                if(idx > 0 && idx < xs.size() - 1) {
                    offsets.set(idx - 1, newValue);
                }
                return true;
            }

            return false;
        });
        yOffsetValueInput.setFocused(true);
        ytop += height;

        rollDerivativeInput = new TextField(screen, xtop, ytop, width, height);
        yOffsetDerivativeInput = new TextField(screen, xtop + width, ytop, width, height);
        ytop += height;

        rollCtrlInput = new TextField(screen, xtop, ytop, width, height);
        yOffsetCtrlInput = new TextField(screen, xtop + width, ytop, width, height);
        ytop += height;
    }

    @Override
    public void onClose() {
        if(!xs.isEmpty()) {
            RollAndOffsetInfo rollAndOffsetInfo = new RollAndOffsetInfo(xs, rolls, rollCtrls, offsets, offsetCtrls);
            settings = settings.immutable().with(mutable -> mutable.rollAndOffsetInfo = rollAndOffsetInfo).mutable();
        }

        if (this.te != null) {
            new ItemRailUpdatePacket(te.getPos(), settings.immutable()).sendToServer();
        } else {
            new ItemRailUpdatePacket(settings.immutable()).sendToServer();
        }
    }

    @Override
    public void draw(IScreenBuilder builder, RenderState state) {
        frame ++;
    }

    private int updateExistedInMap() {//return logic index
        int res = -1;
        existedInMap = false;
        for (int i = 0; i < xs.size(); i += 2) {
            if(xs.get(i) == tSlider.getValue()) {
                existedInMap = true;
                res = i;
                break;
            }
        }
        if(!xs.isEmpty() && xs.get(xs.size() - 1) == tSlider.getValue()) {
            existedInMap = true;
            res = xs.size()-1;
        }
        return res;
    }
 }
