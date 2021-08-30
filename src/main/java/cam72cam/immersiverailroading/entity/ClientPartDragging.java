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
import java.util.UUID;

public class ClientPartDragging {
    private EntityRollingStock stock = null;
    private Control component = null;
    private Float lastDelta = null;

    public static void register() {
        ClientPartDragging dragger = new ClientPartDragging();
        Mouse.registerDragHandler(dragger::capture);
        ClientEvents.TICK.subscribe(dragger::tick);
        Packet.register(DragPacket::new, PacketDirection.ClientToServer);
    }

    private boolean capture(Player.Hand hand) {
        if (hand == Player.Hand.SECONDARY) {
            this.stock = null;
            Player player = MinecraftClient.getPlayer();
            Vec3d look = player.getLookVector();
            Vec3d start = player.getPositionEyes();

            MinecraftClient.getPlayer().getWorld().getEntities(EntityRollingStock.class).stream()
                    .filter(stock ->
                            stock.getPosition().distanceTo(player.getPositionEyes()) < stock.getDefinition().getLength(stock.gauge)
                    )
                    .flatMap(stock ->
                            stock.getDefinition().getModel().getDraggableComponents().stream().map(c -> Pair.of(stock, c))
                    ).filter(p -> {
                        double padding = 0.05 * p.getLeft().gauge.scale();
                        IBoundingBox bb = p.getRight().getBoundingBox(p.getLeft()).grow(new Vec3d(padding, padding, padding));
                        for (double i = 0; i < 3; i += 0.1) {
                            if (bb.contains(start.add(look.scale(i * p.getLeft().gauge.scale())))) {
                                return true;
                            }
                        }
                        return false;
                    }).min(Comparator.comparingDouble(p -> p.getRight().transform(
                            p.getRight().part.center,
                            p.getLeft()
                    ).distanceTo(start.add(look)))).ifPresent(found -> {
                        this.stock = found.getLeft();
                        this.component = found.getRight();
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
        private double delta;
        @TagField
        private double y;
        @TagField
        private boolean released;

        public DragPacket() {
            super(); // Reflection
        }
        public DragPacket(EntityRollingStock stock, Control type, double delta) {
            this.stockUUID = stock.getUUID();
            this.typeKey = type.part.key;
            this.delta = delta;
        }
        public DragPacket(EntityRollingStock stock, Control type) {
            this(stock, type, 0);
            this.released = true;
        }
        @Override
        protected void handle() {
            EntityRollingStock stock = getWorld().getEntity(stockUUID, EntityRollingStock.class);
            if (released) {
                stock.onDragRelease(stock.getDefinition().getModel().getDraggableComponents().stream().filter(x -> x.part.key.equals(typeKey)).findFirst().get());
            } else {
                stock.onDrag(stock.getDefinition().getModel().getDraggableComponents().stream().filter(x -> x.part.key.equals(typeKey)).findFirst().get(), delta);
            }
        }
    }

    private void tick() {
        if (stock != null) {
            if (Mouse.getDrag() == null) {
                component.stopClientDragging();
                new DragPacket(stock, component).sendToServer();
                stock = null;
                component = null;
                lastDelta = null;
                return;
            }

            if (component.toggle) {
                return;
            }

            float delta = component.clientMovementDelta(MinecraftClient.getPlayer(), stock);
            if (lastDelta != null && Math.abs(lastDelta - delta) < 0.001) {
                return;
            }
            //stock.onDrag(component, delta.x / 1000, delta.y / 1000);
            new DragPacket(stock, component, delta).sendToServer();
            lastDelta = delta;
        }
    }
}
