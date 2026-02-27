package cam72cam.immersiverailroading.entity.physics;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.entity.physics.chrono.ChronoState;
import cam72cam.immersiverailroading.entity.physics.chrono.ServerChronoState;
import cam72cam.immersiverailroading.net.MRSSyncPacket;
import cam72cam.immersiverailroading.physics.TickPos;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

import java.util.*;
import java.util.stream.Collectors;

public class Simulation {

    public static boolean forceQuickUpdates = false;
    public static int calculatedStates;
    public static int restStates;
    public static int keptStates;

    double maxCouplerDist = 4;

    private final World world;
    private final int startTickID;
    List<Map<UUID, SimulationState>> stateMaps;
    private final List<Vec3i> blocksAlreadyBroken;
    List<EntityCoupleableRollingStock> loaded;


    public Simulation(World world) {
        calculatedStates = keptStates = restStates = 0;
        long startTimeMs = System.currentTimeMillis();
        this.world = world;
        this.startTickID = ((ServerChronoState)ChronoState.getState(world)).getServerTickID();

        stateMaps = new ArrayList<>();
        blocksAlreadyBroken = new ArrayList<>();

        for (int i = 0; i < Config.ConfigDebug.physicsFutureTicks; i++) {
            stateMaps.add(new HashMap<>());
        }

        for (int i = 0; i < stateMaps.size()-1; i++) {
            simulateTick(i);
        }

        boolean sendPackets = world.getTicks() % (Config.ConfigDebug.physicsFutureTicks / 2) == 0;
        double syncDistanceSq = ImmersiveRailroading.ENTITY_SYNC_DISTANCE * ImmersiveRailroading.ENTITY_SYNC_DISTANCE;
        List<Player> players = sendPackets ? world.getEntities(Player.class) : null;

        // Apply new states
        for (EntityCoupleableRollingStock stock : loaded) {
            stock.states = stateMaps.stream().map(m -> m.get(stock.getUUID())).filter(Objects::nonNull).collect(Collectors.toList());
            for (SimulationState state : stock.states) {
                state.dirty = false;
            }
            stock.positions = stock.states.stream().map(TickPos::new).collect(Collectors.toList());
            if (sendPackets && players.stream().anyMatch(player -> player.getPosition().distanceToSquared(stock.getPosition()) < syncDistanceSq)) {
                new MRSSyncPacket(stock, stock.positions).sendToObserving(stock);
            }
        }

        long totalTimeMs = System.currentTimeMillis() - startTimeMs;
        if (totalTimeMs > Config.ConfigDebug.physicsWarnTotalThresholdMs) {
            ImmersiveRailroading.warn("Calculating Immersive Railroading Physics took %sms (%s, %s, %s)", totalTimeMs, calculatedStates, restStates, keptStates);
        }
    }

    public void simulateTick(int iteration) {
        long startTimeMs = System.currentTimeMillis();

        int tickID = startTickID + iteration;

        Map<UUID, SimulationState> stateMap = stateMaps.get(iteration);
        Map<UUID, SimulationState> nextStateMap = stateMaps.get(iteration+1);

        // Should really only ever run once, maybe twice.  Cut it off after 10 times just to be safe
        for (int tryLoad = 0; tryLoad < 10; tryLoad++) {
            // If this is too slow, we could hang onto the list of stock that is at rest.
            // We should only need to do this when stock is no longer at rest or changes dirty flag
            int lastCount = loaded == null ? 0 : loaded.size();
            loaded = world.getEntities(EntityCoupleableRollingStock.class);
            boolean newChunksLoaded = lastCount != loaded.size();

            for (EntityCoupleableRollingStock stock : loaded) {

                if (!stateMap.containsKey(stock.getUUID())) {
                    for (SimulationState state : stock.states) {
                        int stateIteration = state.tickID - tickID;
                        if (stateIteration >= 0) {
                            state.update(stock);
                            stateMaps.get(stateIteration).put(stock.getUUID(), state);
                        }
                    }

                    if (!stateMap.containsKey(stock.getUUID())) {
                        // This should only ever happen right after stock is placed.
                        SimulationState state = new SimulationState(stock);
                        state.tickID = tickID;
                        stateMap.put(stock.getUUID(), state);
                    }
                }

                SimulationState state = stateMap.get(stock.getUUID());


                // Don't need to load
                if (state.atRest && !state.dirty) {
                    continue;
                }

                // Keep it loaded
                world.keepLoaded(new Vec3i(state.position));

                if (state.consist.positions == null) {
                    continue;
                }

                // Load interacting stock locations
                // We could in the future do a HashSet<Consist> and reduce the calls to the world.
                for (Vec3i pos : state.consist.positions) {
                    if (!world.isBlockLoaded(pos)) {
                        ImmersiveRailroading.debug("Loading chunk at position %s", pos);
                        // Load the chunk, entities should be directly injected into the world
                        world.getBlock(pos);
                        newChunksLoaded = true;
                    }
                }
            }
            if (!newChunksLoaded) {
                break;
            }
        }

        List<SimulationState> states = new ArrayList<>(stateMap.values());

        // Propagate dirty (even across links that will be since that can change between iterations)
        Set<UUID> dirty = new HashSet<>();
        for (SimulationState state : nextStateMap.values()) { // I don't remember why this is the next map
            if (state.dirty) {
                dirty.addAll(state.consist.ids);
            }
        }
        for (UUID uuid : dirty) {
            SimulationState state = stateMap.get(uuid);
            if (state != null) {
                state.dirty = true;
            }
        }

        // Decouple / fix coupler positions
        for (SimulationState state : states) {
            for (boolean isMyCouplerFront : new boolean[]{true, false}) {
                UUID myID = state.config.id;
                UUID otherID = isMyCouplerFront ? state.interactingFront : state.interactingRear;
                Vec3d myCouplerPos = isMyCouplerFront ? state.couplerPositionFront : state.couplerPositionRear;
                String myCouplerLabel = isMyCouplerFront ? "Front" : "Rear";

                if (otherID == null) {
                    // No existing coupler, nothing to check
                    continue;
                }
                SimulationState other = stateMap.get(otherID);
                if (other == null) {
                    Vec3i otherPos = loaded.stream()
                            .filter(x -> x.getUUID().equals(myID)).findFirst()
                            .map(x -> isMyCouplerFront ? x.lastKnownFront : x.lastKnownRear).orElse(null);

                    if (otherPos != null && !world.isBlockLoaded(otherPos)) {
                        // Other location is not loaded, we must not need to do this check.
                        continue;
                    }

                    // This should really only be hit when removing a piece of stock
                    ImmersiveRailroading.debug("%s-%s: Stock not found %s (%s) -> %s!",
                            startTickID, state.tickID, myID, myCouplerLabel, otherID);
                    if (isMyCouplerFront) {
                        state.interactingFront = null;
                    } else {
                        state.interactingRear = null;
                    }
                    state.dirty = true;
                    continue;
                }

                boolean isOtherCouplerFront;
                if (myID.equals(other.interactingFront)) {
                    isOtherCouplerFront = true;
                } else if (myID.equals(other.interactingRear)) {
                    isOtherCouplerFront = false;
                } else {
                    /*
                    This can happen when a piece of stock is marked dirty (discard existing states ex: throttle/brake)
                    and a piece of stock it couples with in the future states has not been marked dirty.  The dirty
                    stock is starting from 0, while the one it has future coupled to in a previous pass (that's english right?)
                    does not know until this re-check that it has desync'd at this point and must generate new states
                    from here on out in this pass.

                    This should be fixed with the smarter "dirty" logic above
                     */
                    ImmersiveRailroading.warn("%s-%s: Mismatched coupler states: %s (%s) -> %s (%s, %s)",
                            startTickID, state.tickID,
                            myID, myCouplerLabel,
                            otherID, other.interactingFront, other.interactingRear);
                    if (isMyCouplerFront) {
                        state.interactingFront = null;
                    } else {
                        state.interactingRear = null;
                    }
                    state.dirty = true;
                    other.dirty = true;
                    continue;
                }

                Vec3d otherCouplerPos = isOtherCouplerFront ? other.couplerPositionFront : other.couplerPositionRear;
                String otherCouplerLabel = isOtherCouplerFront ? "Front" : "Rear";

                double maxCouplerDistScaled = maxCouplerDist * state.config.gauge.scale();
                if (myCouplerPos.distanceToSquared(otherCouplerPos) > maxCouplerDistScaled * maxCouplerDistScaled) {
                    ImmersiveRailroading.debug("%s-%s: Coupler snapping due to distance: %s (%s) -> %s (%s)",
                            startTickID, state.tickID,
                            myID, myCouplerLabel,
                            otherID, otherCouplerLabel);
                    state.dirty = true;
                    other.dirty = true;

                    if (isMyCouplerFront) {
                        state.interactingFront = null;
                    } else {
                        state.interactingRear = null;
                    }

                    if (isOtherCouplerFront) {
                        other.interactingFront = null;
                    } else {
                        other.interactingRear = null;
                    }
                }
            }
        }


        // check for potential couplings and collisions
        for (int sai = 0; sai < states.size()-1; sai++) {
            SimulationState stateA = states.get(sai);
            if (stateA.interactingFront != null && stateA.interactingRear != null) {
                // There's stock in front and behind, can't really hit any other stock here
                continue;
            }

            for (int sbi = sai+1; sbi < states.size(); sbi++) {
                SimulationState stateB = states.get(sbi);
                if (stateA.atRest && stateB.atRest && !stateA.dirty && !stateB.dirty) {
                    continue;
                }
                if (stateB.interactingFront != null && stateB.interactingRear != null) {
                    // There's stock in front and behind, can't really hit any other stock here
                    continue;
                }

                if (stateA.config.gauge != stateB.config.gauge) {
                    // Same gauge required
                    continue;
                }

                double centerDist = stateA.config.length + stateB.config.length;
                if (stateA.position.distanceToSquared(stateB.position) > centerDist * centerDist) {
                    // Too far to reasonably couple
                    continue;
                }

                if (!stateA.bounds.intersects(stateB.bounds)) {
                    // Not close enough to couple
                    continue;
                }

                if (stateB.config.id.equals(stateA.interactingFront) || stateB.config.id.equals(stateA.interactingRear)) {
                    // Already coupled
                    continue;
                }
                if (stateA.config.id.equals(stateB.interactingFront) || stateA.config.id.equals(stateB.interactingRear)) {
                    // Already coupled (double safe check)
                    continue;
                }

                // At this point the stock are colliding / overlapping and we need to do something about it

                /*
                 * 1. |-----a-----| |-----b-----|
                 * 2. |-----a---|=|----b-----|
                 * 3. |---|=a====b|-----|
                 * Keep in mind that we want to make sure that our other coupler might be a better fit
                 */

                // the coupler to target is whichever one the other's center is closest to
                boolean targetACouplerFront =
                        stateA.couplerPositionFront.distanceToSquared(stateB.position) <
                                stateA.couplerPositionRear.distanceToSquared(stateB.position);
                boolean targetBCouplerFront =
                        stateB.couplerPositionFront.distanceToSquared(stateA.position) <
                                stateB.couplerPositionRear.distanceToSquared(stateA.position);

                // Best coupler is already coupled to something
                if ((targetACouplerFront ? stateA.interactingFront : stateA.interactingRear) != null) {
                    continue;
                }
                if ((targetBCouplerFront ? stateB.interactingFront : stateB.interactingRear) != null) {
                    continue;
                }

                // Since bounding boxes can overlap across different tracks (think parallel curves) we need to do
                // a more fine-grained check here
                Vec3d couplerPosA = targetACouplerFront ? stateA.couplerPositionFront : stateA.couplerPositionRear;
                Vec3d couplerPosB = targetBCouplerFront ? stateB.couplerPositionFront : stateB.couplerPositionRear;
                // Move coupler pos up to inside the BB (it's at track level by default)
                // This could be optimized further, but it's an infrequent calculation
                couplerPosA = couplerPosA.add(0, stateB.bounds.max().subtract(stateB.bounds.min()).y/2, 0);
                couplerPosB = couplerPosB.add(0, stateA.bounds.max().subtract(stateA.bounds.min()).y/2, 0);
                if (!stateB.bounds.contains(couplerPosA) || !stateA.bounds.contains(couplerPosB)) {
                    // Not actually on the same track, just a BB collision and can be ignored
                    continue;
                }

                stateA.dirty = true;
                stateB.dirty = true;
                ImmersiveRailroading.debug("%s-%s: Coupling %s (%s) to %s (%s)",
                        startTickID, stateA.tickID,
                        stateA.config.id, targetACouplerFront ? "Front" : "Rear",
                        stateB.config.id, targetBCouplerFront ? "Front" : "Rear");

                // Ok, we are clear to proceed!
                if (targetACouplerFront) {
                    stateA.interactingFront = stateB.config.id;
                } else {
                    stateA.interactingRear = stateB.config.id;
                }
                if (targetBCouplerFront) {
                    stateB.interactingFront = stateA.config.id;
                } else {
                    stateB.interactingRear = stateA.config.id;
                }
            }
        }

        // calculate new velocities
        Consist.iterate(stateMap, nextStateMap, blocksAlreadyBroken);

        long totalTimeMs = System.currentTimeMillis() - startTimeMs;
        if (totalTimeMs > Config.ConfigDebug.physicsWarnThresholdMs) {
            ImmersiveRailroading.warn("Calculating Immersive Railroading Physics Iteration took %sms (%s, %s, %s)", totalTimeMs, calculatedStates, restStates, keptStates);
        }
    }






    public static void simulate(World world) {
        // 100KM/h ~= 28m/s which means non-loaded stationary stock may be phased through at that speed
        // I'm OK with that for now
        // We might want to chunk-load ahead of the train just to be safe?

        if (world.getTicks() % 5 != 0) {
            // Only re-check every 5 ticks
            if (!forceQuickUpdates) {
                return;
            }
        } else {
            forceQuickUpdates = false;
        }

        new Simulation(world);
    }
}
