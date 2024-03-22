package com.goldenfield192.ire.util;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapper;
import com.goldenfield192.ire.serializer.MapVec3iBooleanMapper;

import java.util.HashMap;

public class ConnectionStorage {
    private boolean isFirst;
    private Vec3d relativePos;

    public boolean isFirst() {
        return isFirst;
    }

    public void setFirst(boolean first) {
        isFirst = first;
    }

    public Vec3d getRelativePos() {
        return relativePos;
    }

    public void setRelativePos(Vec3d relativePos) {
        this.relativePos = relativePos;
    }

    public ConnectionStorage(boolean isFirst, Vec3d relativePos) {
        this.isFirst = isFirst;
        this.relativePos = relativePos;
    }
}
