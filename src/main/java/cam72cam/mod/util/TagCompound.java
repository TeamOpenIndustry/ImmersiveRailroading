package cam72cam.mod.util;

import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;
import net.minecraft.nbt.NBTTagCompound;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TagCompound {
    public final NBTTagCompound internal;

    public TagCompound(NBTTagCompound data) {
        this.internal = data;
    }

    public TagCompound() {
        this(new NBTTagCompound());
    }
    public boolean hasKey(String key) {
        return internal.hasKey(key);
    }

    public boolean getBoolean(String key) {
        return internal.getBoolean(key);
    }
    public void setBoolean(String key, boolean value) {
        internal.setBoolean(key, value);
    }

    public byte getByte(String key) {
        return internal.getByte(key);
    }
    public void setByte(String key, byte value) {
        internal.setByte(key, value);
    }

    public int getInteger(String key) {
        return internal.getInteger(key);
    }
    public void setInteger(String key, int value) {
        internal.setInteger(key, value);
    }

    public long getLong(String key) {
        return internal.getLong(key);
    }
    public void setLong(String key, long value) {
        internal.setLong(key, value);
    }

    public float getFloat(String key) {
        return internal.getFloat(key);
    }
    public void setFloat(String key, float value) {
        internal.setFloat(key, value);
    }

    public double getDouble(String key) {
        return internal.getDouble(key);
    }
    public void setDouble(String key, double value) {
        internal.setDouble(key, value);
    }

    public String getString(String key) {
        return internal.getString(key);
    }
    public void setString(String key, String value) {
        if (value == null) {
            internal.removeTag(key);
        } else {
            internal.setString(key, value);
        }
    }

    public UUID getUUID(String key) {
        if (!internal.hasKey(key)) {
            return null;
        }
        return UUID.fromString(getString(key));
    }
    public void setUUID(String key, UUID value) {
        internal.removeTag(key);
        if (value != null) {
            setString(key, value.toString());
        }
    }

    public Vec3i getVec3i(String key) {
        NBTTagCompound tag = internal.getCompoundTag(key);
        return new Vec3i(tag.getInteger("X"), tag.getInteger("Y"), tag.getInteger("Z"));
    }
    public void setVec3i(String key, Vec3i pos) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("X", pos.x);
        tag.setInteger("Y", pos.y);
        tag.setInteger("Z", pos.z);
        internal.setTag(key, tag);
    }

    public Vec3d getVec3d(String key) {
        NBTTagCompound nbt = internal.getCompoundTag(key);
        return new Vec3d(nbt.getDouble("x"),nbt.getDouble("y"),nbt.getDouble("z"));
    }
    public void setVec3d(String key, Vec3d value) {
        NBTTagCompound nbt = new NBTTagCompound();
        if (value != null) {
            nbt.setDouble("x", value.x);
            nbt.setDouble("y", value.y);
            nbt.setDouble("z", value.z);
        }
        internal.setTag(key, nbt);
    }

    public cam72cam.mod.entity.Entity getEntity(String key, World world) {
        return getEntity(key, world, cam72cam.mod.entity.Entity.class);
    }
    public <T extends cam72cam.mod.entity.Entity> T getEntity(String key, World world, Class<T> cls) {
        NBTTagCompound data = internal.getCompoundTag(key);
        UUID id = data.getUniqueId("id");
        int dim = data.getInteger("world");
        world = World.get(dim, world.isClient);
        if (world == null) {
            return null;
        }
        return world.getEntity(id, cls);
    }
    public void setEntity(String key, cam72cam.mod.entity.Entity entity) {
        NBTTagCompound data = new NBTTagCompound();
        data.setUniqueId("id", entity.internal.getUniqueID());
        data.setInteger("world", entity.internal.world.provider.getDimension());
        internal.setTag(key, data);
    }

    public <T extends Enum> T getEnum(String key, Class<T> cls) {
        return cls.getEnumConstants()[internal.getInteger(key)];
    }
    public void setEnum(String key, Enum value) {
        internal.setInteger(key, value.ordinal());
    }

    public void setEnumList(String key, List<? extends Enum> items) {
        internal.setIntArray(key, items.stream().map(Enum::ordinal).mapToInt(i -> i).toArray());
    }
    public <T extends Enum> List<T> getEnumList(String key, Class<T> cls) {
        return Arrays.stream(internal.getIntArray(key)).mapToObj((int i) -> cls.getEnumConstants()[i]).collect(Collectors.toList());
    }

    public TagCompound get(String key) {
        return new TagCompound(internal.getCompoundTag(key));
    }
    public void set(String key, TagCompound value) {
        internal.setTag(key, value.internal);
    }

    public void remove(String key) {
        internal.removeTag(key);
    }

    public <T> List<T> getList(String key, Function<TagCompound, T> decoder) {
        List<T> list = new ArrayList<>();
        NBTTagCompound data = internal.getCompoundTag(key);
        for (int i = 0; i < data.getInteger("count"); i++) {
            list.add(decoder.apply(new TagCompound(data.getCompoundTag(i + ""))));
        }
        return list;
    }
    public <T> void setList(String key, List<T> list, Function<T, TagCompound> encoder) {
        NBTTagCompound data = new NBTTagCompound();
        data.setInteger("count", list.size());
        for (int i = 0; i < list.size(); i++) {
            data.setTag(i + "", encoder.apply(list.get(i)).internal);
        }
        internal.setTag(key, data);
    }

    public <K, V> Map<K, V> getMap(String key, Function<String, K> keyFn, Function<TagCompound, V> valFn) {
        Map<K, V> map = new HashMap<>();
        NBTTagCompound data = internal.getCompoundTag(key);
        for (String item : data.getKeySet()) {
            map.put(keyFn.apply(item), valFn.apply(new TagCompound(data.getCompoundTag(item))));
        }
        return map;
    }
    public <K, V> void setMap(String key, Map<K, V> map, Function<K, String> keyFn, Function<V, TagCompound> valFn) {
        NBTTagCompound data = new NBTTagCompound();

        for(K item : map.keySet()) {
            data.setTag(keyFn.apply(item), valFn.apply(map.get(item)).internal);
        }

        internal.setTag(key, data);
    }

    public ItemStack getStack(String key) {
        return new ItemStack(new TagCompound(internal.getCompoundTag(key)));
    }
    public void setStack(String key, ItemStack stack) {
        internal.setTag(key, stack.toTag().internal);
    }

    public String toString() {
        return internal.toString();
    }

    public void setWorld(String key, World world) {
        setInteger(key, world.internal.provider.getDimension());
    }

    public World getWorld(String key, boolean isClient) {
        return World.get(getInteger(key), isClient);
    }

    public <T extends BlockEntity> void setTile(String key, T preview) {
        TagCompound ted = new TagCompound();
        ted.setWorld("world", preview.world);

        TagCompound data = new TagCompound();
        preview.internal.writeToNBT(data.internal);
        ted.set("data", data);

        set(key, ted);
    }

    public <T extends BlockEntity> T getTile(String key, boolean isClient) {
        TagCompound ted = get(key);
        World world = ted.getWorld("world", isClient);

        //TODO pull logic in here to avoid crash
        net.minecraft.tileentity.TileEntity te = net.minecraft.tileentity.TileEntity.create(world.internal, ted.get("data").internal);
        assert te instanceof TileEntity;
        return (T) ((TileEntity)te).instance();
    }
}
