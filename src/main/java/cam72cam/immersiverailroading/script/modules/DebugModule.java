package cam72cam.immersiverailroading.script.modules;

import cam72cam.mod.ModCore;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.LuaFunction;
import org.luaj.vm2.lib.LuaModule;

public class DebugModule implements LuaModule {

    public DebugModule() {}

    @LuaFunction
    public LuaValue printToInfoLog(LuaValue arg) {
        ModCore.info(arg.tojstring());
        return LuaValue.NIL;
    }

    @LuaFunction
    public LuaValue printToWarnLog(LuaValue arg) {
        ModCore.warn(arg.tojstring());
        return LuaValue.NIL;
    }

    @LuaFunction
    public LuaValue printToErrorLog(LuaValue arg) {
        ModCore.error(arg.tojstring());
        return LuaValue.NIL;
    }

    @Override
    public void init(LuaTable self, LuaValue name, LuaValue env) {
    }

    @Override
    public String name() {
        return "Debug";
    }
}
