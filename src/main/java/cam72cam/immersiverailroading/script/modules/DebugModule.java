package cam72cam.immersiverailroading.script.modules;

import cam72cam.immersiverailroading.entity.EntityScriptableRollingStock;
import cam72cam.immersiverailroading.script.LuaFunction;
import cam72cam.immersiverailroading.script.LuaModule;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.text.PlayerMessage;
import org.luaj.vm2.LuaValue;

public class DebugModule implements LuaModule {

    public DebugModule() {}

    @LuaFunction(module = "Debug")
    public void printToInfoLog(LuaValue arg) {
        ModCore.info(arg.tojstring());
    }

    @LuaFunction(module = "Debug")
    public void printToWarnLog(LuaValue arg) {
        ModCore.warn(arg.tojstring());
    }

    @LuaFunction(module = "Debug")
    public void printToErrorLog(LuaValue arg) {
        ModCore.error(arg.tojstring());
    }

}
