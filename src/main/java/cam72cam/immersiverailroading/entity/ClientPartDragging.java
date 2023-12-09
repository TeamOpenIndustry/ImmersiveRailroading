package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.part.Control;
import cam72cam.immersiverailroading.model.part.Interactable;
import cam72cam.immersiverailroading.model.part.Seat;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.input.Mouse;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.net.Packet;
import cam72cam.mod.net.PacketDirection;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.world.World;

import java.util.UUID;

public class ClientPartDragging {
    private EntityRollingStock interactingStock = null;
    private Control<?> interactingControl = null;
    private Float lastValue = null;

    private EntityRollingStock targetStock = null;
    private Interactable<?> targetInteractable = null;

    public static void register() {
        ClientPartDragging dragger = new ClientPartDragging();
        Mouse.registerDragHandler(dragger::capture);
        ClientEvents.TICK.subscribe(dragger::tick);
        ClientEvents.SCROLL.subscribe(dragger::scroll);
        Packet.register(DragPacket::new, PacketDirection.ClientToServer);
    }

    private boolean scroll(double scroll) {
        if (MinecraftClient.isReady() && targetInteractable != null) {
            if (targetInteractable instanceof Control) {
                float value = targetStock.getControlPosition((Control<?>) targetInteractable);
                // Same as GuiBuilder
                if (targetStock instanceof LocomotiveDiesel && targetInteractable.part.type == ModelComponentType.REVERSER_X) {
                    value += scroll > 0 ? 0.5 : -0.5;
                } else {
                    value += scroll / -50 * ConfigGraphics.ScrollSpeed;
                }

                targetStock.setControlPosition((Control<?>) targetInteractable, value);
                targetStock.onDragRelease((Control<?>) targetInteractable);
                new DragPacket(targetStock, (Control<?>) targetInteractable, true, value, true).sendToServer();
                return false;
            }
        }
        return true;
    }

    private boolean capture(Player.Hand hand) {
        if (hand == Player.Hand.SECONDARY && MinecraftClient.isReady() && !MinecraftClient.getPlayer().isCrouching() && targetInteractable != null) {
            if (targetInteractable instanceof Control) {
                this.interactingStock = targetStock;
                this.interactingControl = (Control<?>) targetInteractable;
                new DragPacket(interactingStock, interactingControl, true, 0, false).sendToServer();
            }
            if (targetInteractable instanceof Seat) {
                new SeatPacket((EntityRidableRollingStock) targetStock, (Seat<?>)targetInteractable).sendToServer();
            }
            return false;
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
            if (start && released) {
                stock.onDragStart(control);
                stock.onDrag(control, newValue);
                stock.onDragRelease(control);
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

    public static class SeatPacket extends Packet {
        @TagField
        private EntityRidableRollingStock stock;

        @TagField
        private String seat;

        public SeatPacket() {
            super(); // Reflection
        }
        public SeatPacket(EntityRidableRollingStock stock, Seat<?> seat) {
            this.stock = stock;
            this.seat = seat.part.key;
        }

        @Override
        protected void handle() {
            if (stock != null) {
                stock.onSeatClick(seat, getPlayer());
            }
        }
    }

    private void tick() {
        if (!MinecraftClient.isReady()) {
            return;
        }

        if (interactingStock != null) {
            interactingControl.lookedAt = interactingStock.getWorld().getTicks();
            if (Mouse.getDrag() == null) {
                interactingControl.stopClientDragging();
                new DragPacket(interactingStock, interactingControl, false, 0, true).sendToServer();
                interactingStock = null;
                interactingControl = null;
                lastValue = null;
                return;
            }

            if (interactingControl.toggle) {
                return;
            }

            float newValue = interactingControl.clientMovementDelta(MinecraftClient.getPlayer(), interactingStock) + interactingStock.getControlPosition(interactingControl);
            if (lastValue != null && Math.abs(lastValue - newValue) < 0.001) {
                return;
            }

            // Server will override this, but for now it allows the client to see the active changes.
            interactingStock.setControlPosition(interactingControl, newValue);

            new DragPacket(interactingStock, interactingControl, false, newValue, false).sendToServer();
            lastValue = newValue;
        } else {
            targetStock = null;
            targetInteractable = null;

            Player player = MinecraftClient.getPlayer();
            Vec3d look = player.getLookVector();
            Vec3d start = player.getPositionEyes();
            World world = player.getWorld();

            Double min = null;

            for (EntityRollingStock stock : world.getEntities(EntityRollingStock.class)) {
                if (stock.getPosition().distanceToSquared(player.getPosition()) > stock.getDefinition().getLength(stock.gauge) * stock.getDefinition().getLength(stock.gauge)) {
                    continue;
                }

                double padding = 0.05 * stock.gauge.scale();
                for (Interactable<?> interactable : stock.getDefinition().getModel().getInteractable()) {
                    if (interactable.disabled()) {
                        continue;
                    }
                    IBoundingBox bb = interactable.getBoundingBox(stock).grow(new Vec3d(padding, padding, padding));
                    for (double i = 0.125; i < 3; i += 0.05 * stock.gauge.scale()) {
                        Vec3d cast = start.add(look.scale(i));
                        if (bb.contains(cast)) {
                            interactable.lookedAt = MinecraftClient.getPlayer().getWorld().getTicks();
                            // This is a weird check, but it seems to work
                            double dist = i * interactable.center(stock).distanceTo(cast);
                            if (min == null || min > dist) {
                                min = dist;
                                targetStock = stock;
                                targetInteractable = interactable;
                            }
                        }
                    }
                }
            }
        }
    }
}
