package cam72cam.immersiverailroading.script.modules;

import cam72cam.immersiverailroading.script.LuaFunction;
import cam72cam.immersiverailroading.script.LuaModule;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import util.Matrix4;

public class ScriptVectorUtil {
    public static class VectorLibrary implements LuaModule {
        public VectorLibrary() {}

        @LuaFunction(module = "VecUtil")
        private LuaValue add(LuaValue vec1, LuaValue vec2) {
            vec1.set("x", vec1.get("x").add(vec2.get("x")));
            vec1.set("y", vec1.get("y").add(vec2.get("y")));
            vec1.set("z", vec1.get("z").add(vec2.get("z")));
            return vec1;
        }

        @LuaFunction(module = "VecUtil")
        private LuaValue scale(LuaValue vec, LuaValue mul) {
            vec.set("x", vec.get("x").mul(mul));
            vec.set("y", vec.get("y").mul(mul));
            vec.set("z", vec.get("z").mul(mul));
            return vec;
        }

        @LuaFunction(module = "VecUtil")
        private LuaValue length(LuaValue vec) {
            double x = vec.get("x").todouble();
            double y = vec.get("y").todouble();
            double z = vec.get("z").todouble();
            return LuaValue.valueOf(Math.sqrt(x * x + y * y + z * z));
        }

        @LuaFunction(module = "VecUtil")
        private LuaValue lengthSquared(LuaValue vec) {
            double x = vec.get("x").todouble();
            double y = vec.get("y").todouble();
            double z = vec.get("z").todouble();
            return LuaValue.valueOf(x * x + y * y + z * z);
        }

        @LuaFunction(module = "VecUtil")
        private LuaValue applyMatrix(LuaValue matrix, LuaValue vec) {
            Matrix4 matrix4 = convertToMatrix4((LuaTable) matrix);
            Vec3d vec3d = convertToVec3d(vec);
            return constructVec3Table(matrix4.apply(vec3d));
        }
    }

    public static LuaTable constructVec3Table(Vec3i vec3i){
        return constructVec3Table(vec3i.x, vec3i.y, vec3i.z);
    }

    public static LuaTable constructVec3Table(Vec3d vec3d){
        return constructVec3Table(vec3d.x, vec3d.y, vec3d.z);
    }

    public static LuaTable constructVec3Table(LuaValue x, LuaValue y, LuaValue z){
        return constructVec3Table(x.todouble(), y.todouble(), z.todouble());
    }

    public static LuaTable constructVec3Table(double x, double y, double z){
        LuaTable vector3d = new LuaTable();
        vector3d.set("x", x);
        vector3d.set("y", y);
        vector3d.set("z", z);
        return vector3d;
    }

    public static LuaTable constructMatrix4Table(Matrix4 matrix4){
        LuaTable matrix = new LuaTable();
        matrix.set("m00", matrix4.m00);
        matrix.set("m01", matrix4.m01);
        matrix.set("m02", matrix4.m02);
        matrix.set("m03", matrix4.m03);
        matrix.set("m10", matrix4.m10);
        matrix.set("m11", matrix4.m11);
        matrix.set("m12", matrix4.m12);
        matrix.set("m13", matrix4.m13);
        matrix.set("m20", matrix4.m20);
        matrix.set("m21", matrix4.m21);
        matrix.set("m22", matrix4.m22);
        matrix.set("m23", matrix4.m23);
        matrix.set("m30", matrix4.m30);
        matrix.set("m31", matrix4.m31);
        matrix.set("m32", matrix4.m32);
        matrix.set("m33", matrix4.m33);
        return matrix;
    }

    public static Vec3i convertToVec3i(LuaValue vector){
        return new Vec3i(vector.get("x").toint(), vector.get("y").toint(), vector.get("z").toint());
    }

    public static Vec3d convertToVec3d(LuaValue vector){
        return new Vec3d(vector.get("x").todouble(), vector.get("y").todouble(), vector.get("z").todouble());
    }

    public static Matrix4 convertToMatrix4(LuaTable matrix){
        return new Matrix4(matrix.get("m00").todouble(), matrix.get("m01").todouble(), matrix.get("m02").todouble(), matrix.get("m03").todouble(),
                matrix.get("m10").todouble(), matrix.get("m11").todouble(), matrix.get("m12").todouble(), matrix.get("m13").todouble(),
                matrix.get("m20").todouble(), matrix.get("m21").todouble(), matrix.get("m22").todouble(), matrix.get("m23").todouble(),
                matrix.get("m30").todouble(), matrix.get("m31").todouble(), matrix.get("m32").todouble(), matrix.get("m33").todouble());
    }
}
