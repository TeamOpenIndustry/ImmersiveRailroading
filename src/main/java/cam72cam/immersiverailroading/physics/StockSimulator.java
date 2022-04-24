package cam72cam.immersiverailroading.physics;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.physics.simulation.RigidBodyBox;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.world.World;
import org.apache.commons.lang3.tuple.Pair;
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
            } else {
                //TODO entity.rbb.setMassKg((float) entity.getWeight());
            }

            entity.rbb.setRestitution(0.3f);

            bodies.add(entity.rbb);

            //entity.setRotationYaw((entity.getRotationYaw()) % 360);
            //entity.setRotationPitch((entity.getRotationPitch()) % 360);
            //entity.roll = (entity.roll) % 360;

            //entity.setRotationYaw(0);
            //entity.setRotationPitch(0);
            //entity.roll = 0;

            //entity.rbb.previousState().setOrientation((entity.roll % 360), 180 + (entity.getRotationYaw() % 360) - 90, (entity.getRotationPitch() % 360));

            if (entity.getTickCount() % 20 == 0) {
                //System.out.println(String.format("In Pitch: %s, Yaw %s, Roll: %s", (int)entity.getRotationPitch(), (int)entity.getRotationYaw(), (int)entity.roll));
            }
        }

        float currentTime = 0f;
        float targetTime = 1f / 20f;
        float deltaTime = targetTime;
        List<Runnable> collisions = new ArrayList<>();

        while(currentTime < deltaTime) {
            boolean isPenetrating = false;
            for (RigidBodyBox body : bodies) {
                body.previousState().computeForces();

                //System.out.println(body.previousState().getPosition());
                body.currentState().integrate(body.previousState(), targetTime - currentTime);
                body.currentState().updateVertices();
            }

            for (RigidBodyBox body : bodies) {
                List<Runnable> collision = body.currentState().collideWithWorld(world);
                if (collision == null) {
                    isPenetrating = true;

                    targetTime = (currentTime + targetTime) / 2;
                    //System.out.println("TIME: " + (targetTime - currentTime));
                    if (Math.abs(targetTime- currentTime) < 0.00001f) {
                        ImmersiveRailroading.warn("PHYSICS LOCK!");
                        deltaTime = 0;
                    }
                    break;
                } else {
                    collisions.addAll(collision);
                }
            }
            if (! isPenetrating) {
                for (int j = 0; j < bodies.size(); j++) {
                    for (int k = 0; k < bodies.size(); k++) {
                        if (j == k) {
                            continue;
                        }
                        List<Runnable> collision = bodies.get(j).collideWithOther(bodies.get(k));
                        collisions.addAll(collision);
                    }
                }
            }
            if (! isPenetrating) {
                // Apply collisions
                for (int i = 0; i < 100; i++) {
                        collisions.forEach(Runnable::run);
                        collisions.clear();
                        for (RigidBodyBox body : bodies) {
                            List<Runnable> collision = body.currentState().collideWithWorld(world);
                            if (collision == null) {
                                ImmersiveRailroading.warn("THIS SHOULD NOT HAPPEN");
                            } else {
                                collisions.addAll(collision);
                            }
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
                        if (collisions.isEmpty()) {
                            break;
                        }
                        //System.out.println("PROCESSING COLLISIONS");
                }

                currentTime = targetTime;
                targetTime = deltaTime;

                if (!bodies.isEmpty()) {
                    //System.out.println("NEXT STATE");
                }
                for (RigidBodyBox body : bodies) {
                    body.nextState();
                }
            }
        }

        //System.out.println(String.format("Physics took %sms", System.currentTimeMillis() - time));

        for (EntityCoupleableRollingStock entity : entities) {
            entity.setPosition(entity.rbb.previousState().getPosition().subtract(0, entity.getDefinition().getHeight(entity.gauge)/2, 0));
            /*
            Vector3f rots = entity.rbb.previousState().getOrientationDelta();
            if (entity.getTickCount() % 20 == 0) {
                System.out.println(String.format("Out Pitch: %s, Yaw %s, Roll: %s",
                        (((rots.x) + 360) % 360),
                        (((rots.y) + 360) % 360),
                        (((rots.z) + 360) % 360)
                ));
            }
            entity.setRoll(entity.roll + rots.x);
            entity.setRotationYaw(entity.getRotationYaw() + rots.y);
            entity.setRotationPitch(entity.getRotationPitch() + rots.z);
             */
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
