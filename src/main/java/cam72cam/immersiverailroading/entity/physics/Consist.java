package cam72cam.immersiverailroading.entity.physics;

import cam72cam.immersiverailroading.util.Speed;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 1d dynamics simulator
 *
 * It's a touch special since it applies forces to groups of
 * particles when they are interfering with each other via slacked linkages
 * It is not technically correct, but I believe it will produce the most reasonable simulation
 *
 * TODO: Make particles / linkages state agnostic for unit testing
 *
 * Question: Do we want to store the deltaV in particle instead of immediately applying it to velocity?
 * */
public class Consist {
    public static class Particle {
        public SimulationState state;

        // Acceleration along consist axis (m/s/s)
        public double acceleration;
        // Friction along consist axis (m/s/s)
        public double friction;
        // Velocity along consist axis (m/s)
        public double velocity;
        // Offset due to coupler overlap (m)
        public double offset;
        // consist axis -> vehicle axis
        public int direction;

        public Linkage prevLink;
        public Linkage nextLink;

        public Particle(SimulationState state, boolean same) {
            this.direction = same ? 1 : -1;

            this.state = state;
            this.acceleration = state.forcesNewtons() * direction;
            this.friction = state.frictionNewtons();
            this.velocity = Speed.fromMinecraft(state.velocity).metric() * direction;
        }

        public void fixNextCoupler() {
            if (nextLink != null) {
                nextLink.fixNextPosition();
            }
        }

        public void findAffectedByForce(double force, List<Particle> output, boolean recursive) {
            if (prevLink != null) {
                if (force > 0 && prevLink.isPulling || force < 0 && prevLink.isPushing) {
                    if (!output.contains(prevLink.prevParticle)) {
                        output.add(prevLink.prevParticle);
                        if (recursive) {
                            prevLink.prevParticle.findAffectedByForce(force, output, true);
                        }
                    }
                }
            }
            if (nextLink != null) {
                if (force < 0 && nextLink.isPulling || force > 0 && nextLink.isPushing) {
                    if (!output.contains(nextLink.nextParticle)) {
                        output.add(nextLink.nextParticle);
                        if (recursive) {
                            nextLink.nextParticle.findAffectedByForce(force, output, true);
                        }
                    }
                }
            }
        }

        public void applyNextCollision() {
            // Since we are iterating, we only want to apply collisions from this -> next
            // Order should not matter, if it does, we will sort that out elsewhere

            if (this.nextLink == null) {
                return;
            }

            double velocityA = this.velocity;
            double velocityB = this.nextLink.nextParticle.velocity;

            // What direction we are applying force in
            double deltaV = velocityA - velocityB;
            if (Math.abs(deltaV) < 0.1) {
                return;
            }

            // This is what fudges the physics to treat a group of cars as a single rigid body
            // My testing so far has shown that it makes normal operations more smooth
            // but edge cases (like newtons cradle) don't work as well
            boolean groupForces = true;

            // Target
            List<Particle> groupB = new ArrayList<>();

            // Setup end-stops on iteration
            groupB.add(this);
            if (this.prevLink != null) {
                groupB.add(this.prevLink.prevParticle);
            }

            // Try to find particles in the next direction that are affected by our collision
            this.findAffectedByForce(deltaV, groupB, groupForces);

            // Remove end-stops on iteration
            groupB.remove(this);
            if (this.prevLink != null) {
                groupB.remove(this.prevLink.prevParticle);
            }

            if (groupB.isEmpty()) {
                // Next particle was not a collision
                return;
            }

            // Source
            List<Particle> groupA = new ArrayList<>();
            // We are part of the supporting group by default
            groupA.add(this);
            // Try to find particles in the prev direction that support us
            if (groupForces) {
                this.findAffectedByForce(-deltaV, groupA, true);
            }

            double massA = groupA.stream().mapToDouble(p -> p.state.config.massKg).sum();
            double massB = groupB.stream().mapToDouble(p -> p.state.config.massKg).sum();

            double restitution = 0.3;
            double deltaVA = (restitution * massB * (velocityB - velocityA) + massA * velocityA + massB * velocityB) / (massA + massB) - velocityA;
            double deltaVB = (restitution * massA * (velocityA - velocityB) + massA * velocityA + massB * velocityB) / (massA + massB) - velocityB;

            groupA.forEach(p -> p.velocity += deltaVA);
            groupB.forEach(p -> p.velocity += deltaVB);
        }

        public void applyAcceleration() {
            if (Math.abs(acceleration) < 0.001) {
                return;
            }
            List<Particle> affected = new ArrayList<>();
            affected.add(this);
            findAffectedByForce(acceleration, affected, true);
            double totalMassKg = affected.stream().mapToDouble(p -> p.state.config.massKg).sum();
            double deltaV = acceleration / totalMassKg;
            affected.forEach(p -> p.velocity += deltaV);
        }

        public void applyFriction() {
            List<Particle> affected = new ArrayList<>();
            affected.add(this);
            findAffectedByForce(-friction, affected, true);
            double totalMassKg = affected.stream().mapToDouble(p -> p.state.config.massKg).sum();

            // This could probably be improved, but is good enough for now.
            // We don't take into account left over available friction, but that's likely a negligible edge case
            double deltaV = friction / totalMassKg;
            affected.forEach(p -> p.velocity += Math.copySign(Math.min(deltaV, Math.abs(p.velocity)), -p.velocity));
        }


        public SimulationState applyToState(List<Vec3i> blocksAlreadyBroken) {
            state.velocity = Speed.fromMetric(velocity).minecraft() * direction;
            double movement = state.velocity + offset * direction;
            Vec3d currentPos = state.position;

            // TODO: Honestly this should all be refactored to the state.next function
            // It would reduce confusion on constructor logic
            SimulationState state = this.state.next(movement);
            if (currentPos.equals(state.position)) {
                state.velocity = 0;
            } else {
                state.calculateCouplerPositions();
                // We will actually break the blocks
                this.state.blocksToBreak = this.state.interferingBlocks;
                // We can now ignore those positions for the rest of the simulation
                blocksAlreadyBroken.addAll(this.state.blocksToBreak);
                // Calculate the next states interference
                state.calculateBlockCollisions(blocksAlreadyBroken);
            }
            return state;
        }
    }

    public static class Linkage {
        private final Particle prevParticle;
        private final Particle nextParticle;

        // TODO triginomify for performance
        private final Vec3d prevCoupler;
        private final Vec3d nextCoupler;

        public boolean isPushing;
        public boolean isPulling;

        private final double maxSlack;

        public Linkage(Particle prev, Particle next) {
            this.prevParticle = prev;
            this.nextParticle = next;

            boolean prevCouplerFront = next.state.config.id.equals(prev.state.interactingFront);
            boolean nextCouplerFront = prev.state.config.id.equals(next.state.interactingFront);

            maxSlack = (prevCouplerFront ? prev.state.config.couplerSlackFront : prev.state.config.couplerSlackRear) +
                       (nextCouplerFront ? next.state.config.couplerSlackFront : next.state.config.couplerSlackRear);

            prevCoupler = prevCouplerFront ? prevParticle.state.couplerPositionFront : prevParticle.state.couplerPositionRear;
            nextCoupler = nextCouplerFront ? nextParticle.state.couplerPositionFront : nextParticle.state.couplerPositionRear;

            boolean prevEngaged = prevCouplerFront ?
                    prev.state.config.couplerEngagedFront :
                    prev.state.config.couplerEngagedRear;
            boolean nextEngaged = nextCouplerFront ?
                    next.state.config.couplerEngagedFront :
                    next.state.config.couplerEngagedRear;

            Vec3d slack = prevCoupler.subtract(nextCoupler);
            boolean contacting = slack.lengthSquared() >= maxSlack * maxSlack;
            boolean isOverlapping = prev.state.position.distanceToSquared(prevCoupler) > prev.state.position.distanceToSquared(nextCoupler);

            isPushing = contacting && isOverlapping;
            isPulling = contacting && !isOverlapping && (prevEngaged && nextEngaged);
        }

        public void fixNextPosition() {
            if (isPushing || isPulling) {
                Vec3d slack = prevCoupler.subtract(nextCoupler);
                double fudge = 5 * prevParticle.state.config.gauge.scale();
                if (slack.lengthSquared() > maxSlack * maxSlack * fudge * fudge) {
                    double slackDist = (slack.length() - maxSlack);
                    nextParticle.offset += isPushing ? slackDist : -slackDist;
                }
            }
        }
    }

    public static Map<UUID, SimulationState> iterate(Map<UUID, SimulationState> states, List<Vec3i> blocksAlreadyBroken) {
        // ordered
        List<Particle> particles = new ArrayList<>();

        List<SimulationState> used = new ArrayList<>();

        for (SimulationState state : states.values()) {
            if (used.contains(state)) {
                continue;
            }

            // Iterate all the way to one end of the consist

            SimulationState current = state;
            boolean direction = true;

            List<SimulationState> visited = new ArrayList<>();
            while (!visited.contains(current)) {
                visited.add(current);

                // Find next
                UUID nextId = direction ? current.interactingFront : current.interactingRear;
                SimulationState next = nextId != null ? states.get(nextId) : null;
                if (next == null) {
                    break;
                }

                // If next is flipped from our direction
                if (!current.config.id.equals(direction ? next.interactingRear : next.interactingFront)) {
                    direction = !direction;
                }

                current = next;
            }

            // Current is now pointing at the head or the tail (does not matter which)

            // Invert iteration direction
            direction = !direction;

            List<Particle> consist = new ArrayList<>();

            Particle prevParticle = null;

            // Build up the consist starting at the head or the tail
            visited.clear();
            while (!visited.contains(current)) {
                visited.add(current);

                // Create the new particle
                Particle currParticle = new Particle(current, direction);
                consist.add(currParticle);

                // Link the two particles together
                if (prevParticle != null) {
                    Linkage link = new Linkage(prevParticle, currParticle);
                    prevParticle.nextLink = link;
                    currParticle.prevLink = link;
                }

                // Find next
                UUID nextId = direction ? current.interactingFront : current.interactingRear;
                SimulationState next = nextId != null ? states.get(nextId) : null;
                if (next == null) {
                    break;
                }

                // If next is flipped from our direction
                if (!current.config.id.equals(direction ? next.interactingRear : next.interactingFront)) {
                    direction = !direction;
                }


                current = next;
                prevParticle = currParticle;
            }

            // Propagate dirty flag
            boolean dirty = consist.stream().anyMatch(p -> p.state.dirty);
            if (dirty) {
                consist.forEach(p -> p.state.dirty = true);
                particles.addAll(consist);
            }

            // Make sure we can't accidentally hook into any of the processed states from this consist
            used.addAll(visited);
        }

        // At this point we should have an ordered list
        // Do we need to reverse it?
        //Collections.reverse(particles);

        // Figure out the coupler offset to be applied
        particles.forEach(Particle::fixNextCoupler);

        // Spread forces
        particles.forEach(Particle::applyNextCollision);
        particles.forEach(Particle::applyAcceleration);
        particles.forEach(Particle::applyFriction);

        // Generate new states
        return particles.stream().map(particle -> particle.applyToState(blocksAlreadyBroken)).collect(Collectors.toMap(s -> s.config.id, s -> s));
    }
}
