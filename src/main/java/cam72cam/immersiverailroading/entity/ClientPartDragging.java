package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.model.part.Control;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.input.Mouse;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.net.Packet;
import cam72cam.mod.net.PacketDirection;
import cam72cam.mod.serialization.TagField;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ClientPartDragging {
    private EntityRollingStock stock = null;
    private Control component = null;
    private Vec3d lastDelta = null;

    public static void register() {
        ClientPartDragging dragger = new ClientPartDragging();
        Mouse.registerDragHandler(dragger::capture);
        ClientEvents.TICK.subscribe(dragger::tick);
        Packet.register(DragPacket::new, PacketDirection.ClientToServer);
    }

    private boolean capture(Player.Hand hand) {
        if (hand == Player.Hand.SECONDARY) {
            if (MinecraftClient.getEntityMouseOver() instanceof EntityRollingStock) {
                stock = (EntityRollingStock) MinecraftClient.getEntityMouseOver();
                List<Control> targets = stock.getDefinition().getModel().getDraggableComponents();
                Player player = MinecraftClient.getPlayer();

                Vec3d look = VecUtil.rotateWrongYaw(player.getLookVector(), - stock.getRotationYaw());

                Vec3d starta = VecUtil.rotateWrongYaw(stock.getPosition().subtract(player.getPositionEyes()), 180-stock.getRotationYaw());
                Vec3d start = starta.add(0, -starta.y + player.getPositionEyes().y - stock.getPosition().y, 0);
                double padding = 0.05 * stock.gauge.scale();
                Optional<Control> found = targets.stream().filter(g -> {
                    IBoundingBox bb = g.getBoundingBox(stock).grow(new Vec3d(padding, padding, padding));
                    for (double i = 0; i < 3; i += 0.1) {
                        if (bb.contains(start.add(look.scale(i * stock.gauge.scale())))) {
                            return true;
                        }
                    }
                    return false;
                }).min(Comparator.comparingDouble(g -> g.transform(
                        g.part.min.add(g.part.max).scale(0.5f),
                        stock
                ).distanceTo(start)));
                if (found.isPresent()) {
                    component = found.get();
                    return false;
                } else {
                    stock = null;
                }
            }
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
            Vec3d delta = Mouse.getDrag();
            if (delta == null) {
                component.stopClientDragging();
                new DragPacket(stock, component).sendToServer();
                stock = null;
                component = null;
                lastDelta = null;
                return;
            }
            if (lastDelta == null) {
                lastDelta = delta;
                return;
            }
            if (lastDelta.distanceTo(delta) < 10) {
                return;
            }
            //stock.onDrag(component, delta.x / 1000, delta.y / 1000);
            new DragPacket(stock, component, component.clientMovementDelta((delta.x - lastDelta.x) / 1000, (delta.y - lastDelta.y) / 1000, stock)).sendToServer();
            lastDelta = delta;
        }
    }
}
