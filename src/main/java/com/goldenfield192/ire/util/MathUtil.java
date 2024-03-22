package com.goldenfield192.ire.util;

import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;

import static java.lang.Math.floor;

public class MathUtil {
    //根据网架的相对坐标算xz旋转角度
    public static double vecToDegreeXZ(Vec3d vec){
        //先特判
        if(vec.x == 0 && vec.z == 0){
            return 0;
        } else if (vec.z == 0) {
            return vec.x > 0 ? 90 : -90;
        } else if (vec.x == 0) {
            return vec.z > 0 ? 0 : 180;
        } else {
            double tangent = Math.abs((double) vec.x/vec.z);
            if(vec.x < 0 && vec.z > 0){//4
                return - Math.toDegrees(Math.atan(tangent));
            }else if(vec.x > 0 && vec.z > 0){//1
                return Math.toDegrees(Math.atan(tangent));
            } else if(vec.x > 0){//2
                return 180 + Math.toDegrees(- Math.atan(tangent));
            } else {//3
                return 180 - Math.toDegrees(- Math.atan(tangent));
            }
        }
    }
    public static double vecToDegreeY(Vec3d vec){
        return Math.toDegrees(Math.atan(vec.y/vecToLengthXZ(vec)));
    }
    //暴力勾股
    public static double vecToLengthXZ(Vec3i vec){
        return Math.sqrt(vec.x * vec.x + vec.z * vec.z);
    }
    public static double vecToLengthXZ(Vec3d vec){
        return Math.sqrt(vec.x * vec.x + vec.z * vec.z);
    }
    public static double vecToLength(Vec3i vec){
        return Math.sqrt(vec.x * vec.x + vec.z * vec.z);
    }
    public static Vec3d toVec3d(Vec3i vec3i){
        return new Vec3d(vec3i.x,vec3i.y,vec3i.z);
    }
    public static Vec3i toVec3i(Vec3d vec3d){
        return new Vec3i(floor(vec3d.x),floor(vec3d.y),floor(vec3d.z));
    }
}
