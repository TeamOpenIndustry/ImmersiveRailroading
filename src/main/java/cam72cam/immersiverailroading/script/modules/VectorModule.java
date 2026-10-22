package cam72cam.immersiverailroading.script.modules;

import cam72cam.mod.math.Vec3d;
import de.poizzy.luaj.LuaValue;
import de.poizzy.luaj.Varargs;
import de.poizzy.luaj.annotations.LuaField;
import de.poizzy.luaj.annotations.LuaFunction;
import de.poizzy.luaj.function.VarArgFunction;
import de.poizzy.luaj.lib.LuaClass;

import static de.poizzy.luaj.LuaValue.*;

public class VectorModule implements LuaClass {
    private VarArgFunction newInstance;
    @LuaField
    public static final String TYPE = "vec";
    @LuaField
    public double x;
    @LuaField
    public double y;
    @LuaField
    public double z;

    @Override
    public void constructor(VarArgFunction newInstance, Varargs args) {
        if (args.narg() != 3) {
            return;
        }
        this.x = args.arg1().todouble();
        this.y = args.arg(2).todouble();
        this.z = args.arg(3).todouble();
        this.newInstance = newInstance;
    }

    @Override
    public String name() {
        return "vec";
    }

    @LuaFunction
    public LuaValue add(LuaValue other) {
        double otherX = other.get("x").todouble();
        double otherY = other.get("y").todouble();
        double otherZ = other.get("z").todouble();
        return newInstance.call(valueOf(this.x + otherX), valueOf(this.y + otherY), valueOf(this.z + otherZ));
    }

    @LuaFunction
    public LuaValue subtract(LuaValue other) {
        double otherX = other.get("x").todouble();
        double otherY = other.get("y").todouble();
        double otherZ = other.get("z").todouble();
        return newInstance.call(valueOf(this.x - otherX), valueOf(this.y - otherY), valueOf(this.z - otherZ));
    }

    @LuaFunction
    public LuaValue lengthSquared() {
        return valueOf(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    @LuaFunction
    public LuaValue length() {
        return valueOf(Math.sqrt(lengthSquared().todouble()));
    }

    @LuaFunction
    public LuaValue normalize() {
        double length = length().todouble();
        return scale(valueOf(1 / length));
    }

    @LuaFunction
    public LuaValue scale(LuaValue scale) {
        double s = scale.checkdouble();
        return newInstance.call(valueOf(this.x * s), valueOf(this.y * s), valueOf(this.z * s));
    }

    @LuaFunction
    public LuaValue min(LuaValue other) {
        double otherX = other.get("x").todouble();
        double otherY = other.get("y").todouble();
        double otherZ = other.get("z").todouble();
        return newInstance.call(valueOf(Math.min(this.x, otherX)), valueOf(Math.min(this.y, otherY)), valueOf(Math.min(this.z, otherZ)));
    }

    @LuaFunction
    public LuaValue max(LuaValue other) {
        double otherX = other.get("x").todouble();
        double otherY = other.get("y").todouble();
        double otherZ = other.get("z").todouble();
        return newInstance.call(valueOf(Math.max(this.x, otherX)), valueOf(Math.max(this.y, otherY)), valueOf(Math.max(this.z, otherZ)));
    }

    @LuaFunction
    public LuaValue dotProduct(LuaValue other) {
        double otherX = other.get("x").todouble();
        double otherY = other.get("y").todouble();
        double otherZ = other.get("z").todouble();
        return valueOf(this.x * otherX + this.y * otherY + this.z * otherZ);
    }

    @LuaFunction
    public LuaValue crossProduct(LuaValue other) {
        double otherX = other.get("x").todouble();
        double otherY = other.get("y").todouble();
        double otherZ = other.get("z").todouble();
        return newInstance.call(valueOf(this.y * otherZ - this.z * otherY), valueOf(this.z * otherX - this.x * otherZ), valueOf(this.x * otherY - this.y * otherX));
    }

    @LuaFunction
    public LuaValue rotateYaw(LuaValue angle) {
        double degrees = angle.checkdouble();
        double rad = Math.toRadians(degrees);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        return newInstance.call(valueOf(cos * this.x + sin * this.z), valueOf(this.y), valueOf(sin * -this.x + cos * this.z));
    }

    @LuaFunction
    public LuaValue equals(LuaValue other) {
        double otherX = other.get("x").todouble();
        double otherY = other.get("y").todouble();
        double otherZ = other.get("z").todouble();
        return valueOf(x == otherX && y == otherY && z == otherZ);
    }

    @LuaFunction(name = "toString")
    public LuaValue toStringLua() {
        return valueOf(String.format("(%s, %s, %s)", this.x, this.y, this.z));
    }

    public static Vec3d convertToJava(LuaValue instance) {
        if (!instance.get("type").tojstring().equals(TYPE)) {
            return Vec3d.ZERO;
        }

        double x = instance.get("x").todouble();
        double y = instance.get("y").todouble();
        double z = instance.get("z").todouble();
        return new Vec3d(x, y, z);
    }

    public static LuaValue convertToLua(Vec3d vec, LuaValue env) {
        LuaValue x = valueOf(vec.x);
        LuaValue y = valueOf(vec.y);
        LuaValue z = valueOf(vec.z);
        LuaValue constructor = env.get("vec").get("new");
        return constructor.call(x, y, z);
    }
}
