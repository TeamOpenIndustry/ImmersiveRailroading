package cam72cam.immersiverailroading.script.modules;

import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.LuaFunction;
import org.luaj.vm2.lib.LuaModule;

public class WorldModule implements LuaModule {
    private final World world;

    @Override
    public String name() {
        return "World";
    }

    @Override
    public void init(LuaTable self, LuaValue name, LuaValue env) {

    }

    public WorldModule(World world) {
        this.world = world;
    }

    @LuaFunction
    public LuaValue isRainingAt(LuaValue pos) {
        return LuaValue.valueOf(world.isRaining(ScriptVectorUtil.convertToVec3i(pos)));
    }

    @LuaFunction
    public LuaValue isSnowingAt(LuaValue pos) {
        return LuaValue.valueOf(world.isSnowing(ScriptVectorUtil.convertToVec3i(pos)));
    }

    @LuaFunction
    public LuaValue getTemperatureCelsius(LuaValue pos) {
        return LuaValue.valueOf(world.getTemperature(ScriptVectorUtil.convertToVec3i(pos)));
    }

    @LuaFunction
    public LuaValue getBiomeTemperature(LuaValue pos) {
        float celsius = world.getTemperature(ScriptVectorUtil.convertToVec3i(pos));
        float mcTemp = (celsius - 7.0879687222f) / 13.6484805403f;
        return LuaValue.valueOf(mcTemp);
    }

    @LuaFunction
    public LuaValue getSnowLevelAt(LuaValue pos) {
        return LuaValue.valueOf(world.getSnowLevel(ScriptVectorUtil.convertToVec3i(pos)));
    }

    @LuaFunction
    public LuaValue getBlockLightLevelAt(LuaValue pos) {
        return LuaValue.valueOf(world.getBlockLightLevel(ScriptVectorUtil.convertToVec3i(pos)));
    }

    @LuaFunction
    public LuaValue getSkyLightLevelAt(LuaValue pos) {
        return LuaValue.valueOf(world.getSkyLightLevel(ScriptVectorUtil.convertToVec3i(pos)));
    }

    @LuaFunction
    public LuaValue getDimension() {
        return LuaValue.valueOf(world.getId());
    }

    @LuaFunction
    public LuaValue getTime() {
        return LuaValue.valueOf(world.getTime());
    }

    @LuaFunction
    public LuaValue isSkyVisible(LuaValue pos) {
        return LuaValue.valueOf(world.canSeeSky(ScriptVectorUtil.convertToVec3i(pos)));
    }

    @LuaFunction
    public LuaValue getTicks() {
        return LuaValue.valueOf(world.getTicks());
    }

    @LuaFunction
    public LuaValue getBlock(LuaValue luaPos) {
        Vec3i pos = ScriptVectorUtil.convertToVec3i(luaPos);
        // It will work, hopefully :)
        ItemStack stack = world.getItemStack(pos);

        String name = stack.getDisplayName();

        return LuaValue.valueOf(name);
    }
}
