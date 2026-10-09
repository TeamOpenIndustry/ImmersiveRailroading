package cam72cam.immersiverailroading.script.modules;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.net.SoundPacket;
import cam72cam.immersiverailroading.script.LuaFunction;
import cam72cam.immersiverailroading.script.LuaModule;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.mod.ModCore;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.*;

public class IRModule implements LuaModule {
    private final EntityScriptableRollingStock stock;

    public IRModule(EntityScriptableRollingStock stock) {
        this.stock = stock;
    }

    @LuaFunction(module = "IR")
    public void setCG(LuaValue ctrl, LuaValue value) {
        String control = ctrl.tojstring();
        float val = value.tofloat();

        stock.setControlPosition(control, val);
    }

    @LuaFunction(module = "IR")
    public LuaValue getCG(LuaValue control) {
        float pos = stock.getControlPosition(control.tojstring());
        return LuaValue.valueOf(pos);
    }

    @LuaFunction(module = "IR")
    public LuaValue getPaint() {
        return LuaValue.valueOf(stock.getTexture());
    }

    @LuaFunction(module = "IR")
    public void setPaint(LuaValue textureName) {
        stock.setTexture(textureName.tojstring());
    }

    @LuaFunction(module = "IR")
    public LuaValue getReadout(LuaValue readout) {
        Readouts readouts = Readouts.valueOf(readout.tojstring().toUpperCase());
        float value = readouts.getValue(stock);
        return LuaValue.valueOf(value);
    }

    @LuaFunction(module = "IR")
    public void couplerEngaged(LuaValue position, LuaValue engaged) {
        EntityCoupleableRollingStock.CouplerType type = EntityCoupleableRollingStock.CouplerType.valueOf(position.tojstring().toUpperCase());
        stock.setCouplerEngaged(type, engaged.toboolean());
    }

    @LuaFunction(module = "IR")
    public void setThrottle(LuaValue value) {
        if (stock instanceof Locomotive) {
            ((Locomotive) stock).setThrottle(value.tofloat());
        }
    }

    @LuaFunction(module = "IR")
    public LuaValue getThrottle() {
        if (stock instanceof Locomotive) {
            return LuaValue.valueOf(((Locomotive) stock).getThrottle());
        }
        return LuaValue.valueOf(0);
    }

    @LuaFunction(module = "IR")
    public void setReverser(LuaValue value) {
        if (stock instanceof Locomotive) {
            ((Locomotive) stock).setReverser(value.tofloat());
        }
    }

    @LuaFunction(module = "IR")
    public LuaValue getReverser() {
        if (stock instanceof Locomotive) {
            return LuaValue.valueOf(((Locomotive) stock).getReverser());
        }
        return LuaValue.valueOf(0);
    }

    @LuaFunction(module = "IR")
    public void setTrainBrake(LuaValue value) {
        if (stock instanceof Locomotive) {
           ((Locomotive) stock).setTrainBrake(value.tofloat());
        }
    }

    @LuaFunction(module = "IR")
    protected LuaValue getTrainBrake() {
        if (stock instanceof Locomotive) {
            float brake = ((Locomotive) stock).getTrainBrake();
            return LuaValue.valueOf(brake);
        }

        return (LuaValue.valueOf(0));
    }

    @LuaFunction(module = "IR")
    public void setIndependentBrake(LuaValue value) {
        stock.setIndependentBrake(value.tofloat());
    }

    @LuaFunction(module = "IR", name = "getIndependentBrake")
    public LuaValue getIndependentBrakeLua() {
        return LuaValue.valueOf(stock.getIndependentBrake());
    }

    @LuaFunction(module = "IR")
    public void setGlobal(LuaValue control, LuaValue value) {
        stock.mapTrain(stock, false, stock -> stock.setControlPosition(control.tojstring(), value.tofloat()));
    }

    @LuaFunction(module = "IR")
    public void setTag(LuaValue tag) {
        stock.setEntityTag(tag.tojstring());
    }

    @LuaFunction(module = "IR")
    public LuaValue getTag(LuaValue tag) {
        return LuaValue.valueOf(stock.tag);
    }

    @LuaFunction(module = "IR", name = "getTrain")
    public LuaTable getTrainConsist() {
        List<EntityCoupleableRollingStock> train = stock.getTrain();

        LuaTable consist = new LuaTable();

        for (EntityCoupleableRollingStock rollingStock : train) {
            LuaTable stockTable = new LuaTable();

            stockTable.set("UUID", LuaValue.valueOf(stock.getUUID().toString()));
            stockTable.set("coupledFront", LuaValue.valueOf(stock.coupledFront.toString()));
            stockTable.set("coupledBack", LuaValue.valueOf(stock.coupledBack.toString()));

            consist.set(rollingStock.getDefinitionID(), stockTable);
        }

        return consist;
    }

    @LuaFunction(module = "IR")
    public LuaValue isTurnedOn() {
        if (stock instanceof LocomotiveDiesel diesel) {
            return LuaValue.valueOf(diesel.isTurnedOn());
        }
        return LuaValue.valueOf(false);
    }

    @LuaFunction(module = "IR")
    public void engineStartStop(LuaValue bool) {
       if (stock instanceof LocomotiveDiesel) {
           ((LocomotiveDiesel) stock).setTurnedOn(bool.toboolean());
       }
    }

    @LuaFunction(module = "IR")
    public void newParticle() {
        // Currently Disabled
    }

    @LuaFunction(module = "IR")
    public LuaValue getStockPosition() {
        return ScriptVectorUtil.constructVec3Table(stock.getPosition());
    }

    @LuaFunction(module = "IR")
    public LuaValue getStockMatrix() {
        return ScriptVectorUtil.constructMatrix4Table(stock.getModelMatrix());
    }

    @LuaFunction(module = "IR")
    public LuaValue newVector(LuaValue x, LuaValue y, LuaValue z) {
        return ScriptVectorUtil.constructVec3Table(x, y, z);
    }

    @LuaFunction(module = "IR")
    public LuaTable getCoupled(LuaValue type) {
        LuaTable table = new LuaTable();
        String sType = type.tojstring();

        EntityCoupleableRollingStock rollingStock = stock.getCoupled(EntityCoupleableRollingStock.CouplerType.valueOf(sType.toUpperCase()));
        EntityCoupleableRollingStock.CouplerType coupler = stock.getCouplerFor(rollingStock);
        UUID uuid = rollingStock.getUUID();
        String defID = rollingStock.getDefinitionID();
        String tag = rollingStock.tag;

        table.set("coupler", coupler.toString());
        table.set("uuid", uuid.toString());
        table.set("defID", defID);
        table.set("tag", tag);
        return table;
    }

    @LuaFunction(module = "IR", name = "isBuilt")
    public LuaValue isBuiltLua() {
        return LuaValue.valueOf(stock.isBuilt());
    }

    @LuaFunction(module = "IR")
    public void playSound(LuaValue identifier, LuaValue luaPos, LuaValue volume) {
        Vec3d pos;
        if (luaPos != LuaValue.NIL) {
            Vec3d objPos = ScriptVectorUtil.convertToVec3d(luaPos);
            pos = stock.getPosition().add(objPos);
        } else {
            pos = stock.getPosition();
        }

        if (volume == LuaValue.NIL) {
            volume = LuaValue.valueOf(1);
        }

        Identifier sound = new Identifier(ImmersiveRailroading.MODID, identifier.tojstring());

        if (!sound.canLoad()) {
            ModCore.error("[Lua] Sound file %s does not exist! Not playing sound.", sound.toString());
            return;
        }

        new SoundPacket(sound, pos, stock.getVelocity(), volume.tofloat(), 1, 10, ConfigSound.SoundCategories.controls(), SoundPacket.PacketSoundCategory.SCRIPTED).sendToObserving(stock);
    }

    @LuaFunction(module = "IR")
    public LuaValue getObjectPos(LuaValue name) {
        Optional<String> object = stock.getDefinition().getModel().model.groups().stream().filter(s -> s.contains(name.toString())).findFirst();

        if (object.isEmpty()) {
            return LuaValue.NIL;
        }

        Vec3d center = stock.getDefinition().getModel().model.centerOfGroups(Collections.singletonList(object.get()));
        return ScriptVectorUtil.constructVec3Table(center);
    }

    @LuaFunction(module = "IR", name = "getPassengerCount")
    public LuaValue getPassengerCountLua() {
        return LuaValue.valueOf(stock.getPassengerCount());
    }

    @LuaFunction(module = "IR")
    public LuaValue isStockFlipped() {
        Collection<EntityCoupleableRollingStock.DirectionalStock> train = stock.getDirectionalTrain(false);

        boolean flipped = false;
        for (EntityCoupleableRollingStock.DirectionalStock directionalStock : train) {
            if (directionalStock.stock.getUUID().equals(stock.getUUID())) {
                flipped = !directionalStock.direction;
                break;
            }
        }

        return LuaValue.valueOf(flipped);
    }

    @LuaFunction(module = "IR")
    public LuaValue getSpeedKmh() {
        Speed speed = stock.getCurrentSpeed();
        return LuaValue.valueOf(speed.metric());
    }

    @LuaFunction(module = "IR")
    public LuaValue getSpeedMph() {
        Speed speed = stock.getCurrentSpeed();
        return LuaValue.valueOf(speed.imperial());
    }

    @LuaFunction(module = "IR")
    public LuaValue getTemperature() {
        if (stock instanceof LocomotiveSteam) {
            float temp = ((LocomotiveSteam) stock).getBoilerTemperature();
            return LuaValue.valueOf(temp);
        } else if (stock instanceof LocomotiveDiesel) {
            float temp = ((LocomotiveDiesel) stock).getEngineTemperature();
            return LuaValue.valueOf(temp);
        }

        return LuaValue.valueOf(0);
    }

    @LuaFunction(module = "IR", name = "getBoilerPressure")
    public LuaValue getBoilerPressureLua() {
        if (stock instanceof LocomotiveSteam) {
            float pressure = ((LocomotiveSteam) stock).getBoilerPressure();
            return LuaValue.valueOf(pressure);
        }

        return LuaValue.valueOf(0);
    }

    @LuaFunction(module = "IR")
    public LuaValue isSliding() {
        return LuaValue.valueOf(stock.isSliding());
    }

    @LuaFunction(module = "IR")
    public LuaValue getBrakePressure() {
        return LuaValue.valueOf(stock.getBrakePressure());
    }

    @LuaFunction(module = "IR")
    public LuaValue getWeight() {
        return LuaValue.valueOf(stock.getWeight());
    }
}
