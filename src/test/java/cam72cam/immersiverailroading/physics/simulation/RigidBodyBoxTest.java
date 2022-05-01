package cam72cam.immersiverailroading.physics.simulation;

import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector3f;

import static org.junit.jupiter.api.Assertions.*;

class RigidBodyBoxTest {

    public void testAngles() {
        RigidBodyBox rbb = new RigidBodyBox(1, 1, 1, 1);
        int total = 0;
        int broken = 0;
        float epsilon = 0.001f;
        for (int yaw = -18; yaw <= 18; yaw+=1) {
            for (int pitch = -18; pitch <= 18; pitch+=1) {
                for (int roll = -18; roll <= 18; roll+=1) {
                    total += 1;
                    rbb.currentState().setOrientation(yaw, pitch, roll);
                    Vector3f out = rbb.currentState().getOrientation();
                    if (Math.abs(out.x) < epsilon && Math.abs(out.y) < epsilon && Math.abs(out.z) < epsilon) {
                        continue;
                    }
                    System.out.println(String.format(
                            "Mismatch %s %s, %s %s, %s %s",
                            yaw, out.x,
                            pitch, out.y,
                            roll, out.z
                    ));
                    broken += 1;
                }
            }
        }
        assertEquals(broken, 0, String.format("%s / %s broken", broken, total));
    }

    public void testDelta() {
        RigidBodyBox rbb = new RigidBodyBox(1, 1, 1, 1);
        int total = 0;
        int broken = 0;
        float epsilon = 2.0001f; // Why is this so high for y???
        for (int yaw = -180; yaw <= 180; yaw+=90) {
            for (int pitch = -180; pitch <= 180; pitch+=90) {
                for (int roll = -180; roll <= 180; roll+=90) {
                    for (int i = -10; i <= 10; i++) {
                        int x = 10;
                        int y = i;
                        int z = -10;


                        total += 1;
                        rbb.currentState().setOrientation(yaw, pitch, roll);
                        rbb.addRotation(x, y, z);
                        Vector3f out = rbb.currentState().getOrientation();
                        if (Math.abs(out.x - x) < epsilon && Math.abs(out.y - y) < epsilon && Math.abs(out.z - z) < epsilon) {
                            continue;
                        }
                        System.out.println(String.format(
                                "Mismatch %s: %s %s, %s %s, %s %s",
                                i,
                                yaw, out.x,
                                pitch, out.y,
                                roll, out.z
                        ));
                        broken += 1;
                    }
                }
            }
        }
        assertEquals(broken, 0, String.format("%s / %s broken", broken, total));
    }
}