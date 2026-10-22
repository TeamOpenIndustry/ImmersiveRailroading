package cam72cam.immersiverailroading.script.modules;

import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;
import de.poizzy.luaj.LuaTable;
import de.poizzy.luaj.LuaValue;
import de.poizzy.luaj.annotations.LuaFunction;
import de.poizzy.luaj.lib.LuaModule;

public class WorldModule implements LuaModule {
    private final World world;

    @Override
    public String name() {
        return "World";
    }

    public WorldModule(World world) {
        this.world = world;
    }

    @LuaFunction
    public LuaValue isRainingAt(LuaValue pos) {
        return LuaValue.valueOf(world.isRaining(new Vec3i(VectorModule.convertToJava(pos))));
    }

    @LuaFunction
    public LuaValue isSnowingAt(LuaValue pos) {
        return LuaValue.valueOf(world.isSnowing(new Vec3i(VectorModule.convertToJava(pos))));
    }

    @LuaFunction
    public LuaValue getTemperatureCelsius(LuaValue pos) {
        return LuaValue.valueOf(world.getTemperature(new Vec3i(VectorModule.convertToJava(pos))));
    }

    @LuaFunction
    public LuaValue getBiomeTemperature(LuaValue pos) {
        float celsius = world.getTemperature(new Vec3i(VectorModule.convertToJava(pos)));
        float mcTemp = (celsius - 7.0879687222f) / 13.6484805403f;
        return LuaValue.valueOf(mcTemp);
    }

    @LuaFunction
    public LuaValue getSnowLevelAt(LuaValue pos) {
        return LuaValue.valueOf(world.getSnowLevel(new Vec3i(VectorModule.convertToJava(pos))));
    }

    @LuaFunction
    public LuaValue getBlockLightLevelAt(LuaValue pos) {
        return LuaValue.valueOf(world.getBlockLightLevel(new Vec3i(VectorModule.convertToJava(pos))));
    }

    @LuaFunction
    public LuaValue getSkyLightLevelAt(LuaValue pos) {
        return LuaValue.valueOf(world.getSkyLightLevel(new Vec3i(VectorModule.convertToJava(pos))));
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
        return LuaValue.valueOf(world.canSeeSky(new Vec3i(VectorModule.convertToJava(pos))));
    }

    @LuaFunction
    public LuaValue getTicks() {
        return LuaValue.valueOf(world.getTicks());
    }

    @LuaFunction
    public LuaValue getBlock(LuaValue luaPos) {
        Vec3i pos = new Vec3i(VectorModule.convertToJava(luaPos));
        // It will work, hopefully :)
        ItemStack stack = world.getItemStack(pos);

        String name = stack.getDisplayName();

        return LuaValue.valueOf(name);
    }
}
