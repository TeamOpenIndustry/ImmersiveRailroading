package cam72cam.immersiverailroading.lua;

import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.entity.EntityScriptableRollingStock;
import cam72cam.immersiverailroading.util.ILuaEvent;
import cam72cam.mod.ModCore;
import de.poizzy.luaj.LuaValue;
import de.poizzy.luaj.Varargs;
import de.poizzy.luaj.annotations.LuaFunction;
import de.poizzy.luaj.lib.LuaModule;

import java.util.ArrayList;

public class EventModule implements LuaModule {
    private EntityScriptableRollingStock stock;
    private final ILuaEvent event;

    public EventModule(ILuaEvent event) {
        this.event = event;
        if (event instanceof EntityScriptableRollingStock) {
            this.stock = (EntityScriptableRollingStock) event;
        }
    }

    @LuaFunction
    public LuaValue registerEvent(LuaValue name, LuaValue func) {
        if (func.isfunction()) {
            event.getLuaEventCallbacks().computeIfAbsent(name.tojstring(), k -> new ArrayList<>()).add(func);
        } else {
            ModCore.warn("registerEvent called with non-function for event: " + name.tojstring());
        }
        return LuaValue.NIL;
    }

    @LuaFunction
    public Varargs triggerEvent(Varargs args) {
        if (args == null || args.narg() == 0) {
            ModCore.warn("[Lua] Events.triggerEvent() called without an event name");
            return LuaValue.NIL;
        }
        String funcName = args.arg1().tojstring();
        if (funcName.equals("onTick") || funcName.equals("onControlGroupChange")) {
            ModCore.error("[Lua] Cannot call \"onTick\" or \"onControlGroupChange\" manually. Please use another name for your event!");
            return LuaValue.NIL;
        }
        Varargs varargs = args.subargs(2);

        if (stock != null) {
            stock.mapTrain(stock, false, stock -> ((EntityScriptableRollingStock) stock).triggerEvent(funcName, varargs));
        } else {
            event.triggerEvent(funcName, varargs);
        }
        return LuaValue.NIL;
    }

    @LuaFunction
    public Varargs triggerCouplerEvent(Varargs args) {
        if (stock == null) {
            return LuaValue.NIL;
        }
        if (args == null || args.narg() < 2) {
            ModCore.warn("[Lua] Events.triggerCouplerEvent(name, coupler, ...) needs at least an event name and a coupler side");
            return LuaValue.NIL;
        }
        String functionName = args.arg1().tojstring();
        EntityCoupleableRollingStock.CouplerType coupler;
        try {
            coupler = EntityCoupleableRollingStock.CouplerType.valueOf(args.arg(2).tojstring().toUpperCase());
        } catch (IllegalArgumentException e) {
            ModCore.warn("[Lua] Events.triggerCouplerEvent: unknown coupler '%s'", args.arg(2).tojstring());
            return LuaValue.NIL;
        }

        EntityScriptableRollingStock coupled = (EntityScriptableRollingStock) stock.getCoupled(coupler);
        if (coupled == null) {
            return LuaValue.NIL;
        }
        coupled.triggerEvent(functionName, args.subargs(3));
        return LuaValue.NIL;
    }

    @Override
    public String name() {
        return "Events";
    }
}
