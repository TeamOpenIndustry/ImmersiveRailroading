package cam72cam.immersiverailroading.physics;

import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.physics.simulation.RigidBodyBox;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.world.World;
import org.lwjgl.util.vector.Vector3f;

import java.util.*;

public class StockSimulator {
    public static void register() {
        World.onTick(StockSimulator::simulate);
    }

    private static void simulate(World world) {
        long time = System.currentTimeMillis();

        RigidBodyBox.airPositions.clear();

        List<Vec3i> blocksBroken = new ArrayList<>();

        // Single step for now
        List<EntityCoupleableRollingStock> entities = world.getEntities(EntityCoupleableRollingStock.class);

        List<RigidBodyBox> bodies = new ArrayList<>();

        for (EntityCoupleableRollingStock entity : entities) {
            if (entity.rbb == null) {
                EntityRollingStockDefinition def = entity.getDefinition();
                entity.rbb = new RigidBodyBox(
                        (float) def.getLength(entity.gauge), (float) def.getWidth(entity.gauge), (float) def.getHeight(entity.gauge),
                        (float) entity.getWeight()
                );

                entity.rbb.previousState().setPosition(entity.getPosition().add(0, def.getHeight(entity.gauge)/2, 0));
                entity.rbb.previousState().setOrientation((entity.roll % 360), 180 + (entity.getRotationYaw() % 360) - 90, (entity.getRotationPitch() % 360));
                entity.rbb.setRestitution(0.3f);
            } else {
                entity.rbb.setMassKg((float) entity.getWeight());
            }

            if (entity instanceof Locomotive) {
                entity.rbb.previousState().addInternalLinearForce((float)((Locomotive) entity).getTractiveEffortNewtons(Speed.fromMetric(30)) * 4, new Vec3d(-1, 0, 0));
            }

            bodies.add(entity.rbb);
        }

        float currentTime = 0f;
        float targetTime = 1f / 20f;
        float deltaTime = targetTime;
        List<Runnable> collisions = new ArrayList<>();

        while(currentTime < deltaTime) {
            for (RigidBodyBox body : bodies) {
                body.previousState().computeForces();

                //System.out.println(body.previousState().getPosition());
                body.currentState().integrate(body.previousState(), targetTime - currentTime);
                body.currentState().updateVertices();
            }

            for (RigidBodyBox body : bodies) {
                List<Runnable> collision = body.currentState().collideWithWorld(world);
                collisions.addAll(collision);
            }
            for (int j = 0; j < bodies.size(); j++) {
                for (int k = 0; k < bodies.size(); k++) {
                    if (j == k) {
                        continue;
                    }
                    List<Runnable> collision = bodies.get(j).collideWithOther(bodies.get(k));
                    collisions.addAll(collision);
                }
            }
                    collisions.forEach(Runnable::run);

            currentTime = targetTime;
            targetTime = deltaTime;

            if (!bodies.isEmpty()) {
                //System.out.println("NEXT STATE");
            }
            for (RigidBodyBox body : bodies) {
                body.nextState();
                body.previousState().resetForces();
            }
        }

        if (world.getTicks() % 10 == 0) {
            System.out.println(String.format("Physics took %sms", System.currentTimeMillis() - time));
        }

        for (EntityCoupleableRollingStock entity : entities) {
            entity.setPosition(entity.rbb.previousState().getPosition().subtract(0, entity.getDefinition().getHeight(entity.gauge)/2, 0));

            Vector3f rots = entity.rbb.previousState().getOrientation();
            if (entity.getTickCount() % 20 == 0) {
                System.out.println(String.format("Out Pitch: %s, Yaw %s, Roll: %s",
                        (((rots.x) + 360) % 360),
                        (((rots.y) + 360) % 360),
                        (((rots.z) + 360) % 360)
                ));
            }
            entity.setRoll(rots.x);
            entity.setRotationYaw(180 - rots.y - 90);
            entity.setRotationPitch(rots.z);

            entity.matrix = entity.rbb.previousState().getMatrix().transpose();
            entity.points.clear();;
            for (Vector3f point : entity.rbb.getPoints()) {
                entity.points.add(new Vec3d(point.x, point.y, point.z));
            }
            try {
                entity.sync.setInterval(1);
                entity.sync.send();
            } catch (SerializationException e) {
                e.printStackTrace();
            }
            //TODO other rots
        }


        /*

        // Setup initial states
        List<StockState> states = new ArrayList<>();
        for (EntityCoupleableRollingStock entity : entities) {
            states.add(entity.state == null ? new StockState(entity) : entity.state);
        }

        // Accelerations
        for (StockState state : states) {
            state.calculateIndependentAccelerations();
        }

        // Collisions
        for (StockState state : states) {
            state.calculateStockInteractions(states);
        }

        //Apply
        for (StockState state : states) {
            state.compute().apply();
        }*/

    }
}
