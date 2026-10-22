package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.script.library.ILuaEvent;
import cam72cam.immersiverailroading.script.library.LuaSerialization;
import cam72cam.immersiverailroading.script.modules.*;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.serialization.TagField;
import de.poizzy.luaj.Globals;
import de.poizzy.luaj.LuaTable;
import de.poizzy.luaj.LuaValue;
import de.poizzy.luaj.Varargs;
import de.poizzy.luaj.annotations.LuaFunction;
import de.poizzy.luaj.function.ThreeArgFunction;
import de.poizzy.luaj.function.TwoArgFunction;
import de.poizzy.luaj.lib.LuaModule;
import de.poizzy.luaj.lib.jse.JsePlatform;

import java.util.*;

public abstract class EntityScriptableRollingStock extends EntityCoupleableRollingStock implements ILuaEvent {
    public Globals environment;
    private Boolean hasOwnScript;

    /**
     * Used by {@link EventModule}
     */
    public final Map<String, List<LuaValue>> luaEventCallbacks = new HashMap<>();

    @TagField(value = "luaTagField", mapper = LuaSerialization.LuaMapper.class)
    private Map<String, LuaValue> tagFields = new HashMap<>();

    protected final Map<Long, List<Runnable>> schedule = new HashMap<>();

    /**
     * <h2>Overrides</h2>
     */

    @Override
    public void onTick() {
        super.onTick();

        if (getWorld().isClient || Config.ConfigPerformance.disableLuaScript || getDefinition().scripts == null) {
            return;
        }

        loadLuaEnv();

        if (!schedule.isEmpty()) {
            List<Runnable> due = schedule.remove((long) getTickCount());
            if (due != null) {
                for (Runnable runnable : due) {
                    runnable.run();
                }
            }
        }

        triggerEvent("onTick");
    }

    private void loadLuaEnv() {
        if (environment != null) {
            return;
        }

        environment = JsePlatform.standardGlobals();
        environment.load(new DebugModule());
        environment.load(new EventModule(this));
        environment.load(new IRModule(this));
        environment.load(new MarkupModule());
        environment.load(new StockDebugModule(this));
        environment.load(new WorldModule(this.getWorld()));
        environment.load(new Global());
        environment.load(new VectorModule());

        // Load compiled scripts
        LuaValue preload = environment.get("package").get("preload");
        LuaValue entry = null;
        for (EntityRollingStockDefinition.ScriptDefinition def : getDefinition().scripts) {
            LuaValue chunk;
            try {
                chunk = environment.loader.load(def.compiledScript, def.name, environment);
            } catch (Exception e) {
                ModCore.catching(e, "An error occurred while binding script %s", def.name);
                continue;
            }

            switch (def.type) {
                case MAIN -> entry = chunk;
                case MODULE -> preload.set(def.name.replace(".lua", ""), chunk);
            }
        }
        if (entry != null) {
            entry.call();
        }
    }

    @Override
    public Map<String, List<LuaValue>> getLuaEventCallbacks() {
        return luaEventCallbacks;
    }

    @Override
    public void handleKeyPress(Player source, KeyTypes key, boolean disableIndependentThrottle) {
        boolean hasPermission = switch (key) {
            case INDEPENDENT_BRAKE_UP, INDEPENDENT_BRAKE_DOWN, INDEPENDENT_BRAKE_ZERO ->
                    source.hasPermission(Permissions.BRAKE_CONTROL);
            default -> source.hasPermission(Permissions.LOCOMOTIVE_CONTROL);
        };
        if (getWorld().isServer) {
            triggerEvent("onKeyPress", LuaValue.valueOf(key.toString()), LuaValue.valueOf(hasPermission));
        }

        super.handleKeyPress(source, key, disableIndependentThrottle);
    }

    public class Global implements LuaModule {

        @Override
        public String name() {
            return "_G";
        }

        @LuaFunction
        public LuaValue getName() {
            return LuaValue.valueOf(getDefinition().name());
        }

        @Override
        public void init(LuaTable self, LuaValue name, LuaValue env) {
            LuaTable t = new LuaTable();
            LuaTable meta = new LuaTable();

            meta.set(LuaValue.INDEX, new TwoArgFunction((s, key) -> {
                LuaValue v = tagFields.get(key.tojstring());
                return v != null ? v : LuaValue.NIL;
            }));

            meta.set(LuaValue.NEWINDEX, new ThreeArgFunction((s, key, value) -> {
                tagFields.put(key.tojstring(), value);
                return LuaValue.NIL;
            }));

            t.setmetatable(meta);
            env.set("_TagField", t);
        }

        @LuaFunction
        public Varargs print(Varargs str) {
            List<String> args = new ArrayList<>();
            for (int i = 1; i <= str.narg(); i++) {
                args.add(str.arg(i).tojstring());
            }

            String formatedArgs = String.join("    ", args);
            ModCore.info("[Lua, %s] %s", getDefinition().name(), formatedArgs);
            return LuaValue.NIL;
        }

        @LuaFunction
        public LuaValue wait(LuaValue sec, LuaValue func) {
            float seconds = sec.tofloat();
            int delayTicks = Math.max(1, Math.round(seconds * 20));
            long fireAt = (long) getTickCount() + delayTicks;

            Runnable runnable = () -> {
                try {
                    func.call();
                } catch (Exception e) {
                    ModCore.error("[Lua] Error while executing scheduled wait function: " + e.getMessage());
                }
            };

            schedule.computeIfAbsent(fireAt, _ -> new ArrayList<>(2)).add(runnable);

            return LuaValue.NIL;
        }
    }
}
