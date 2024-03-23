package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.Permissions;
import cam72cam.immersiverailroading.model.part.Control;
import cam72cam.immersiverailroading.registry.LocomotiveDieselDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveElectricDefinition;
import cam72cam.immersiverailroading.util.BurnUtil;
import cam72cam.immersiverailroading.util.FluidQuantity;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.fluid.FluidStack;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.text.PlayerMessage;

import javax.annotation.Nullable;
import java.util.List;
import java.util.OptionalDouble;

public class LocomotiveElectric extends Locomotive {
    private float relativeRPM;
    private int turnOnOffDelay = 0;
    @TagSync
    @TagField("TURNED_ON")
    private boolean turnedOn = false;
    @TagSync
    @TagField("IS_POWERED")
    private boolean isPowered;
    private int powerCooldown;
    private int throttleCooldown;
    private int reverserCooldown;

    public LocomotiveElectric() {
    }

    public boolean isPowered() {
        return isPowered;
    }

    @Override
    public ClickResult onClick(Player player, Player.Hand hand) {
        player.sendMessage(PlayerMessage.direct(String.valueOf(this.isPowered())));
        return super.onClick(player, hand);
    }

    public void setPowered(boolean powered) {
        isPowered = powered;
    }
    public int getInventoryWidth() {
        return this.getDefinition().isCabCar() ? 0 : 2;
    }

    public void setTurnedOn(boolean value) {
        this.turnedOn = value;
        this.setControlPositions(ModelComponentType.ENGINE_START_X, this.turnedOn ? 1.0F : 0.0F);
    }

    public void setPowerCooldown(int powerCooldown) {
        this.powerCooldown = powerCooldown;
    }

    public boolean isTurnedOn() {
        return this.turnedOn;
    }

    public boolean isRunning() {
        if (!Config.isFuelRequired(this.gauge)) {
            return this.isTurnedOn();
        } else {
            return this.isTurnedOn() && this.isPowered();
        }
    }

    public LocomotiveElectricDefinition getDefinition() {
        return (LocomotiveElectricDefinition)super.getDefinition(LocomotiveElectricDefinition.class);
    }

    public boolean openGui(Player player) {
        if (!this.getDefinition().isCabCar() && player.hasPermission(Permissions.LOCOMOTIVE_CONTROL)) {
            GuiTypes.ELECTRIC_LOCOMOTIVE.open(player, this);
            return true;
        } else {
            return false;
        }
    }

    public void handleKeyPress(Player source, KeyTypes key, boolean disableIndependentThrottle) {
        switch (key) {
            case START_STOP_ENGINE:
                if (this.turnOnOffDelay == 0) {
                    this.turnOnOffDelay = 10;
                    this.setTurnedOn(!this.isTurnedOn());
                }
                break;
            case REVERSER_UP:
            case REVERSER_ZERO:
            case REVERSER_DOWN:
                if (this.reverserCooldown > 0) {
                    return;
                }

                this.reverserCooldown = 3;
                super.handleKeyPress(source, key, disableIndependentThrottle);
                break;
            case THROTTLE_UP:
            case THROTTLE_ZERO:
            case THROTTLE_DOWN:
                if (this.throttleCooldown > 0) {
                    return;
                }

                this.throttleCooldown = 2;
                super.handleKeyPress(source, key, disableIndependentThrottle);
                break;
            default:
                super.handleKeyPress(source, key, disableIndependentThrottle);
        }

    }

    public boolean providesElectricalPower() {
        return this.isRunning();
    }

    protected float getReverserDelta() {
        return 0.51F;
    }

    public void setThrottle(float newThrottle) {
        int notches = this.getDefinition().getThrottleNotches();
        if (newThrottle > this.getThrottle()) {
            super.setThrottle((float)(Math.ceil((double)(newThrottle * (float)notches)) / (double)notches));
        } else {
            super.setThrottle((float)(Math.floor((double)(newThrottle * (float)notches)) / (double)notches));
        }
    }

    public void setReverser(float newReverser) {
        super.setReverser((float)Math.round(newReverser));
    }

    public double getAppliedTractiveEffort(Speed speed) {
        if (this.isRunning() && !Config.isFuelRequired(this.gauge)) {
            double maxPower_W = (double)this.getDefinition().getHorsePower(this.gauge) * 745.7;
            double efficiency = 0.82;
            double speed_M_S = Math.abs(speed.metric()) / 3.6;
            double maxPowerAtSpeed = maxPower_W * efficiency / Math.max(0.001, speed_M_S);
            double applied = maxPowerAtSpeed * (double)this.relativeRPM * (double)this.getReverser();
            if (this.getDefinition().hasDynamicTractionControl) {
                double max = this.getStaticTractiveEffort(speed);
                if (Math.abs(applied) > max) {
                    return Math.copySign(max, applied) * 0.95;
                }
            }

            return applied;
        } else {
            return 0.0;
        }
    }

    public void onTick() {
        super.onTick();
        if (this.turnOnOffDelay > 0) {
            --this.turnOnOffDelay;
        }

        float absThrottle = Math.abs(this.getThrottle());
        if (this.relativeRPM > absThrottle) {
            this.relativeRPM -= Math.min(0.01F, this.relativeRPM - absThrottle);
        } else if (this.relativeRPM < absThrottle) {
            this.relativeRPM += Math.min(0.01F, absThrottle - this.relativeRPM);
        }

        if (!this.getWorld().isClient) {
            OptionalDouble control = this.getDefinition().getModel().getControls().stream().filter((x) -> {
                return x.part.type == ModelComponentType.HORN_CONTROL_X;
            }).mapToDouble(this::getControlPosition).max();
            if (control.isPresent() && control.getAsDouble() > 0.0) {
                this.setHorn(10, this.hornPlayer);
            }

//            float engineTemperature = this.getEngineTemperature();
//            float heatUpSpeed = 0.0029167F * (float) Config.ConfigBalance.dieselLocoHeatTimeScale / 1.7F;
//            float ambientDelta = engineTemperature - this.ambientTemperature();
//            float coolDownSpeed = heatUpSpeed * Math.copySign((float)Math.pow((double)(ambientDelta / 130.0F), 2.0), ambientDelta);
//            if (this.throttleCooldown > 0) {
//                --this.throttleCooldown;
//            }
//
//            if (this.reverserCooldown > 0) {
//                --this.reverserCooldown;
//            }
//
//            engineTemperature -= coolDownSpeed;
//            if (this.getLiquidAmount() > 0 && this.isRunning()) {
//                float consumption = Math.abs(this.getThrottle()) + 0.05F;
//                float burnTime = (float) BurnUtil.getBurnTime(this.getLiquid());
//                if (burnTime == 0.0F) {
//                    burnTime = 200.0F;
//                }
//
//                burnTime *= (float)this.getDefinition().getFuelEfficiency() / 100.0F;
//                burnTime *= (float) Config.ConfigBalance.locoDieselFuelEfficiency / 100.0F;
//                burnTime *= 10.0F;
//
//                while(this.internalBurn < 0.0F && this.getLiquidAmount() > 0) {
//                    this.internalBurn += burnTime;
//                    this.theTank.drain(new FluidStack(this.theTank.getContents().getFluid(), 1), false);
//                }
//
//                consumption *= 100.0F;
//                consumption = (float)((double)consumption * this.gauge.scale());
//                this.internalBurn -= consumption;
//                engineTemperature += heatUpSpeed * (Math.abs(this.getThrottle()) + 0.2F);
//                if (engineTemperature > 150.0F) {
//                    engineTemperature = 150.0F;
//                    this.setEngineOverheated(true);
//                }
//            }
//
//            if (engineTemperature < 100.0F && this.isEngineOverheated()) {
//                this.setEngineOverheated(false);
//            }
//
//            this.setEngineTemperature(engineTemperature);
            this.powerCooldown--;
            if(this.powerCooldown < 0){
                this.powerCooldown = 0;
                isPowered = false;
            }else if(!isPowered){
                isPowered = true;
            }
        }
    }

    @Override
    public FluidQuantity getTankCapacity() {
        return FluidQuantity.ZERO;
    }

    @Nullable
    @Override
    public List<Fluid> getFluidFilter() {
        return null;
    }

    public void onDissassemble() {
        super.onDissassemble();
//        this.setEngineTemperature(this.ambientTemperature());
//        this.setEngineOverheated(false);
        this.setTurnedOn(false);
    }

    public float getRelativeRPM() {
        return this.relativeRPM;
    }

    public void onDragRelease(Control<?> component) {
        super.onDragRelease(component);
        if (component.part.type == ModelComponentType.ENGINE_START_X) {
            this.turnedOn = this.getDefinition().getModel().getControls().stream().filter((c) -> {
                return c.part.type == ModelComponentType.ENGINE_START_X;
            }).allMatch((c) -> {
                return this.getControlPosition(c) == 1.0F;
            });
        }

        if (component.part.type == ModelComponentType.REVERSER_X) {
            this.setControlPositions(ModelComponentType.REVERSER_X, this.getReverser() / -2.0F + 0.5F);
        }

    }
}
