package cam72cam.immersiverailroading.script.modules;

import cam72cam.mod.ModCore;
import de.poizzy.luaj.LuaTable;
import de.poizzy.luaj.LuaValue;
import de.poizzy.luaj.annotations.LuaFunction;
import de.poizzy.luaj.lib.LuaModule;

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
    public String name() {
        return "Debug";
    }
}
