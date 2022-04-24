package cam72cam.immersiverailroading.physics.simulation;

import java.util.ArrayList;
import java.util.List;

public class Simulation {
    List<RigidBodyBox> bodies = new ArrayList<>();
    static float tickInSeconds = 1f/20l;

    void run() {
        for (RigidBodyBox body : bodies) {
            body.currentState().computeForces();
            body.currentState().integrate(null, tickInSeconds);
        }
    }
}
