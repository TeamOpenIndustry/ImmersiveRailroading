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
                ImmersiveRailroading.info("Pos: %s, Vel: %s%n", position_M, velocity_M_S);
            }*/
        }

        public void computeVelocity(double dt_S) {
            double netForce_KgM_S_S = force_KgM_S_S;

            if (netForce_KgM_S_S == 0) {
                return;
            }

            List<Particle> particles = interactingParticles(netForce_KgM_S_S > 0);

            double totalFriction_KgM_S_S = 0;
            double netMass_Kg = 0;
            for (Particle particle : particles) {
                totalFriction_KgM_S_S += particle.remainingFriction_KgM_S_S;
                netMass_Kg += particle.mass_Kg;
            }
            double resistedForce_KgM_S_S = 0;
            // Friction only applies to the resisted force if it's in the same direction
            if (Math.copySign(1, netForce_KgM_S_S) == Math.copySign(1, velocity_M_S)) {
                resistedForce_KgM_S_S = Math.copySign(Math.min(Math.abs(netForce_KgM_S_S), totalFriction_KgM_S_S), netForce_KgM_S_S);
                netForce_KgM_S_S = netForce_KgM_S_S - resistedForce_KgM_S_S;
            }

            double dv_M_S = netForce_KgM_S_S * dt_S / netMass_Kg;
            if (debug) {
                ImmersiveRailroading.info("Accelerating %d particles by %.7f M/S with force of %.7f KgM/S/S from starting force of %.7f KgM/S/S and friction %.7f KgM/S/S%n", particles.size(), dv_M_S, netForce_KgM_S_S, force_KgM_S_S, totalFriction_KgM_S_S);
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
            List<Particle> particles = interactingParticles(velocity_M_S < 0);

            // TODO this might be clearer in terms of Momentum (technically equivalent)
            double totalMass_Kg = 0.0;
            double totalVelocity_M_S = 0.0;
            for (Particle particle : particles) {
                totalMass_Kg += particle.mass_Kg;
                totalVelocity_M_S += particle.velocity_M_S;
            }

            double availableResistance_M_S = remainingFriction_KgM_S_S / totalMass_Kg * dt_S;

            double resistedVelocity_M_S = Math.copySign(Math.min(Math.abs(totalVelocity_M_S), availableResistance_M_S), totalVelocity_M_S);

            if (debug && Math.abs(resistedVelocity_M_S) > 0.00001) {
                ImmersiveRailroading.info("DeltaV of %d particles totals %.7f M/S (%.7f M/S avg) from a starting velocity of %.7f M/S and resistance of %.7f M/S%n", particles.size(), resistedVelocity_M_S, (resistedVelocity_M_S / particles.size()), totalVelocity_M_S, availableResistance_M_S);
            }
            for (Particle particle : particles) {
                particle.velocity_M_S = particle.velocity_M_S - resistedVelocity_M_S;
            }

            remainingFriction_KgM_S_S = (availableResistance_M_S - Math.abs(resistedVelocity_M_S)) * totalMass_Kg / dt_S;
        }

        public void computePosition(double dt) {
            // Apply velocity
            position_M = position_M + velocity_M_S * dt;
            if (prevLink != null) {
                prevLink.correctDistance();
            }
        }


        public List<Particle> interactingParticles(boolean positive) {
            List<Particle> particles = new ArrayList<>();
            particles.add(this);
            // Positive: We are moving away from the previous particle
            // Negative: We are moving toward the previous particle
            for (Linkage link = prevLink; link != null && (positive ? link.canPull : link.canPush); link = link.prevParticle.prevLink) {
                particles.add(link.prevParticle);
            }
            // Positive: We are moving toward the next particle
            // Negative: We are moving away from the next particle
            for (Linkage link = nextLink; link != null && (positive ? link.canPush : link.canPull); link = link.nextParticle.nextLink) {
                particles.add(link.nextParticle);
            }
            return particles;
        }

        public void processCollisions() {
            if (nextLink == null) {
                return;
            }

            Particle a = this;
            Particle b = nextLink.nextParticle;

            if (a.velocity_M_S > b.velocity_M_S) {
                if (!nextLink.canPush) {
                    return;
                }
            } else {
                if (!nextLink.canPull) {
                    return;
                }
            }

            // TODO this falls apart at low speeds?
            double relativeDifference = Math.abs(a.velocity_M_S + b.velocity_M_S) < 0.01 ? 0 : Math.abs((a.velocity_M_S - b.velocity_M_S)/(a.velocity_M_S + b.velocity_M_S));
            if (relativeDifference < 0.00001) {
                // Negligible
                return;
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

            if (relativeDifference > 0.05) {
                // Coefficient of restitution is how much of the dj is not absorbed by the impact
                double cr = 0.25;

                if (debug) {
                    ImmersiveRailroading.info("Collision %s between %s and %s: %s and %s %n", relativeDifference, a.state.config.id, b.state.config.id, a.velocity_M_S, b.velocity_M_S);
                }

                a.velocity_M_S = (b_dj_KgM_s * cr + total_j_KgM_S) / total_m_Kg;
                b.velocity_M_S = (a_dj_KgM_S * cr + total_j_KgM_S) / total_m_Kg;

                if (Math.abs(a.velocity_M_S + b.velocity_M_S) > 0.1) {
                    state.collided = Math.max(state.collided, relativeDifference);
                }
            } else {
                // Smaller deltas should be treated as negligible w/ perfect restitution
                if (debug) {
                    ImmersiveRailroading.info("Merge %s between %s and %s: %s and %s%n", relativeDifference, a.state.config.id, b.state.config.id, a.velocity_M_S, b.velocity_M_S);
                }
                a.velocity_M_S = b.velocity_M_S = total_j_KgM_S / total_m_Kg;
            }
        }




        public SimulationState applyToState(List<Vec3i> blocksAlreadyBroken) {
            double velocityMPT = Speed.fromMetric(this.velocity_M_S * 3.6).minecraft(); // per 1 tick

            // Calculate the applied velocity from this particle.  This should not include the coupler adjustment speed/distance below
            state.velocity = velocityMPT * direction;

            SimulationState future =  this.state.next((position_M - initial_position_M) * direction, blocksAlreadyBroken);

            //ImmersiveRailroading.info("Moving: %s", position_M - initial_position_M);

            Linkage frontLink = null;
            Linkage rearLink = null;
            if (prevLink != null) {
                if (prevLink.prevParticle.state.config.id.equals(future.interactingFront)) {
                    frontLink = prevLink;
                } else if (prevLink.prevParticle.state.config.id.equals(future.interactingRear)) {
                    rearLink = prevLink;
                }
            }
            if (nextLink != null) {
                if (nextLink.nextParticle.state.config.id.equals(future.interactingFront)) {
                    frontLink = nextLink;
                } else if (nextLink.nextParticle.state.config.id.equals(future.interactingRear)) {
                    rearLink = nextLink;
                }
            }

            future.frontPushing = frontLink != null && frontLink.canPush;
            future.frontPulling = frontLink != null && frontLink.canPull;
            future.rearPushing = rearLink != null && rearLink.canPush;
            future.rearPulling = rearLink != null && rearLink.canPull;

            return future;
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

        }

        public void setup() {
            Particle prev = this.prevParticle;
            Particle next = this.nextParticle;

            boolean prevCouplerFront = next.state.config.id.equals(prev.state.interactingFront);
            boolean nextCouplerFront = prev.state.config.id.equals(next.state.interactingFront);

            double maxCouplerDistance = (prevCouplerFront ? prev.state.config.couplerSlackFront : prev.state.config.couplerSlackRear) +
                    (nextCouplerFront ? next.state.config.couplerSlackFront : next.state.config.couplerSlackRear);
            //maxCouplerDistance = 0.21;
            //maxCouplerDistance = 0;

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
            //double couplerDistance = prevCoupler.distanceTo(nextCoupler);

            //boolean isOverlapping = prevPos.distanceToSquared(prevCoupler) > prevPos.distanceToSquared(nextCoupler);
            double couplerDistance = (prevPos.distanceTo(nextCoupler) - prevPos.distanceTo(prevCoupler) + (nextPos.distanceTo(prevCoupler) - nextPos.distanceTo(nextCoupler)))/2;
            double totalLength = prevLength + nextLength + couplerDistance;

            // Setup nextParticle position based on prevParticle
            nextParticle.initial_position_M = nextParticle.position_M = prevParticle.position_M + totalLength;

            // Setup min/max distances
            minDistance_M = prevLength + nextLength - maxCouplerDistance;
            maxDistance_M = prevLength + nextLength + maxCouplerDistance;
            coupled = prevEngaged && nextEngaged;

            // Fix precision issues in track pathing between cycles
            if (maxCouplerDistance == 0) {
                correctDistance();
            } else {
                if (nextCouplerFront ? next.state.frontPushing : next.state.rearPushing) {
                    double delta = nextParticle.position_M - (prevParticle.position_M + minDistance_M);
                    if (Math.abs(delta) > 0.01 && debug) {
                        ImmersiveRailroading.info("DELTA PUSH %s : %s + %s + %s = %s vs %s :: %s vs %s ?? %s %s", delta, prevLength, nextLength, couplerDistance, totalLength, minDistance_M, prevCoupler, nextCoupler, prevPos.distanceTo(prevCoupler) - prevPos.distanceTo(nextCoupler), nextPos.distanceTo(prevCoupler) - nextPos.distanceTo(nextCoupler));
                    }
                    nextParticle.position_M = prevParticle.position_M + minDistance_M;
                    //nextParticle.position_M -= Math.copySign(Math.min(Math.abs(delta), maxCouplerDistance), isOverlapping ? -1 : 1);
                } else if (nextCouplerFront ? next.state.frontPulling : next.state.rearPulling) {
                    double delta = nextParticle.position_M - (prevParticle.position_M + maxDistance_M);
                    if (Math.abs(delta) > 0.01 && debug) {
                        ImmersiveRailroading.info("DELTA PULL %s : %s + %s + %s = %s vs %s :: %s vs %s ?? %s %s", delta, prevLength, nextLength, couplerDistance, totalLength, maxDistance_M, prevCoupler, nextCoupler, prevPos.distanceTo(prevCoupler) - prevPos.distanceTo(nextCoupler), nextPos.distanceTo(prevCoupler) - nextPos.distanceTo(nextCoupler));

                    }
                    nextParticle.position_M = prevParticle.position_M + maxDistance_M;
                    //nextParticle.position_M += Math.copySign(Math.min(Math.abs(delta), maxCouplerDistance), isOverlapping ? 1 : -1);
                }
            }
        }

        public void update() {
            // Next particle always has a larger position
            double epsilon = 0.000001;
            this.currentDistance_M = nextParticle.position_M - prevParticle.position_M;
            this.canPush = currentDistance_M - minDistance_M <= epsilon || minDistance_M == maxDistance_M;
            this.canPull = coupled && currentDistance_M - maxDistance_M >= -epsilon || minDistance_M == maxDistance_M;
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
                    ImmersiveRailroading.info("CORRECTION %s %s%n", nextParticle.hashCode(), currentDistance_M - minDistance_M);
                }
                nextParticle.position_M = prevParticle.position_M + (minDistance_M);
                if (nextParticle.velocity_M_S < prevParticle.velocity_M_S) {
                    //nextParticle.velocity_M_S = prevParticle.velocity_M_S;
                }
            }

            // Too far and coupled
            if (currentDistance_M - maxDistance_M > 0.001 && coupled) {
                // Recompute position
                if (debug) {
                    ImmersiveRailroading.info("CORRECTION %s %s%n", nextParticle.hashCode(), currentDistance_M - maxDistance_M);
                }
                nextParticle.position_M = prevParticle.position_M + (maxDistance_M);
                if (nextParticle.velocity_M_S > prevParticle.velocity_M_S) {
                    //nextParticle.velocity_M_S = prevParticle.velocity_M_S;
                }
            }
        }
    }

    public static Map<UUID, SimulationState> iterate(Map<UUID, SimulationState> states, List<Vec3i> blocksAlreadyBroken) {
        debug = false;
        // ordered
        List<Particle> particles = new ArrayList<>();

        List<SimulationState> used = new ArrayList<>();

        if (!states.isEmpty()) {
            if (debug) {
                System.out.println("=============BOUNDARY==========");
                // This shows that the movement between consist simulations is not exact.
                // Some instability can be seen between ticks as the couplers get slack introduced
            }
        }


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
            // TODO don't spread brake pressure across shunting connections
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

        // At this point we should have an ordered list, particle -> next -> next, etc...

        double ticksPerSecond = 20;
        double stepsPerTick = 40;
        double dt_S = (1 / (ticksPerSecond * stepsPerTick));

        for (Particle particle : particles) {
            if (particle.nextLink != null) {
                particle.nextLink.setup();
            }
        }

        // Spread forces
        for (int i = 0; i < stepsPerTick; i++) {
            particles.forEach(Particle::setup);

            if (debug) {
                String s = "";
                for (Particle particle : particles) {
                    s += String.format("[%s = %.3f]", particle.hashCode(), particle.velocity_M_S);
                    if (particle.nextLink != null) {
                        if (particle.nextLink.canPush) {
                            s += " >< ";
                        } else if (particle.nextLink.canPull) {
                            s += " <> ";
                        } else {
                            s += String.format(" %.2f ", particle.nextLink.currentDistance_M - particle.nextLink.minDistance_M);
                        }
                    } else {
                        ImmersiveRailroading.info(s);
                        s = "";
                    }
                }
                if (!s.isEmpty()) {
                    ImmersiveRailroading.info(s);
                }
            }

            particles.forEach(p -> p.computeVelocity(dt_S));
            particles.forEach(p -> p.applyFriction(dt_S));
            particles.forEach(Particle::processCollisions);
            particles.forEach(p -> p.applyFriction(dt_S));
            particles.forEach(p -> p.computePosition(dt_S));
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
