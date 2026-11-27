package cam72cam.immersiverailroading.entity.physics;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock;
import cam72cam.immersiverailroading.entity.physics.chrono.ChronoState;
import cam72cam.immersiverailroading.entity.physics.chrono.ServerChronoState;
import cam72cam.immersiverailroading.net.MRSSyncPacket;
import cam72cam.immersiverailroading.physics.TickPos;
import cam72cam.immersiverailroading.track.graph.WorldData;
import cam72cam.mod.entity.CustomEntity;
import cam72cam.mod.entity.EntityPool;
import cam72cam.mod.entity.ModdedEntity;
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

    // How long a coupled car may stay absent before its couplers are released.
    private static final int MISSING_STATE_GRACE_TICKS = 600;
    // Partner uuids that have never produced a state in this world -> first tick noticed missing.
    private static final Map<World, Map<UUID, Long>> UNSEEN_MISSING = new WeakHashMap<>();

    /*
     * states is transient while positions is serialized.  If the same carrier object survives a
     * reload and load() replaces positions from NBT, the two lists can end up on different
     * timelines: matching tickIDs with bodies tens of blocks apart.  Drop the transient states so
     * they get rebuilt from the persisted positions.
     */
    private static boolean statesMatchPositions(EntityCoupleableRollingStock stock) {
        if (stock.states.isEmpty()) {
            return true;
        }
        if (stock.positions.isEmpty()) {
            return false;
        }
        for (SimulationState state : stock.states) {
            TickPos matching = null;
            for (TickPos position : stock.positions) {
                if (position.tickID == state.tickID) {
                    matching = position;
                    break;
                }
            }
            if (matching == null || matching.position == null || state.position == null) {
                return false;
            }
            if (matching.position.distanceToSquared(state.position) > 0.0001) {
                return false;
            }
        }
        return true;
    }

    /*
     * True when a coupled uuid has never produced a state in this world and was first reported
     * missing less than MISSING_STATE_GRACE_TICKS ago.  This is the "first pass after a world or
     * chunk load" case, where the partner entity may not be registered with the world yet.
     */
    private boolean unseenWithinGrace(UUID uuid) {
        Map<UUID, Long> unseen = UNSEEN_MISSING.computeIfAbsent(world, k -> new HashMap<>());
        long now = world.getTicks();
        Long first = unseen.get(uuid);
        if (first == null) {
            unseen.put(uuid, now);
            return true;
        }
        if (now - first > MISSING_STATE_GRACE_TICKS) {
            unseen.remove(uuid);
            return false;
        }
        return true;
    }

    public Simulation(World world) {
        calculatedStates = keptStates = restStates = 0;
        long startTimeMs = System.currentTimeMillis();
        this.world = world;
        this.startTickID = ((ServerChronoState)ChronoState.getState(world)).getServerTickID();
        // Anchor the persisted TickPos timeline of this session before any stock is rebuilt.
        SimulationState.noteSessionStart(world, startTickID);

        stateMaps = new ArrayList<>();
        blocksAlreadyBroken = new ArrayList<>();

        for (int i = 0; i < Config.ConfigDebug.physicsFutureTicks; i++) {
            stateMaps.add(new HashMap<>());
        }

        for (int i = 0; i < stateMaps.size()-1; i++) {
            simulateTick(i);
        }

        long now = world.getTicks();
        boolean lowFreqDue = now % (Config.ConfigDebug.physicsFutureTicks / 2) == 0;
        boolean anyMovingTable = loaded != null && loaded.stream()
                .anyMatch(s -> now < s.onMovingTableUntilTick);

        List<Player> players = (lowFreqDue || anyMovingTable) ? world.getEntities(Player.class) : null;
        double syncDistanceSq = ImmersiveRailroading.ENTITY_SYNC_DISTANCE * ImmersiveRailroading.ENTITY_SYNC_DISTANCE;

        // Apply new states
        for (EntityCoupleableRollingStock stock : loaded) {
            stock.states = stateMaps.stream().map(m -> m.get(stock.getUUID())).filter(Objects::nonNull).collect(Collectors.toList());
            for (SimulationState state : stock.states) {
                state.dirty = false;
            }
            stock.positions = stock.states.stream().map(TickPos::new).collect(Collectors.toList());

            if (players == null) {
                continue;
            }
            boolean onMovingTable = now < stock.onMovingTableUntilTick;
            if ((lowFreqDue || onMovingTable)
                    && players.stream().anyMatch(player -> player.getPosition().distanceToSquared(stock.getPosition()) < syncDistanceSq)) {
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
            loaded = new ArrayList<>();
            Set<UUID> pooledUuids = new HashSet<>();
            for (ModdedEntity m : EntityPool.get(world).getAllEntities()) {
                CustomEntity self = m.getSelf();
                if (self instanceof EntityCoupleableRollingStock stock && !stock.isDead()) {
                    loaded.add(stock);
                    pooledUuids.add(stock.getUUID());
                }
            }
            for (EntityCoupleableRollingStock stock : world.getEntities(EntityCoupleableRollingStock.class)) {
                if (!pooledUuids.contains(stock.getUUID()) && !stock.isDead()) {
                    loaded.add(stock);
                }
            }

            boolean newChunksLoaded = lastCount != loaded.size();

            for (EntityCoupleableRollingStock stock : loaded) {

                if (!statesMatchPositions(stock)) {
                    stock.states.clear();
                }

                if (!stateMap.containsKey(stock.getUUID())) {
                    if (stock.states.isEmpty()) {
                        /*
                         * The transient states list is lost whenever the carrier entity is
                         * re-created (1.21 destroys and recreates it on chunk unload/reload).
                         * The persisted TickPos list survives, so rebuild from it instead of
                         * trusting the re-created entity's NBT/zero position.
                         */
                        SimulationState rebuilt = SimulationState.forTick(stock, tickID);
                        rebuilt.update(stock);
                        stateMap.put(stock.getUUID(), rebuilt);
                    } else {
                        for (SimulationState state : stock.states) {
                            int stateIteration = state.tickID - tickID;
                            if (stateIteration >= 0 && stateIteration < stateMaps.size()) {
                                state.update(stock);
                                stateMaps.get(stateIteration).put(stock.getUUID(), state);
                            }
                        }

                        if (!stateMap.containsKey(stock.getUUID())) {
                            // Stock placed right after this pass started.
                            stateMap.put(stock.getUUID(), SimulationState.forTick(stock, tickID));
                        }
                    }
                }

                SimulationState state = stateMap.get(stock.getUUID());


                // Don't need to load
                if (state.atRest && !state.dirty) {
                    continue;
                }

                boolean offline = !stock.isAddedToChunk();
                if (!offline || Config.ConfigDebug.offlineVehicleLoadChunks) {
                    world.keepLoaded(new Vec3i(state.position));

                    if (state.consist.positions != null) {
                        for (Vec3i pos : state.consist.positions) {
                            if (!world.isBlockLoaded(pos)) {
                                world.getBlock(pos);
                                newChunksLoaded = true;
                            }
                        }
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
                    /*
                     * A missing state in this pass does NOT mean the car was removed.  In 1.21
                     * the carrier entity is destroyed/re-created when its chunk unloads (and it
                     * may live in the offline entity pool), so the world registry can transiently
                     * forget it.  Treating "no state this pass" as "removed" is what tears a
                     * consist into pieces when a player leaves and re-enters the area, so rebuild
                     * the partner from its persisted TickPos and only release the coupler once
                     * there is really no stock left.
                     */
                    EntityCoupleableRollingStock otherStock = world.getEntity(otherID, EntityCoupleableRollingStock.class);
                    if (otherStock == null && Config.ConfigDebug.enableEntityPool) {
                        ModdedEntity pooled = EntityPool.get(world).getEntity(otherID);
                        if (pooled != null) {
                            if (pooled.getSelf() instanceof EntityCoupleableRollingStock) {
                                otherStock = (EntityCoupleableRollingStock) pooled.getSelf();
                            } else {
                                // Still being restored into a live entity; do not drop the link.
                                continue;
                            }
                        }
                    }

                    if (otherStock != null && !otherStock.isDead()) {
                        EntityCoupleableRollingStock partner = otherStock;
                        Map<UUID, Long> unseen = UNSEEN_MISSING.get(world);
                        if (unseen != null) {
                            unseen.remove(otherID);
                        }
                        SimulationState rebuilt = SimulationState.forTick(partner, state.tickID);
                        rebuilt.update(partner);
                        rebuilt.dirty = true;
                        stateMap.put(otherID, rebuilt);
                        if (loaded.stream().noneMatch(x -> x.getUUID().equals(partner.getUUID()))) {
                            loaded.add(partner);
                        }
                        continue;
                    }

                    EntityCoupleableRollingStock meStock = loaded.stream()
                            .filter(x -> x.getUUID().equals(myID)).findFirst()
                            .orElseGet(() -> world.getEntity(myID, EntityCoupleableRollingStock.class));
                    Vec3i otherPos = meStock == null ? null
                            : (isMyCouplerFront ? meStock.lastKnownFront : meStock.lastKnownRear);

                    if (otherPos == null) {
                        // No state, no entity and no last known position: most likely a car whose
                        // chunk has not been streamed back in yet, not a removed coupler.
                        continue;
                    }

                    if (!world.isBlockLoaded(otherPos)) {
                        // Other location is not loaded, we must not need to do this check.
                        continue;
                    }

                    if (unseenWithinGrace(otherID)) {
                        // First pass after a load: the partner may simply not be registered yet.
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

        Map<UUID, WorldData.VehicleRef> refs = new HashMap<>();
        for (EntityCoupleableRollingStock stock : loaded) {
            refs.put(stock.getUUID(), new WorldData.VehicleRef(stock.getPosition(), stock.getVelocity(), stock.lastFrontEdge, stock.lastRearEdge));
        }
        WorldData.get(world).updateVehicleRefs(refs);

        long totalTimeMs = System.currentTimeMillis() - startTimeMs;
        if (totalTimeMs > Config.ConfigDebug.physicsWarnThresholdMs) {
            ImmersiveRailroading.warn("Calculating Immersive Railroading Physics Iteration took %sms (%s, %s, %s)", totalTimeMs, calculatedStates, restStates, keptStates);
        }
    }




    private static final Map<World, Boolean> LAST_OFF_WORLD_PATHING = new WeakHashMap<>();

    public static void simulate(World world) {
        // 100KM/h ~= 28m/s which means non-loaded stationary stock may be phased through at that speed
        // I'm OK with that for now
        // We might want to chunk-load ahead of the train just to be safe?

        boolean current = Config.ConfigDebug.offWorldPathing;
        Boolean last = LAST_OFF_WORLD_PATHING.put(world, current);

        // off -> non-off, clear all topo for stocks
        if (last != null && last && !current) {
            clearTopology(world);
        }

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

    public static double calculateRoll(double frontRoll, double rearRoll) {//TODO: multi bogey support?
        return (frontRoll + rearRoll) / 2;
    }

    private static void clearTopology(World world) {
        Set<EntityCoupleableRollingStock> all =
                new HashSet<>(world.getEntities(EntityCoupleableRollingStock.class));

        EntityPool.get(world).getAllEntities().stream()
                .map(ModdedEntity::getSelf)
                .filter(Objects::nonNull)
                .filter(s -> s instanceof EntityCoupleableRollingStock)
                .map(s -> (EntityCoupleableRollingStock) s)
                .forEach(all::add);

        for (EntityCoupleableRollingStock stock : all) {
            stock.lastFrontEdge = null;
            stock.lastFrontT = -1;
            stock.lastRearEdge = null;
            stock.lastRearT = -1;

            SimulationState state = stock.getCurrentState();
            if (state != null) {
                state.lastFrontEdge = null;
                state.lastFrontT = -1;
                state.lastRearEdge = null;
                state.lastRearT = -1;
            }
        }
    }
}
