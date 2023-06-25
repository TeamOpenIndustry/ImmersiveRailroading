package cam72cam.immersiverailroading.entity.physics;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
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
 * */
public class Consist {
    static boolean debug = false;

    public static class Particle {
        public SimulationState state;

        public double mass_Kg;
        public double position_M;
        public double velocity_M_S;

        public double remainingFriction_KgM_S_S;

        public double initial_position_M;
        public double force_KgM_S_S;
        public double friction_KgM_S_S;

        public Linkage nextLink;
        public Linkage prevLink;

        public int direction;

        public Particle(SimulationState state, boolean same) {
            this.state = state;
            this.direction = same ? 1 : -1;

            this.mass_Kg = state.config.massKg;
            this.position_M = this.initial_position_M = 0; // Updated by Linkage
            this.velocity_M_S = Speed.fromMinecraft(state.velocity).metric()/3.6 * direction;
            this.force_KgM_S_S = state.forcesNewtons() * direction;
            this.friction_KgM_S_S = state.frictionNewtons();
        }

        public void setup() {
            remainingFriction_KgM_S_S = friction_KgM_S_S;
            if (nextLink != null) {
                nextLink.update();
            }
            /*
            if (Math.abs(velocity_M_S) > 0) {
                System.out.printf("Pos: %s, Vel: %s%n", position_M, velocity_M_S);
            }*/
        }

        public void computeVelocity(double dt_S) {
            double netForce_KgM_S_S = force_KgM_S_S;

            if (netForce_KgM_S_S == 0) {
                return;
            }

            List<Particle> particles = new ArrayList<>();
            particles.add(this);
            this.interactingParticles(particles, netForce_KgM_S_S > 0);

            double totalFriction_KgM_S_S = 0;
            double netMass_Kg = 0;
            for (Particle particle : particles) {
                totalFriction_KgM_S_S += particle.remainingFriction_KgM_S_S;
                netMass_Kg += particle.mass_Kg;
            }
            double resistedForce_KgM_S_S = Math.copySign(Math.min(Math.abs(netForce_KgM_S_S), totalFriction_KgM_S_S), netForce_KgM_S_S);
            netForce_KgM_S_S = netForce_KgM_S_S - resistedForce_KgM_S_S;

            double dv_M_S = netForce_KgM_S_S * dt_S / netMass_Kg;
            if (debug) {
                System.out.printf("Accelerating %d particles by %.4f M/S with force of %.4f KgM/S/S from starting force of %.4f KgM/S/S and friction %.4f KgM/S/S%n", particles.size(), dv_M_S, netForce_KgM_S_S, force_KgM_S_S, totalFriction_KgM_S_S);
            }
            for (Particle particle : particles) {
                if (Math.abs(dv_M_S) > 0) {
                    particle.velocity_M_S = particle.velocity_M_S + dv_M_S;
                }
                if (totalFriction_KgM_S_S > 0 && Math.abs(resistedForce_KgM_S_S) > 0) {
                    // Proportional to available friction
                    particle.remainingFriction_KgM_S_S = particle.remainingFriction_KgM_S_S - (Math.abs(resistedForce_KgM_S_S) * particle.remainingFriction_KgM_S_S / totalFriction_KgM_S_S);
                }
            }
        }

        public void applyFriction(double dt_S) {
            if (remainingFriction_KgM_S_S == 0 || Math.abs(velocity_M_S) == 0) {
                return;
            }
            List<Particle> particles = new ArrayList<>();
            particles.add(this);
            this.interactingParticles(particles, velocity_M_S < 0);

            // TODO this might be clearer in terms of Momentum (technically equivalent)
            double totalMass_Kg = 0.0;
            double totalVelocity_M_S = 0.0;
            for (Particle particle : particles) {
                totalMass_Kg += particle.mass_Kg;
                totalVelocity_M_S += particle.velocity_M_S;
            }

            double availableResistance_M_S = remainingFriction_KgM_S_S / totalMass_Kg * dt_S;

            double resistedVelocity_M_S = Math.copySign(Math.min(Math.abs(totalVelocity_M_S), availableResistance_M_S), totalVelocity_M_S);

            if (debug && Math.abs(resistedVelocity_M_S) > 0.0001) {
                System.out.printf("DeltaV of %d particles totals %.4f M/S (%.4f M/S avg) from a starting velocity of %.4f M/S and resistance of %.4f M/S%n", particles.size(), resistedVelocity_M_S, (resistedVelocity_M_S / particles.size()), totalVelocity_M_S, availableResistance_M_S);
            }
            for (Particle particle : particles) {
                // Proportional to smooth out differing velocities
                particle.velocity_M_S = particle.velocity_M_S - (resistedVelocity_M_S * particle.velocity_M_S / totalVelocity_M_S);
            }

            remainingFriction_KgM_S_S = (availableResistance_M_S - Math.abs(resistedVelocity_M_S)) * totalMass_Kg / dt_S;
        }

        public void computePosition(double dt) {
            // Apply velocity
            position_M = position_M + velocity_M_S * dt;
            /*
            if (prevLink != null) {
                // This is probably redundant if the processing order is stable
                prevLink.correctDistance();
            }
            if (nextLink != null) {
                nextLink.correctDistance();
            }*/
        }


        public void interactingParticles(Collection<Particle> particles, boolean positive) {
            if (prevLink != null) {
                Particle prevParticle = prevLink.prevParticle;
                if (!particles.contains(prevParticle)) {
                    // Positive: We are moving away from the previous particle
                    // Negative: We are moving toward the previous particle
                    if (positive ? prevLink.canPull : prevLink.canPush) {
                        particles.add(prevParticle);
                        prevParticle.interactingParticles(particles, positive);
                    }
                }
            }
            if (nextLink != null) {
                Particle nextParticle = nextLink.nextParticle;
                if (!particles.contains(nextParticle)) {
                    // Positive: We are moving toward the next particle
                    // Negative: We are moving away from the next particle
                    if (positive ? nextLink.canPush : nextLink.canPull) {
                        particles.add(nextParticle);
                        nextParticle.interactingParticles(particles, positive);
                    }
                }
            }
        }

        public void processCollisions() {
            // For new, use old "simple" collision code
            if (1 == 1) {
                if (nextLink != null && (nextLink.canPull || nextLink.canPush)) {
                    this.collide_old(nextLink.nextParticle);
                }
                return;
            }

            // Assume non looping for now
            /*
             * We always care about the state of nextlink
             *
             * if nextlink is pushing, we care about other pushing
             *      we collide that with this + who is pushing us
             *
             * if nextlink is pulling, we care about other pulling
             *      we collide that with this + who is pulling us
             *
             */

            if (nextLink == null || !nextLink.canPull && !nextLink.canPush) {
                return;
            }

            List<Particle> a = new ArrayList<>();
            List<Particle> b = new ArrayList<>();

            double maxVD_M = 0.01;

            // a <-slower-> this <-faster-> b
            if (nextLink.canPull) {
                a.add(this);
                for (Linkage link = prevLink; link != null && link.canPull &&
                        link.prevParticle.velocity_M_S <= link.nextParticle.velocity_M_S// &&
                        ;//link.prevParticle.velocity_M_S >= link.nextParticle.velocity_M_S - maxVD_M;
                     link = link.prevParticle.prevLink) {
                    a.add(link.prevParticle);
                }
                for (Linkage link = nextLink; link != null && link.canPull &&
                        link.prevParticle.velocity_M_S <= link.nextParticle.velocity_M_S// &&
                        ;//link.prevParticle.velocity_M_S >= link.nextParticle.velocity_M_S - maxVD_M;
                     link = link.nextParticle.nextLink) {
                    b.add(link.nextParticle);
                }
            } else {
                a.add(this);
                // a >-faster-< this >-slower-< b
                for (Linkage link = prevLink; link != null && link.canPush &&
                        link.prevParticle.velocity_M_S >= link.nextParticle.velocity_M_S// &&
                        ;//link.prevParticle.velocity_M_S <= link.nextParticle.velocity_M_S + maxVD_M;
                     link = link.prevParticle.prevLink) {
                    a.add(link.prevParticle);
                }
                for (Linkage link = nextLink; link != null && link.canPush &&
                        link.prevParticle.velocity_M_S >= link.nextParticle.velocity_M_S// &&
                        ;//link.prevParticle.velocity_M_S <= link.nextParticle.velocity_M_S + maxVD_M;
                     link = link.nextParticle.nextLink) {
                    b.add(link.nextParticle);
                }
            }

            if (b.isEmpty() || a.isEmpty()) {
                return;
            }

            if (Math.abs(a.get(0).velocity_M_S - b.get(0).velocity_M_S) < 0.001) {
                return;
            }

            if (debug) {
                System.out.printf("Collision between %s and %s%n", a.stream().map(p -> p.state.config.id.toString()).collect(Collectors.joining(",")), b.stream().map(p -> p.state.config.id.toString()).collect(Collectors.joining(",")));
            }

            double a_mass_Kg = 0;
            double b_mass_Kg = 0;

            double a_velocity_M_S = 0;
            double b_velocity_M_S = 0;

            // Calculate current momentum
            double a_j_KgM_S = 0;
            double b_j_KgM_S = 0;

            double total_m_Kg = 0;
            for (Particle particle : a) {
                a_j_KgM_S += particle.mass_Kg * particle.velocity_M_S;
                a_mass_Kg += particle.mass_Kg;
                a_velocity_M_S += particle.velocity_M_S / a.size();
            }
            for (Particle particle : b) {
                b_j_KgM_S += particle.mass_Kg * particle.velocity_M_S;
                b_mass_Kg += particle.mass_Kg;
                b_velocity_M_S += particle.velocity_M_S / b.size();
            }

            // Calculate factors for momentum transfer
            total_m_Kg = a_mass_Kg + b_mass_Kg;
            double total_j_KgM_S = a_j_KgM_S + b_j_KgM_S;

            double a_dv_M_S = a_velocity_M_S - b_velocity_M_S;
            double b_dv_M_S = b_velocity_M_S - a_velocity_M_S;
            double a_dj_KgM_S = a_mass_Kg * a_dv_M_S;
            double b_dj_KgM_s = b_mass_Kg * b_dv_M_S;

            // Coefficient of restitution is how much of the dj is not absorbed by the impact
            double cr = 0.25;

            // DeltaV is probably good enough here.  We could do a "fancier" momentum transfer here, or rely on small time steps and individual collisions to resolve.  Let's see how this works.
            a_velocity_M_S = (b_dj_KgM_s * cr + total_j_KgM_S) / total_m_Kg;
            b_velocity_M_S = (a_dj_KgM_S * cr + total_j_KgM_S) / total_m_Kg;
            //a_dv_M_S = (b_dj_KgM_s * cr + total_j_KgM_S) / total_m_Kg - a_velocity_M_S;
            //b_dv_M_S = (a_dj_KgM_S * cr + total_j_KgM_S) / total_m_Kg - b_velocity_M_S;
            for (Particle particle : a) {
                //particle.velocity_M_S += a_dv_M_S;
                particle.velocity_M_S = a_velocity_M_S;
            }
            for (Particle particle : b) {
                //particle.velocity_M_S += b_dv_M_S;
                particle.velocity_M_S = b_velocity_M_S;
            }
        }

        public void collide_old(Particle b) {
            Particle a = this;

            if (a.velocity_M_S > b.velocity_M_S) {
                if (!nextLink.canPush) {
                    return;
                }
            } else {
                if (!nextLink.canPull) {
                    return;
                }
            }

            double relativeDifference = a.velocity_M_S + b.velocity_M_S == 0 ? 0 : Math.abs((a.velocity_M_S - b.velocity_M_S)/(a.velocity_M_S + b.velocity_M_S));
            if (relativeDifference < 0.001) {
                // Smooth out negligible collisions
                b.velocity_M_S = a.velocity_M_S;
                return;
            }

            if (debug) {
                System.out.printf("Collision between %s and %s%n", a.state.config.id, b.state.config.id);
            }

            // Calculate current momentum
            double a_j_KgM_S = a.mass_Kg * a.velocity_M_S;
            double b_j_KgM_S = b.mass_Kg * b.velocity_M_S;

            // Calculate factors for momentum transfer
            double total_m_Kg = a.mass_Kg + b.mass_Kg;
            double total_j_KgM_S = a_j_KgM_S + b_j_KgM_S;

            double a_dv_M_S = a.velocity_M_S - b.velocity_M_S;
            double b_dv_M_S = b.velocity_M_S - a.velocity_M_S;
            double a_dj_KgM_S = a.mass_Kg * a_dv_M_S;
            double b_dj_KgM_s = b.mass_Kg * b_dv_M_S;

            // Coefficient of restitution is how much of the dj is not absorbed by the impact
            double cr = 0.25;

            a.velocity_M_S = (b_dj_KgM_s * cr + total_j_KgM_S) / total_m_Kg;
            b.velocity_M_S = (a_dj_KgM_S * cr + total_j_KgM_S) / total_m_Kg;
        }




        public SimulationState applyToState(List<Vec3i> blocksAlreadyBroken) {
            double velocityMPT = Speed.fromMetric(this.velocity_M_S * 3.6).minecraft(); // per 1 tick

            // Calculate the applied velocity from this particle.  This should not include the coupler adjustment speed/distance below
            state.velocity = velocityMPT * direction;

            return this.state.next((position_M - initial_position_M) * direction, blocksAlreadyBroken);
        }
    }

    public static class Linkage {
        private final Particle prevParticle;
        private final Particle nextParticle;

        public double minDistance_M;
        public double maxDistance_M;

        public boolean coupled;


        // Dynamic
        private double currentDistance_M;
        private boolean canPush;
        private boolean canPull;

        public Linkage(Particle prev, Particle next) {
            this.prevParticle = prev;
            this.nextParticle = next;

            boolean prevCouplerFront = next.state.config.id.equals(prev.state.interactingFront);
            boolean nextCouplerFront = prev.state.config.id.equals(next.state.interactingFront);

            double maxCouplerDistance = (prevCouplerFront ? prev.state.config.couplerSlackFront : prev.state.config.couplerSlackRear) +
                       (nextCouplerFront ? next.state.config.couplerSlackFront : next.state.config.couplerSlackRear);
            //maxCouplerDistance = 0.25;

            boolean prevEngaged = prevCouplerFront ?
                    prev.state.config.couplerEngagedFront :
                    prev.state.config.couplerEngagedRear;
            boolean nextEngaged = nextCouplerFront ?
                    next.state.config.couplerEngagedFront :
                    next.state.config.couplerEngagedRear;

            Vec3d prevPos = prev.state.position;
            Vec3d nextPos = next.state.position;
            Vec3d prevCoupler = prevCouplerFront ? prevParticle.state.couplerPositionFront : prevParticle.state.couplerPositionRear;
            Vec3d nextCoupler = nextCouplerFront ? nextParticle.state.couplerPositionFront : nextParticle.state.couplerPositionRear;

            // These two distanceTos could be replaced with definitions
            double prevLength = prevCoupler.distanceTo(prevPos);
            double nextLength = nextCoupler.distanceTo(nextPos);
            double couplerDistance = prevCoupler.distanceTo(nextCoupler);

            boolean isOverlapping = prevPos.distanceToSquared(prevCoupler) > prevPos.distanceToSquared(nextCoupler);
            couplerDistance = couplerDistance * (isOverlapping ? -1 : 1);

            double totalLength = prevLength + nextLength + couplerDistance;

            // Setup nextParticle position based on prevParticle
            nextParticle.initial_position_M = nextParticle.position_M = prevParticle.position_M + totalLength;

            // Setup min/max distances
            minDistance_M = prevLength + nextLength - maxCouplerDistance;
            maxDistance_M = prevLength + nextLength + maxCouplerDistance;
            coupled = prevEngaged && nextEngaged;
        }

        public void update() {
            // Next particle always has a larger position
            this.currentDistance_M = nextParticle.position_M - prevParticle.position_M;
            this.canPush = currentDistance_M <= minDistance_M;
            this.canPull = coupled && currentDistance_M >= maxDistance_M;
            if (debug) {
                //ImmersiveRailroading.info("min:%s curr:%s max:%s pull:%s push: %s", minDistance_M, currentDistance_M, maxDistance_M, canPull, canPush);
            }
        }

        /**
         * This function should rarely make positions to the simulation
         */
        public void correctDistance() {
            // Recompute here since slack is changing as we iterate
            this.currentDistance_M = nextParticle.position_M - prevParticle.position_M;

            // Too Close
            if (currentDistance_M - minDistance_M < -0.001) {
                // Recompute position
                if (debug) {
                    System.out.printf("CORRECTION %s%n", currentDistance_M - minDistance_M);
                }
                nextParticle.position_M = prevParticle.position_M + (minDistance_M);
                if (nextParticle.velocity_M_S < prevParticle.velocity_M_S)  {
                    nextParticle.velocity_M_S = prevParticle.velocity_M_S;
                }
            }

            // Too far and coupled
            if (currentDistance_M - maxDistance_M > 0.001 && coupled) {
                // Recompute position
                if (debug) {
                    System.out.printf("CORRECTION %s%n", currentDistance_M - maxDistance_M);
                }
                nextParticle.position_M = prevParticle.position_M + (maxDistance_M);
                if (nextParticle.velocity_M_S > prevParticle.velocity_M_S) {
                    nextParticle.velocity_M_S = prevParticle.velocity_M_S;
                }
            }
        }
    }

    public static Map<UUID, SimulationState> iterate(Map<UUID, SimulationState> states, List<Vec3i> blocksAlreadyBroken) {
        debug = false;
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
            boolean canBeUnloaded = consist.stream().allMatch(p -> p.state.velocity == 0 && state.forcesNewtons() == 0);
            consist.forEach(p -> p.state.canBeUnloaded = canBeUnloaded);

            boolean dirty = consist.stream().anyMatch(p -> p.state.dirty);

            // Spread the brake pressure evenly.  TODO spread it from the suppliers (requires complete rethink of brake controls)
            float desiredBrakePressure = (float) consist.stream().mapToDouble(x -> x.state.config.desiredBrakePressure).max().orElse(0);
            boolean needsBrakeEqualization = consist.stream().anyMatch(x -> x.state.config.hasPressureBrake && Math.abs(x.state.brakePressure - desiredBrakePressure) > 0.01);
            if (needsBrakeEqualization) {
                //dirty = true;
                double brakePressureDelta = 0.1 / consist.stream().filter(x -> x.state.config.hasPressureBrake).count();
                consist.forEach(p -> {
                    if (p.state.config.hasPressureBrake) {
                        if (Config.ImmersionConfig.instantBrakePressure) {
                            p.state.brakePressure = desiredBrakePressure;
                        } else {
                            if (p.state.brakePressure > desiredBrakePressure + brakePressureDelta) {
                                p.state.brakePressure -= brakePressureDelta;
                            } else if (p.state.brakePressure < desiredBrakePressure - brakePressureDelta) {
                                p.state.brakePressure += brakePressureDelta;
                            } else {
                                p.state.brakePressure = desiredBrakePressure;
                            }
                        }
                    }
                });
            }

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

        double ticksPerSecond = 20;
        double stepsPerTick = 40;
        double dt_S = (1 / (ticksPerSecond * stepsPerTick));

        if (!particles.isEmpty()) {
            if (debug) {
                System.out.println("=============BOUNDARY==========");
                // This shows that the movement between consist simulations is not exact.
                // Some instability can be seen between ticks as the couplers get slack introduced
            }
        }

        // Spread forces
        for (int i = 0; i < stepsPerTick; i++) {
            particles.forEach(Particle::setup);
            particles.forEach(p -> p.computeVelocity(dt_S));
            particles.forEach(p -> p.applyFriction(dt_S));
            particles.forEach(Particle::processCollisions);
            particles.forEach(p -> p.applyFriction(dt_S));
            particles.forEach(p -> p.computePosition(dt_S));
            for (Particle particle : particles) {
                if (particle.nextLink != null) {
                    particle.nextLink.correctDistance();
                }
            }
        }

        // Generate new states
        try {
            return particles.stream().map(particle -> particle.applyToState(blocksAlreadyBroken)).collect(Collectors.toMap(s -> s.config.id, s -> s));
        } catch (Exception ex) {
            for (SimulationState state : states.values()) {
                ImmersiveRailroading.debug("State: %s (%s, %s)", state.config.id, state.interactingFront, state.interactingRear);
            }
            for (Particle particle : particles) {
                ImmersiveRailroading.debug("Particle: %s (%s, %s)", particle.state.config.id, particle.state.interactingFront, particle.state.interactingRear);
            }

            throw ex;
        }
    }
}
