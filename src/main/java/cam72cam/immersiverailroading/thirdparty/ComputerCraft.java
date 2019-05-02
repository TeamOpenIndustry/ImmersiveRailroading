package cam72cam.immersiverailroading.thirdparty;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.Augment;
import cam72cam.immersiverailroading.tile.TileRailBase;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;

public class ComputerCraft {
    public static void init() {
        ComputerCraftAPI.registerPeripheralProvider(new IPeripheralProvider() {
            @Nullable
            @Override
            public IPeripheral getPeripheral(@Nonnull World world, @Nonnull BlockPos blockPos, @Nonnull EnumFacing enumFacing) {
                TileRailBase rail = TileRailBase.get(world, blockPos);
                if (rail != null) {
                    if (rail.getAugment() == Augment.DETECTOR) {
                        return new DetectorPeripheral(world, blockPos);
                    }
                    if (rail.getAugment() == Augment.LOCO_CONTROL) {
                        return new LocoControlPeripheral(world, blockPos);
                    }
                }
                return null;
            }
        });
    }

    @FunctionalInterface
    private interface APICall {
        Object[] apply(CommonAPI api, Object[] params) throws LuaException;
    }

    private static abstract class BasePeripheral implements IPeripheral {
        private final World world;
        private final BlockPos pos;
        private final String[] fnNames;
        private final APICall[] fnImpls;

        public BasePeripheral(World world, BlockPos blockPos, LinkedHashMap<String, APICall> methods) {
            this.world = world;
            this.pos = blockPos;
            this.fnNames = methods.keySet().toArray(new String[0]);
            this.fnImpls = methods.values().toArray(new APICall[0]);
        }

        @Nonnull
        @Override
        public String[] getMethodNames() {
            return fnNames;
        }

        @Nullable
        @Override
        public Object[] callMethod(@Nonnull IComputerAccess iComputerAccess, @Nonnull ILuaContext iLuaContext, int i, @Nonnull Object[] objects) throws LuaException, InterruptedException {
            try {
                CommonAPI api = CommonAPI.create(world, pos);
                if (api != null && i < fnImpls.length) {
                    return fnImpls[i].apply(api, objects);
                }
            } catch (Exception ex) {
                ImmersiveRailroading.catching(ex);
            }
            return null;
        }

        @Override
        public boolean equals(@Nullable IPeripheral iPeripheral) {
            return iPeripheral == this;
        }
    }

    private static Object getObjParam(Object[] params, int id, String name) throws LuaException {
        if (params.length > id) {
            return params[id];
        }
        throw new LuaException("Required parameter \"" + name +"\"");
    }

    private static double getDoubleParam(Object[] params, int id, String name) throws LuaException {
        Object obj = getObjParam(params, id, name);
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException ex) {
            throw new LuaException("Required parameter \"" + name +"\" is not a number");
        }
    }

    private static class DetectorPeripheral extends BasePeripheral {
        private static LinkedHashMap<String, APICall> methods = new LinkedHashMap<>();
        static {
            methods.put("info", (CommonAPI api, Object[] params) -> new Object[]{api.info()});
            methods.put("consist", (CommonAPI api, Object[] params) -> new Object[]{api.consist(false)});
            methods.put("getTag", (CommonAPI api, Object[] params) -> new Object[]{api.getTag()});
            methods.put("setTag", (CommonAPI api, Object[] params) -> {
                api.setTag(getObjParam(params, 0, "tag").toString());
                return null;
            });
        }

        public DetectorPeripheral(World world, BlockPos blockPos) {
            super(world, blockPos, methods);
        }

        @Nonnull
        @Override
        public String getType() {
            return "ir_augment_detector";
        }
    }

    private static class LocoControlPeripheral extends BasePeripheral {
        private static LinkedHashMap<String, APICall> methods = new LinkedHashMap<>();
        static {
            methods.putAll(DetectorPeripheral.methods);
            methods.put("setThrottle", (CommonAPI api, Object[] params) -> {
                api.setThrottle(getDoubleParam(params, 0, "throttle"));
                return null;
            });
            methods.put("setBrake", (CommonAPI api, Object[] params) -> {
                api.setAirBrake(getDoubleParam(params, 0, "brake"));
                return null;
            });
            methods.put("setHorn", (CommonAPI api, Object[] params) -> {
                api.setHorn((int) getDoubleParam(params, 0, "horn"));
                return null;
            });
            methods.put("setBell", (CommonAPI api, Object[] params) -> {
                api.setBell((int) getDoubleParam(params, 0, "bell"));
                return null;
            });
        }

        public LocoControlPeripheral(World world, BlockPos blockPos) {
            super(world, blockPos, methods);
        }

        @Nonnull
        @Override
        public String getType() {
            return "ir_augment_control";
        }
    }
}
