package cam72cam.immersiverailroading.script.modules;

import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.entity.EntityScriptableRollingStock;
import cam72cam.immersiverailroading.script.LuaFunction;
import cam72cam.immersiverailroading.script.LuaModule;
import cam72cam.immersiverailroading.script.library.ILuaEvent;
import cam72cam.mod.ModCore;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.util.ArrayList;
import java.util.Arrays;

public class EventModule implements LuaModule {
    private EntityScriptableRollingStock stock;
    private final ILuaEvent event;

    public EventModule(ILuaEvent event) {
        this.event = event;
        if (event instanceof EntityScriptableRollingStock) {
            this.stock = (EntityScriptableRollingStock) event;
        }
    }

    @LuaFunction(module = "Events")
    public void registerEvent(LuaValue name, LuaValue func) {
        if (func.isfunction()) {
            event.getLuaEventCallbacks().computeIfAbsent(name.tojstring(), k -> new ArrayList<>()).add(func);
        } else {
            ModCore.warn("registerEvent called with non-function for event: " + name.tojstring());
        }
    }

    @LuaFunction(module = "Events")
    public void triggerEvent(LuaValue... args) {
        if (args == null || args.length == 0) {
            ModCore.warn("[Lua] Events.triggerEvent() called without an event name");
            return;
        }
        String funcName = args[0].tojstring();
        if (funcName.equals("onTick") || funcName.equals("onControlGroupChange")) {
            ModCore.error("[Lua] Cannot call \"onTick\" or \"onControlGroupChange\" manually. Please use another name for your event!");
            return;
        }
        Varargs varargs = LuaValue.varargsOf(Arrays.copyOfRange(args, 1, args.length));

        if (stock != null) {
            stock.mapTrain(stock, false, stock -> ((EntityScriptableRollingStock) stock).triggerEvent(funcName, varargs));
        } else {
            event.triggerEvent(funcName, varargs);
        }
    }

    @LuaFunction(module = "Events")
    public void triggerCouplerEvent(LuaValue... args) {
        if (stock == null) {
            return;
        }
        if (args == null || args.length < 2) {
            ModCore.warn("[Lua] Events.triggerCouplerEvent(name, coupler, ...) needs at least an event name and a coupler side");
            return;
        }
        String functionName = args[0].tojstring();
        EntityCoupleableRollingStock.CouplerType coupler;
        try {
            coupler = EntityCoupleableRollingStock.CouplerType.valueOf(args[1].tojstring().toUpperCase());
        } catch (IllegalArgumentException e) {
            ModCore.warn("[Lua] Events.triggerCouplerEvent: unknown coupler '%s'", args[1].tojstring());
            return;
        }

        EntityScriptableRollingStock coupled = (EntityScriptableRollingStock) stock.getCoupled(coupler);
        if (coupled == null) {
            return;
        }
        coupled.triggerEvent(functionName, LuaValue.varargsOf(Arrays.copyOfRange(args, 2, args.length)));
    }
}
