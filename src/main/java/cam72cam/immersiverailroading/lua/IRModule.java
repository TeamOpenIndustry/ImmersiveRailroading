package cam72cam.immersiverailroading.lua;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.library.unit.PressureDisplayType;
import cam72cam.immersiverailroading.library.unit.SpeedDisplayType;
import cam72cam.immersiverailroading.library.unit.TemperatureDisplayType;
import cam72cam.immersiverailroading.net.SoundPacket;
import cam72cam.mod.ModCore;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;
import de.poizzy.luaj.LuaTable;
import de.poizzy.luaj.LuaValue;
import de.poizzy.luaj.annotations.LuaFunction;
import de.poizzy.luaj.lib.LuaModule;

import java.util.*;

import static de.poizzy.luaj.LuaValue.*;

public class IRModule implements LuaModule {
    private final EntityScriptableRollingStock stock;
    private LuaValue env;

    public IRModule(EntityScriptableRollingStock stock) {
        this.stock = stock;
    }

    @Override
    public String name() {
        return "IR";
    }

    @Override
    public void init(LuaTable self, LuaValue name, LuaValue env) {
        this.env = env;
    }

    @LuaFunction
    public LuaValue setCG(LuaValue ctrl, LuaValue value) {
        String control = ctrl.tojstring();
        float val = value.tofloat();

        stock.setControlPosition(control, val);
        return NIL;
    }

    @LuaFunction
    public LuaValue getCG(LuaValue control) {
        float pos = stock.getControlPosition(control.tojstring());
        return LuaValue.valueOf(pos);
    }

    @LuaFunction
    public LuaValue getPaint() {
        return LuaValue.valueOf(stock.getTexture());
    }

    @LuaFunction
    public LuaValue setPaint(LuaValue textureName) {
        stock.setTexture(textureName.tojstring());
        return NIL;
    }

    @LuaFunction
    public LuaValue getReadout(LuaValue readout) {
        Readouts readouts = Readouts.valueOf(readout.tojstring().toUpperCase());
        float value = readouts.getValue(stock);
        return LuaValue.valueOf(value);
    }

    @LuaFunction
    public LuaValue couplerEngaged(LuaValue position, LuaValue engaged) {
        EntityCoupleableRollingStock.CouplerType type = EntityCoupleableRollingStock.CouplerType.valueOf(position.tojstring().toUpperCase());
        stock.setCouplerEngaged(type, engaged.toboolean());
        return NIL;
    }

    @LuaFunction
    public LuaValue setThrottle(LuaValue value) {
        if (stock instanceof Locomotive) {
            ((Locomotive) stock).setThrottle(value.tofloat());
        }
        return NIL;
    }

    @LuaFunction
    public LuaValue getThrottle() {
        if (stock instanceof Locomotive) {
            return LuaValue.valueOf(((Locomotive) stock).getThrottle());
        }
        return LuaValue.valueOf(0);
    }

    @LuaFunction
    public LuaValue setReverser(LuaValue value) {
        if (stock instanceof Locomotive) {
            ((Locomotive) stock).setReverser(value.tofloat());
        }
        return NIL;
    }

    @LuaFunction
    public LuaValue getReverser() {
        if (stock instanceof Locomotive) {
            return LuaValue.valueOf(((Locomotive) stock).getReverser());
        }
        return LuaValue.valueOf(0);
    }

    @LuaFunction
    public LuaValue setTrainBrake(LuaValue value) {
        if (stock instanceof Locomotive) {
           ((Locomotive) stock).setTrainBrake(value.tofloat());
        }
        return NIL;
    }

    @LuaFunction
    public LuaValue getTrainBrake() {
        if (stock instanceof Locomotive) {
            float brake = ((Locomotive) stock).getTrainBrake();
            return LuaValue.valueOf(brake);
        }

        return (LuaValue.valueOf(0));
    }

    @LuaFunction
    public LuaValue setIndependentBrake(LuaValue value) {
        stock.setIndependentBrake(value.tofloat());
        return NIL;
    }

    @LuaFunction(name = "getIndependentBrake")
    public LuaValue getIndependentBrakeLua() {
        return LuaValue.valueOf(stock.getIndependentBrake());
    }

    @LuaFunction
    public LuaValue setGlobal(LuaValue control, LuaValue value) {
        stock.mapTrain(stock, false, stock -> stock.setControlPosition(control.tojstring(), value.tofloat()));
        return NIL;
    }

    @LuaFunction
    public LuaValue setTag(LuaValue tag) {
        stock.setEntityTag(tag.tojstring());
        return NIL;
    }

    @LuaFunction
    public LuaValue getTag() {
        return LuaValue.valueOf(stock.tag);
    }

    @LuaFunction
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

    @LuaFunction
    public LuaValue isTurnedOn() {
        if (stock instanceof LocomotiveDiesel diesel) {
            return LuaValue.valueOf(diesel.isTurnedOn());
        }
        return LuaValue.valueOf(false);
    }

    @LuaFunction
    public LuaValue engineStartStop(LuaValue bool) {
       if (stock instanceof LocomotiveDiesel) {
           ((LocomotiveDiesel) stock).setTurnedOn(bool.toboolean());
       }
       return NIL;
    }

    public LuaValue newParticle() {
        // Currently Disabled
        return NIL;
    }

    @LuaFunction
    public LuaValue getStockPosition() {
        return VectorModule.convertToLua(stock.getPosition(), env);
    }

//    @LuaFunction
//    public LuaValue getStockMatrix() {
//        return ScriptVectorUtil.constructMatrix4Table(stock.getModelMatrix());
//    }

    @LuaFunction
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

    @LuaFunction
    public LuaValue isBuilt() {
        return LuaValue.valueOf(stock.isBuilt());
    }

    @LuaFunction
    public LuaValue playSound(LuaValue identifier, LuaValue luaPos, LuaValue volume) {
        Vec3d pos;
        if (luaPos != LuaValue.NIL) {
            Vec3d objPos = VectorModule.convertToJava(luaPos);
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
            return NIL;
        }

        new SoundPacket(sound, pos, stock.getVelocity(), volume.tofloat(), 1, 10, ConfigSound.SoundCategories.controls(), SoundPacket.PacketSoundCategory.SCRIPTED).sendToObserving(stock);
        return NIL;
    }

    @LuaFunction
    public LuaValue getObjectPos(LuaValue name) {
        Optional<String> object = stock.getDefinition().getModel().model.groups().stream().filter(s -> s.contains(name.toString())).findFirst();

        if (object.isEmpty()) {
            return LuaValue.NIL;
        }

        Vec3d center = stock.getDefinition().getModel().model.centerOfGroups(Collections.singletonList(object.get()));
        return VectorModule.convertToLua(center, env);
    }

    @LuaFunction
    public LuaValue getPassengerCountLua() {
        return LuaValue.valueOf(stock.getPassengerCount());
    }

    @LuaFunction
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

    @LuaFunction
    public LuaValue getSpeed(LuaValue unit) {
        double kmh = stock.getCurrentSpeed().metric();
        if (unit.isnil()) {
            return LuaValue.valueOf(kmh);
        }
        SpeedDisplayType type = SpeedDisplayType.valueOf(unit.tojstring());
        return LuaValue.valueOf(type.convertFromKmh(kmh));
    }

    @LuaFunction
    public LuaValue getTemperature(LuaValue unit) {
        float temp;
        switch (stock) {
            case LocomotiveSteam steam -> temp = steam.getBoilerTemperature();
            case LocomotiveDiesel diesel -> temp = diesel.getEngineTemperature();
            default -> temp = 0;
        }

        if (unit.isnil()) {
            return valueOf(temp);
        }

        TemperatureDisplayType type = TemperatureDisplayType.valueOf(unit.tojstring());
        return valueOf(type.convertFromCelcius(temp));
    }

    @LuaFunction
    public LuaValue getBoilerPressure(LuaValue unit) {
        float pressure = 0.0f;
        if (stock instanceof LocomotiveSteam steam) {
            pressure = steam.getBoilerPressure();
        }

        if (unit.isnil()) {
            return valueOf(pressure);
        }

        PressureDisplayType type = PressureDisplayType.valueOf(unit.tojstring());

        return valueOf(type.convertFromPSI(pressure));
    }

    @LuaFunction
    public LuaValue isSliding() {
        return LuaValue.valueOf(stock.isSliding());
    }

    @LuaFunction
    public LuaValue getBrakePressure() {
        return LuaValue.valueOf(stock.getBrakePressure());
    }

    @LuaFunction
    public LuaValue getWeight() {
        return LuaValue.valueOf(stock.getWeight());
    }
}
