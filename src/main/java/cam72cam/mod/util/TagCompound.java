package cam72cam.mod.util;

import cam72cam.mod.math.Vec3i;
import net.minecraft.nbt.NBTTagCompound;

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
        internal.setString(key, value);
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

    public TagCompound get(String key) {
        return new TagCompound(internal.getCompoundTag(key));
    }
    public void set(String key, TagCompound value) {
        internal.setTag(key, value.internal);
    }

    public void remove(String key) {
        internal.removeTag(key);
    }
}
