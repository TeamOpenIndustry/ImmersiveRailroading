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
    private List<Double> ls = new ArrayList<>();
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
    private Slider lSlider;
    private boolean existedInMap = false;
    //roll
    private TextField rollValueInput;
    private TextField rollCtrlPitchInput;
    private TextField rollCtrlXLenInput;
    //height offset
    private TextField yOffsetValueInput;
    private TextField yOffsetCtrlPitchInput;
    private TextField yOffsetCtrlXLenInput;
    public RollAndOffsetGui() {//GUI
        this(MinecraftClient.getPlayer().getHeldItem(Player.Hand.PRIMARY));
    }
    private RollAndOffsetGui(ItemStack stack) {
        stack = stack.copy();
        settings = RailSettings.from(stack).mutable();

        if(settings.rollAndOffsetInfo != null) {
            RollAndOffsetInfo rollAndOffsetInfo = settings.rollAndOffsetInfo;
            ls = rollAndOffsetInfo.ls;
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
        lSlider = new Slider(screen, xtop, ytop, width * 2 - 50, height, "", 0.0, 1.0, 0.0, false, (slider) -> {}) {
            @Override
            public void onSlider() {
                lSlider.setText(String.format("%.2f", lSlider.getValue()));
                int idx = updateExistedInMap();

                if (insertPoint != null) insertPoint.setVisible(!existedInMap);
                if (deletePoint != null) deletePoint.setVisible(existedInMap);

                //todo:these may cause problems, need another way?
//                if(existedInMap) {
//                    if(rollValueInput != null) rollValueInput.setText(String.format("%.2f", rolls.get(idx).z));
//                    if(rollCtrlPitchInput != null) rollCtrlPitchInput.setText(String.format("%.2f", getRollPitch(idx)));
//                    if(rollCtrlXLenInput != null) rollCtrlXLenInput.setText(String.format("%.2f", Math.abs(rollCtrls.get(idx).x - rolls.get(idx).x)));
//
//                    if(yOffsetValueInput != null) yOffsetValueInput.setText(String.format("%.2f", offsets.get(idx).z));
//                    if(yOffsetCtrlPitchInput != null) yOffsetCtrlPitchInput.setText(String.format("%.2f", getOffsetPitch(idx)));
//                    if(yOffsetCtrlXLenInput != null) yOffsetCtrlXLenInput.setText(String.format("%.2f", Math.abs(offsetCtrls.get(idx).x - offsets.get(idx).x)));
//                }else {
//                    if(rollValueInput != null) rollValueInput.setText("");
//                    if(rollCtrlPitchInput != null) rollCtrlXLenInput.setText("");
//                    if(rollCtrlXLenInput != null) rollCtrlXLenInput.setText("");
//                    if(yOffsetValueInput != null) yOffsetValueInput.setText("");
//                    if(yOffsetCtrlPitchInput != null) yOffsetCtrlXLenInput.setText("");
//                    if(yOffsetCtrlXLenInput != null) yOffsetCtrlXLenInput.setText("");
//                }
            }

            @Override
            public double getValue() {
                try {
                    return Double.parseDouble(String.format("%.2f",((GuiSlider) button).getValue()));
                } catch (NumberFormatException e) {
                    ImmersiveRailroading.warn("invalid text");//所见即索引，max100
                    return 0d;
                }
            }
        };
        lSlider.onSlider();
        insertPoint = new Button(screen, xtop + width * 2 - 50, ytop, 50, height, "Insert Point") {
            @Override
            public void onClick(Player.Hand hand) {
                if(ls.isEmpty()) {
                    ls.add(0d); ls.add(1d);
                    rolls.add(new Vec3d(0, 0, 0)); rolls.add(new Vec3d(1, 0, 0));
                    rollCtrls.add(new Vec3d(0.25, 0, 0)); rollCtrls.add(new Vec3d(1.25, 0, 0));
                    offsets.add(new Vec3d(0, 0, 0)); offsets.add(new Vec3d(1, 0, 0));
                    offsetCtrls.add(new Vec3d(0.25, 0, 0)); offsetCtrls.add(new Vec3d(1.25, 0, 0));
                }

                updateExistedInMap();

                if(!existedInMap) {
                    double tSliderValue = lSlider.getValue();

                    List<Pair<Double, Double>> divider = new ArrayList<>();
                    divider.add(Pair.of(0d, tSliderValue));
                    divider.add(Pair.of(tSliderValue, 1d));

                    RollAndOffsetInfo rollAndOffsetInfo = new RollAndOffsetInfo(ls, rolls, rollCtrls, offsets, offsetCtrls);
                    List<RollAndOffsetInfo> res = rollAndOffsetInfo.subSplit(divider, false);//will divide it manually here, should be faster

                    ls = res.get(0).ls;
                    ls.addAll(res.get(1).ls);

                    rolls = res.get(0).rolls;
                    rolls.addAll(res.get(1).rolls);
                    rollCtrls = res.get(0).rollCtrls;
                    rollCtrls.addAll(res.get(1).rollCtrls);
                    offsets = res.get(0).offsets;
                    offsets.addAll(res.get(1).offsets);
                    offsetCtrls = res.get(0).offsetCtrls;
                    offsetCtrls.addAll(res.get(1).offsetCtrls);

//                    insertPoint.setVisible(false);
//                    deletePoint.setVisible(true);
                    //todo update text input here
                }
            }
        };


        deletePoint = new Button(screen, xtop + width * 2 - 50, ytop, 50, height, "Delete Point") {
            @Override
            public void onClick(Player.Hand hand) {
                int idx = updateExistedInMap();

                if(existedInMap) {
                    if(idx == 0 || idx == ls.size()-1) return;

                    ls.remove(idx - 1);
                    ls.remove(idx - 1);
                    rolls.remove(idx - 1);
                    rolls.remove(idx - 1);
                    rollCtrls.remove(idx - 1);
                    rollCtrls.remove(idx - 1);
                    offsets.remove(idx - 1);
                    offsets.remove(idx - 1);
                    offsetCtrls.remove(idx - 1);
                    offsetCtrls.remove(idx - 1);

//                    insertPoint.setVisible(true);
//                    deletePoint.setVisible(false);
                    //todo update text input here
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
            Float max = 10f;
            if (Math.abs(val) < max) {
                if(!existedInMap) return false;

                int idx = updateExistedInMap();

                Vec3d newValue = new Vec3d(rolls.get(idx).x, rolls.get(idx).y, val);
                Vec3d oldValue = rollCtrls.get(idx);
                double delta = val - rolls.get(idx).z;
                rolls.set(idx, newValue);
                rollCtrls.set(idx, new Vec3d(oldValue.x, oldValue.y, oldValue.z + delta));

                if(idx > 0 && idx < ls.size() - 1) {
                    oldValue = rollCtrls.get(idx - 1);
                    rolls.set(idx - 1, newValue);
                    rollCtrls.set(idx - 1, new Vec3d(oldValue.x, oldValue.y, oldValue.z + delta));
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
            Float max = (float) settings.gauge.scale() * 0.5f;
            if (Math.abs(val) < max) {
                if(!existedInMap) return false;

                int idx = updateExistedInMap();

                Vec3d newValue = new Vec3d(offsets.get(idx).x, offsets.get(idx).y, val);
                Vec3d oldValue = offsetCtrls.get(idx);
                double delta = val - offsets.get(idx).z;
                offsets.set(idx, newValue);
                offsetCtrls.set(idx, new Vec3d(oldValue.x, oldValue.y, oldValue.z + delta));

                if(idx > 0 && idx < ls.size() - 1) {
                    oldValue = offsetCtrls.get(idx - 1);
                    offsets.set(idx - 1, newValue);
                    offsetCtrls.set(idx - 1, new Vec3d(oldValue.x, oldValue.y, oldValue.z + delta));
                }
                return true;
            }

            return false;
        });
        yOffsetValueInput.setFocused(true);

        ytop += height;

        rollCtrlPitchInput = new TextField(screen, xtop, ytop, width, height);
        rollCtrlPitchInput.setText("");
        rollCtrlPitchInput.setValidator(s -> {
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
                setRollPitch(idx, val);

                if(idx > 0 && idx < ls.size() - 1) {
                    setRollPitch(idx - 1, val);
                }
                return true;
            }

            return false;
        });
        rollCtrlPitchInput.setFocused(true);

        yOffsetCtrlPitchInput = new TextField(screen, xtop + width, ytop, width, height);
        yOffsetCtrlPitchInput.setText("");
        yOffsetCtrlPitchInput.setValidator(s -> {
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
                setOffsetPitch(idx, val);

                if(idx > 0 && idx < ls.size() - 1) {
                    setOffsetPitch(idx - 1, val);
                }
                return true;
            }

            return false;
        });
        yOffsetValueInput.setFocused(true);

        ytop += height;

        rollCtrlXLenInput = new TextField(screen, xtop, ytop, width, height);
        rollCtrlXLenInput.setText("");
        rollCtrlXLenInput.setValidator(s -> {
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
            Float max = 0.5f;
            if (Math.abs(val) < max) {
                if(!existedInMap) return false;

                int idx = updateExistedInMap();
                //check length
                if(idx < ls.size() -1) {
                    double maxXLen = ls.get(idx + 1) - ls.get(idx);
                    if(val >= maxXLen * max)return false;
                }else {
                    double maxXLen = ls.get(idx) - ls.get(idx - 1);
                    if(val >= maxXLen * max)return false;
                }

                double scale =  val / rollCtrls.get(idx).x;
                Vec3d newValue = new Vec3d(val, rollCtrls.get(idx).y, rollCtrls.get(idx).z * scale);
                rollCtrls.set(idx, newValue);

                if(idx > 0 && idx < ls.size() - 1) {
                    rollCtrls.set(idx - 1, newValue);
                }
                return true;
            }

            return false;
        });
        rollCtrlXLenInput.setFocused(true);

        yOffsetCtrlXLenInput = new TextField(screen, xtop + width, ytop, width, height);
        yOffsetCtrlXLenInput.setText("");
        yOffsetCtrlXLenInput.setValidator(s -> {
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
            Float max = 0.5f;
            if (Math.abs(val) < max) {
                if(!existedInMap) return false;

                int idx = updateExistedInMap();
                //check length
                if(idx < ls.size() -1) {
                    double maxXLen = ls.get(idx + 1) - ls.get(idx);
                    if(val >= maxXLen * max)return false;
                }else {
                    double maxXLen = ls.get(idx) - ls.get(idx - 1);
                    if(val >= maxXLen * max)return false;
                }

                double scale =  val / offsetCtrls.get(idx).x;
                Vec3d newValue = new Vec3d(val, offsetCtrls.get(idx).y, offsetCtrls.get(idx).z * scale);
                offsetCtrls.set(idx, newValue);

                if(idx > 0 && idx < ls.size() - 1) {
                    offsetCtrls.set(idx - 1, newValue);
                }
                return true;
            }

            return false;
        });
        yOffsetCtrlXLenInput.setFocused(true);

        ytop += height;
    }

    @Override
    public void onClose() {
        if(!ls.isEmpty()) {
            RollAndOffsetInfo rollAndOffsetInfo = new RollAndOffsetInfo(ls, rolls, rollCtrls, offsets, offsetCtrls);
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
        for (int i = 0; i < ls.size(); i += 2) {
            if(ls.get(i) == lSlider.getValue()) {
                existedInMap = true;
                res = i;
                break;
            }
        }
        if(!ls.isEmpty() && ls.get(ls.size() - 1) == lSlider.getValue()) {
            existedInMap = true;
            res = ls.size()-1;
        }
        return res;
    }

    private double getRollPitch(int idx) {
        double up = Math.atan(rollCtrls.get(idx).z) - rolls.get(idx).z;
        double down = Math.abs(rollCtrls.get(idx).x - rolls.get(idx).x);
        return Math.toDegrees(Math.atan(up / down));
    }
    private double getOffsetPitch(int idx) {
        double up = Math.atan(offsetCtrls.get(idx).z) - offsets.get(idx).z;
        double down = Math.abs(offsetCtrls.get(idx).x - offsets.get(idx).x);
        return Math.toDegrees(Math.atan(up / down));
    }

    private void setRollPitch(int idx, double pitch) {
        double tan = Math.tan(Math.toRadians(pitch));
        double down = Math.abs(rollCtrls.get(idx).x - rolls.get(idx).x);
        double newZ = tan * down + rolls.get(idx).z;
        Vec3d oldPoint = rollCtrls.get(idx);
        rollCtrls.set(idx, new Vec3d(oldPoint.x, oldPoint.y, newZ));
    }

    private void setOffsetPitch(int idx, double pitch) {
        double tan = Math.tan(Math.toRadians(pitch));
        double down = Math.abs(offsetCtrls.get(idx).x - offsets.get(idx).x);
        double newZ = tan * down + offsets.get(idx).z;
        Vec3d oldPoint = offsetCtrls.get(idx);
        offsetCtrls.set(idx, new Vec3d(oldPoint.x, oldPoint.y, newZ));
    }
 }
