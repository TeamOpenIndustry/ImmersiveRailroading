package cam72cam.immersiverailroading.entity.physics;

import cam72cam.immersiverailroading.util.Speed;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.util.DegreeFuncs;

import java.util.*;

public class Consist {
    public static class Particle {
        public SimulationState state;
        // Velocity along axis
        public double velocity;
        public boolean fullyLinked = false;

        public Particle(SimulationState state) {
            this.state = state;
            this.velocity = state.velocity.length() * (DegreeFuncs.delta(VecUtil.toWrongYaw(state.velocity), state.yaw) < 90 ? 1 : -1);

            this.velocity += Speed.fromMetric(
                    state.forcesNewtons() / state.config.massKg
            ).minecraft();

            this.velocity += Math.copySign(
                    Math.min(
                            Speed.fromMetric(state.frictionNewtons() / state.config.massKg).minecraft(),
                            Math.abs(velocity)
                    ), -velocity
            );
        }

        public void applyVelocity() {
            Vec3d currentPos = state.position;
            state.moveAlongTrack(VecUtil.fromWrongYaw(velocity, state.yaw));
            state.velocity = state.position.subtract(currentPos);
            state.calculateCouplerPositions();
        }
    }

    public static class Collision {
        private final Particle particleA;
        private final Particle particleB;

        private final boolean particleACouplerFront;
        private final boolean particleBCouplerFront;
        private final boolean particleAEngaged;
        private final boolean particleBEngaged;

        private final boolean sameDirection;
        private final double maxSlack;

        public static Collision detect(Particle particleA, Particle particleB) {
            if (particleB == null) {
                return null;
            }
            if (particleB.fullyLinked) {
                return null;
            }
            //System.out.println(VecUtil.between(particleA.state.position, particleB.state.position));
            return new Collision(particleA, particleB);
        }

        private Collision(Particle particleA, Particle particleB) {
            this.particleA = particleA;
            this.particleB = particleB;

            particleACouplerFront = particleB.state.config.id.equals(particleA.state.interactingFront);
            particleBCouplerFront = particleA.state.config.id.equals(particleB.state.interactingFront);
            sameDirection = particleACouplerFront != particleBCouplerFront;

            particleAEngaged = particleACouplerFront ?
                    particleA.state.config.couplerEngagedFront :
                    particleA.state.config.couplerEngagedRear;
            particleBEngaged = particleBCouplerFront ?
                    particleB.state.config.couplerEngagedFront :
                    particleB.state.config.couplerEngagedRear;

            maxSlack = 0.1 * particleA.state.config.gauge.scale(); // TODO configurable
        }

        private Vec3d particleACoupler() {
            return particleACouplerFront ? particleA.state.couplerPositionFront : particleA.state.couplerPositionRear;
        }
        private Vec3d particleBCoupler() {
            return particleBCouplerFront ? particleB.state.couplerPositionFront : particleB.state.couplerPositionRear;
        }

        private boolean isTransferringForce(boolean checkAppliedForces) {
            int dir2 = sameDirection ? 1 : -1;
            double velocityA = particleA.velocity;
            double velocityB = particleB.velocity * dir2;

            Vec3d slack = particleACoupler().subtract(particleBCoupler());
            boolean contacting = slack.lengthSquared() >= maxSlack * maxSlack;
            boolean isOverlapping =
                    particleA.state.position.distanceToSquared(particleACoupler()) >
                            particleA.state.position.distanceToSquared(particleBCoupler());

            boolean isPushingTogether = contacting && isOverlapping;
            boolean isPullingApart = contacting && !isOverlapping && (particleAEngaged && particleBEngaged);

            isPushingTogether = isPushingTogether && (!checkAppliedForces || (
                    particleACouplerFront ?
                            // A >>>*< B
                            velocityA > velocityB :
                            // A >*<<< B
                            velocityB > velocityA
            ));
            isPullingApart = isPullingApart && (!checkAppliedForces || (
                    particleACouplerFront ?
                            // A <<<*> B
                            velocityA < velocityB :
                            // A <*>>> B
                            velocityB < velocityA
            ));
            if (velocityA != 0 || velocityB != 0) {
                //System.out.println(String.format("%s - %s : %s push=%s pull=%s", particleA.state.config.id, particleB.state.config.id, contacting, isPushingTogether, isPullingApart));
            }
            return isPushingTogether || isPullingApart;
        }

        public void collide() {
            int dirB = sameDirection ? 1 : -1;
            double velocityA = particleA.velocity;
            double velocityB = particleB.velocity * dirB;
            double massA = particleA.state.config.massKg;
            double massB = particleB.state.config.massKg;

            if (velocityA != velocityB && isTransferringForce(true)) {
                //System.out.println("Transfer Velocity");
                // Transfer velocity
                //System.out.println(String.format("I M1=%s V1=%s M2=%s V2=%s", massA, particleA.velocity, massB, particleB.velocity));

                double restitution = 0.3;
                double newVA = (restitution * massB * (velocityB - velocityA) + massA * velocityA + massB * velocityB) / (massA + massB);
                double newVB = (restitution * massA * (velocityA - velocityB) + massA * velocityA + massB * velocityB) / (massA + massB);

                particleA.velocity = newVA;
                particleB.velocity = newVB * dirB;

                //System.out.println(String.format("O M1=%s V1=%s M2=%s V2=%s", massA, particleA.velocity, massB, particleB.velocity));
            }
        }

        public void fixPositions() {
            if (isTransferringForce(false)) {
                Vec3d slack = particleACoupler().subtract(particleBCoupler());
                if (slack.lengthSquared() > maxSlack * maxSlack) {
                    double slackDist = slack.length() - maxSlack;
                    if (Math.abs(slackDist) > 0.01) {
                        //System.out.println("CORRECT: " + slackDist);
                        particleB.state.moveAlongTrack(slack.normalize().scale(slackDist));
                        //particleB.state.calculateCouplerPositions();
                    }
                }
            }
        }
    }

    public static void iterate(Map<UUID, SimulationState> states) {
        //System.out.println(String.format("Iterate: %s", states.size()));
        List<Particle> ordered = new ArrayList<>();
        Map<UUID, Particle> particles = new HashMap<>();

        for (Map.Entry<UUID, SimulationState> entry : states.entrySet()) {
            particles.put(entry.getKey(), new Particle(entry.getValue()));
        }

        for (Particle particle : particles.values()) {
            if (ordered.contains(particle)) {
                //Already in order
                continue;
            }


            Particle current = particle;
            List<Particle> visited = new ArrayList<>();

            // Iterate to one end of the consist
            Particle prev = current;
            while (!visited.contains(current)) {
                visited.add(current);
                // If we have a Front connection
                if (current.state.interactingFront != null) {
                    // Find the particle for that connection
                    Particle next = particles.get(current.state.interactingFront);

                    // Check if it's not the previous node and it's not already been walked as part of another iteration
                    if (next != null && prev != next && !ordered.contains(next)) {
                        prev = current;
                        current = next;
                        continue;
                    }
                }
                // If we have a Rear connection
                if (current.state.interactingRear != null) {
                    // Find the particle for that connection
                    Particle next = particles.get(current.state.interactingRear);

                    // Check if it's not the previous node and it's not already been walked as part of another iteration
                    if (next != null && prev != next && !ordered.contains(next)) {
                        prev = current;
                        current = next;
                        continue;
                    }
                }
                break;
            }

            ordered.add(current);

            // We are now at the head of the iteration
            prev = current;
            visited.clear();
            while (!visited.contains(current)) {
                visited.add(current);

                // If we have a Front connection
                if (current.state.interactingFront != null) {
                    // Find the particle for that connection
                    Particle next = particles.get(current.state.interactingFront);

                    // Check if it's not the previous node and it's not already been walked as part of another iteration
                    if (next != null && prev != next && !ordered.contains(next)) {
                        ordered.add(next);
                        prev = current;
                        current = next;
                        continue;
                    }
                }
                // If we have a Rear connection
                if (current.state.interactingRear != null) {
                    // Find the particle for that connection
                    Particle next = particles.get(current.state.interactingRear);

                    // Check if it's not the previous node and it's not already been walked as part of another iteration
                    if (next != null && prev != next && !ordered.contains(next)) {
                        ordered.add(next);
                        prev = current;
                        current = next;
                        continue;
                    }
                }
                break;
            }
        }

        // collisions follows same order
        List<Collision> collisions = new ArrayList<>();
        for (Particle particleA : ordered) {
            if (particleA.state.interactingFront != null) {
                Collision c = Collision.detect(particleA, particles.get(particleA.state.interactingFront));
                if (c != null) {
                    collisions.add(c);
                }
            }

            if (particleA.state.interactingRear != null) {
                Collision c = Collision.detect(particleA, particles.get(particleA.state.interactingRear));
                if (c != null) {
                    collisions.add(c);
                }
            }

            particleA.fullyLinked = true;
        }

        //System.out.println("Collisions: " + collisions.size());

        for (Collision collision : collisions) {
            collision.fixPositions();
        }

        for (Collision collision : collisions) {
            collision.collide();
        }

        particles.values().forEach(Particle::applyVelocity);
    }
}
