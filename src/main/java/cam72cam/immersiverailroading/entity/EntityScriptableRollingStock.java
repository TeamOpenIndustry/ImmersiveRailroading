package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.script.*;
import cam72cam.immersiverailroading.script.library.ILuaEvent;
import cam72cam.immersiverailroading.script.library.LuaSerialization;
import cam72cam.immersiverailroading.script.modules.*;
import cam72cam.mod.ModCore;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.serialization.TagField;
import org.luaj.vm2.LuaValue;

import java.util.*;
import java.util.stream.Collectors;

public abstract class EntityScriptableRollingStock extends EntityCoupleableRollingStock implements ILuaEvent {
    private LuaContext context;
    private Boolean hasOwnScript;

    /**
     * Used by {@link EventModule}
     */
    public final Map<String, List<LuaValue>> luaEventCallbacks = new HashMap<>();

    /**
     * Used by {@link LuaContext}
     */
    @TagSync
    @TagField(value = "luaTagField", mapper = LuaSerialization.LuaMapper.class)
    private Map<String, LuaValue> tagFields = new HashMap<>();

    protected final Map<Long, List<Runnable>> schedule = new HashMap<>();

    /**
     * <h2>Overrides</h2>
     */

    @Override
    public void onTick() {
        super.onTick();

        if (getWorld().isClient || Config.ConfigPerformance.disableLuaScript || !hasOwnScript()) {
            return;
        }

        ensureContext();

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

    private void registerModules() {
        context.registerLibrary(new ScriptVectorUtil.VectorLibrary());
        context.registerLibrary(new MarkupModule());

        context.registerLibrary(new IRModule(this));
        context.registerLibrary(new WorldModule(getWorld()));
        context.registerLibrary(new StockDebugModule(this));
        context.registerLibrary(new EventModule(this));
    }

    private void loadLuaScript() {
        Identifier script = getDefinition().script;

        List<String> modules = getDefinition().addScripts;
        if (modules != null && !modules.isEmpty() && script != null) {
            context.loadModules(modules, script);
        }

        if (script != null && script.canLoad()) {
            context.loadScript(script);
        }
    }

    private boolean hasOwnScript() {
        if (hasOwnScript == null) {
            EntityRollingStockDefinition def = getDefinition();
            if (def == null) {
                return false;
            }
            hasOwnScript = def.script != null && def.script.canLoad();
        }
        return hasOwnScript;
    }

    private void ensureContext() {
        if (context != null) {
            return;
        }
        context = LuaContext.create(this);
        registerModules();
        loadLuaScript();
        context.refreshSerialization(tagFields);
    }

    @LuaFunction(module = "")
    private LuaValue getName() {
        return LuaValue.valueOf(getDefinition().name());
    }

    @LuaFunction(module = "")
    public void print(LuaValue... str) {
        List<String> args = Arrays.stream(str).map(LuaValue::tojstring).collect(Collectors.toList());
        String formatedArgs = String.join("    ", args);
        ModCore.info("[Lua, %s] %s", getDefinition().name(), formatedArgs);
    }


    @LuaFunction(module = "Utils")
    public void wait(LuaValue sec, LuaValue func) {
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
    }
}
