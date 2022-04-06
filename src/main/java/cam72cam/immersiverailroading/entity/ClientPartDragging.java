package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.model.part.Control;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.input.Mouse;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.net.Packet;
import cam72cam.mod.net.PacketDirection;
import cam72cam.mod.serialization.TagField;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

public class ClientPartDragging {
    private EntityRollingStock stock = null;
    private Control<?> component = null;
    private Float lastValue = null;

    public static void register() {
        ClientPartDragging dragger = new ClientPartDragging();
        Mouse.registerDragHandler(dragger::capture);
        ClientEvents.TICK.subscribe(dragger::tick);
        Packet.register(DragPacket::new, PacketDirection.ClientToServer);
    }

    private boolean capture(Player.Hand hand) {

        if (hand == Player.Hand.SECONDARY && MinecraftClient.isReady()) {
            this.stock = null;
            Player player = MinecraftClient.getPlayer();
            Vec3d look = player.getLookVector();
            Vec3d start = player.getPositionEyes();

            MinecraftClient.getPlayer().getWorld().getEntities(EntityRollingStock.class).stream()
                    .filter(stock ->
                            stock.getPosition().distanceTo(player.getPositionEyes()) < stock.getDefinition().getLength(stock.gauge)
                    )
                    .flatMap(stock ->
                            stock.getDefinition().getModel().getDraggable().stream().map(c -> Pair.of(stock, c))
                    ).map(p -> {
                        double padding = 0.2 * p.getLeft().gauge.scale();
                        Double min = null;
                        Vec3d center = p.getRight().center(p.getLeft());
                        IBoundingBox bb = p.getRight().getBoundingBox(p.getLeft()).grow(new Vec3d(padding, padding, padding));
                        for (double i = 0; i < 3; i += 0.1 * p.getLeft().gauge.scale()) {
                            Vec3d cast = start.add(look.scale(i));
                            if (bb.contains(cast)) {
                                double dist = cast.distanceTo(center);
                                min = min == null ? dist : Math.min(dist, min);
                            }
                        }
                        return min != null ? Pair.of(min, p) : null;
                    }).filter(Objects::nonNull)
                    .min(Comparator.comparingDouble(Pair::getLeft))
                    .map(Pair::getRight)
                    .ifPresent(found -> {
                        this.stock = found.getLeft();
                        this.component = found.getRight();
                        new DragPacket(stock, component, true, 0, false).sendToServer();
                    });
            return stock == null;
        }
        return true;
    }

    public static class DragPacket extends Packet {
        @TagField
        private UUID stockUUID;
        @TagField
        private String typeKey;
        @TagField
        private double newValue;
        @TagField
        private boolean start;
        @TagField
        private boolean released;

        public DragPacket() {
            super(); // Reflection
        }
        public DragPacket(EntityRollingStock stock, Control<?> type, boolean start, double newValue, boolean released) {
            this.stockUUID = stock.getUUID();
            this.typeKey = type.part.key;
            this.start = start;
            this.newValue = newValue;
            this.released = released;
        }
        @Override
        protected void handle() {
            EntityRollingStock stock = getWorld().getEntity(stockUUID, EntityRollingStock.class);
            Control<?> control = stock.getDefinition().getModel().getDraggable().stream().filter(x -> x.part.key.equals(typeKey)).findFirst().get();
            if (!stock.playerCanDrag(getPlayer(), control)) {
                return;
            }
            if (start) {
                stock.onDragStart(control);
            } else if (released) {
                stock.onDragRelease(control);
            } else {
                stock.onDrag(control, newValue);
            }
        }
    }

    private void tick() {
        if (stock != null && MinecraftClient.isReady()) {
            if (Mouse.getDrag() == null) {
                component.stopClientDragging();
                new DragPacket(stock, component, false, 0, true).sendToServer();
                stock = null;
                component = null;
                lastValue = null;
                return;
            }

            if (component.toggle) {
                return;
            }

            float newValue = component.clientMovementDelta(MinecraftClient.getPlayer(), stock) + stock.getControlPosition(component);
            if (lastValue != null && Math.abs(lastValue - newValue) < 0.001) {
                return;
            }

            // Server will override this, but for now it allows the client to see the active changes.
            stock.setControlPosition(component, newValue);

            new DragPacket(stock, component, false, newValue, false).sendToServer();
            lastValue = newValue;
        }
    }
}
