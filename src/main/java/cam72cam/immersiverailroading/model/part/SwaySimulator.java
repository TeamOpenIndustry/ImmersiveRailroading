package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.util.BlockUtil;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.sound.ISound;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SwaySimulator {
    public SwaySimulator() {

    }

    private static class Effect {
        private final EntityMoveableRollingStock stock;
        private final ISound clackFront;
        private final ISound clackRear;
        private double swayMagnitude;
        private double swayImpulse;
        private Vec3i clackFrontPos;
        private Vec3i clackRearPos;

        Effect(EntityMoveableRollingStock stock) {
            this.stock = stock;
            clackFront = stock.getWorld().isServer ? null : stock.createSound(stock.getDefinition().clackFront, false, 30, ConfigSound.SoundCategories.RollingStock::clack);
            clackRear = stock.getWorld().isServer ? null : stock.createSound(stock.getDefinition().clackRear, false, 30, ConfigSound.SoundCategories.RollingStock::clack);
            swayImpulse = 0;
            swayMagnitude = 0;
        }

        public void effects() {

            float adjust = (float) Math.abs(stock.getCurrentSpeed().metric()) / 300;
            float pitch = adjust + 0.7f;
            if (stock.getDefinition().shouldScalePitch()) {
                // TODO this is probably wrong...
                pitch = (float) (pitch/ stock.gauge.scale());
            }
            float volume = 0.01f + adjust;

            volume = Math.min(1, volume * 2);

            Vec3i posFront = new Vec3i(VecUtil.fromWrongYawPitch(stock.getDefinition().getBogeyFront(stock.gauge), stock.getRotationYaw(), stock.getRotationPitch()).add(stock.getPosition()));
            if (BlockUtil.isIRRail(stock.getWorld(), posFront)) {
                TileRailBase rb = stock.getWorld().getBlockEntity(posFront, TileRailBase.class);
                rb = rb != null ? rb.getParentTile() : null;
                if (rb != null && !rb.getPos().equals(clackFrontPos) && rb.clacks()) {
                    if (volume > 0 && clackFront != null) {
                        if (!clackFront.isPlaying() && !clackRear.isPlaying()) {
                            clackFront.setPitch(pitch);
                            clackFront.setVolume(volume);
                            clackFront.play(new Vec3d(posFront));
                        }
                    }
                    clackFrontPos = rb.getPos();
                    if (stock.getWorld().getTicks() % ConfigGraphics.StockSwayChance == 0) {
                        swayImpulse += 7 * rb.getBumpiness();
                        swayImpulse = Math.min(swayImpulse, 20);
                    }
                }
            }
            Vec3i posRear = new Vec3i(VecUtil.fromWrongYawPitch(stock.getDefinition().getBogeyRear(stock.gauge), stock.getRotationYaw(), stock.getRotationPitch()).add(stock.getPosition()));
            if (BlockUtil.isIRRail(stock.getWorld(), posRear)) {
                TileRailBase rb = stock.getWorld().getBlockEntity(posRear, TileRailBase.class);
                rb = rb != null ? rb.getParentTile() : null;
                if (rb != null && !rb.getPos().equals(clackRearPos) && rb.clacks()) {
                    if (volume > 0 && clackRear != null) {
                        if (!clackFront.isPlaying() && !clackRear.isPlaying()) {
                            clackRear.setPitch(pitch);
                            clackRear.setVolume(volume);
                            clackRear.play(new Vec3d(posRear));
                        }
                    }
                    clackRearPos = rb.getPos();
                }
            }

            swayMagnitude -= 0.07;
            double swayMin = stock.getCurrentSpeed().metric() / 300 / 3;
            swayMagnitude = Math.max(swayMagnitude, swayMin);

            if (swayImpulse > 0) {
                swayMagnitude += 0.3;
                swayImpulse -= 0.7;
            }
            swayMagnitude = Math.min(swayMagnitude, 3);
        }

        public double getRollDegrees(float partialTicks) {
            if (Math.abs(stock.getCurrentSpeed().metric() * stock.gauge.scale()) < 4) {
                // don't calculate it
                return 0;
            }

            double sway = Math.cos(Math.toRadians((stock.getTickCount() + partialTicks) * 13)) *
                    swayMagnitude / 5 *
                    stock.getDefinition().getSwayMultiplier() *
                    ConfigGraphics.StockSwayMultiplier;

            double tilt = stock.getDefinition().getTiltMultiplier() * (stock.getPrevRotationYaw() - stock.getRotationYaw()) * (stock.getCurrentSpeed().minecraft() > 0 ? 1 : -1);

            return sway + tilt;
        }

        public void removed() {
            // These should never be null, but for some reason they are
            if (clackFront != null) {
                clackFront.stop();
            }
            if (clackRear != null) {
                clackRear.stop();
            }
        }
    }

    private final Map<UUID, Effect> effects = new HashMap<>();

    public double getRollDegrees(EntityMoveableRollingStock stock, float partialTicks) {
        return effects.computeIfAbsent(stock.getUUID(), uuid -> new Effect(stock)).getRollDegrees(partialTicks);
    }

    public void effects(EntityMoveableRollingStock stock) {
        effects.computeIfAbsent(stock.getUUID(), uuid -> new Effect(stock)).effects();
    }

    public void removed(EntityMoveableRollingStock stock) {
        Effect effect = effects.remove(stock.getUUID());
        if (effect != null) {
            effect.removed();
        }
    }
}
