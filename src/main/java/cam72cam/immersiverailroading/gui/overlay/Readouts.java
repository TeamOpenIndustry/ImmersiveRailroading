package cam72cam.immersiverailroading.gui.overlay;

import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.model.LocomotiveModel;
import cam72cam.immersiverailroading.model.StockModel;

public enum Readouts {
    LIQUID,
    SPEED,
    TEMPERATURE,
    BOILER_PRESSURE,
    THROTTLE,
    REVERSER,
    TRAIN_BRAKE,
    TRAIN_BRAKE_LEVER,
    INDEPENDENT_BRAKE,
    BRAKE_PRESSURE,
    COUPLER_FRONT,
    COUPLER_REAR,
    COUPLED_FRONT,
    COUPLED_REAR,
    COUPLER_SLACK_FRONT,
    COUPLER_SLACK_REAR,
    BELL,
    WHISTLE,
    HORN,
    ENGINE,
    FRONT_BOGEY_ANGLE,
    REAR_BOGEY_ANGLE,
    FRONT_LOCOMOTIVE_ANGLE,
    REAR_LOCOMOTIVE_ANGLE,
    CYLINDER_DRAIN,
    CARGO_FILL,
    ENGINE_RPM,
    TENDER_FEED,
    ;

    public float getValue(EntityRollingStock stock) {
        return getValue(stock, 0);
    }

    public float getValue(EntityRollingStock stock, float lever) {
        return switch (this) {
            case LIQUID ->
                stock instanceof FreightTank tank ? tank.getPercentLiquidFull() / 100f : 0;
            case SPEED -> {
                double maxSpeed = (stock instanceof Locomotive loco ? loco.getDefinition().getMaxSpeed(stock.gauge).metric() : 0);
                if (maxSpeed == 0) {
                    maxSpeed = 200;
                }
                yield stock instanceof EntityMoveableRollingStock moveable ? (float) Math.abs(moveable.getCurrentSpeed().metric() / maxSpeed) : 0;
            }
            case TEMPERATURE -> {
                if (stock instanceof LocomotiveSteam steam) {
                    yield steam.getBoilerTemperature() / 100f;
                }
                if (stock instanceof LocomotiveDiesel diesel) {
                    yield diesel.getEngineTemperature() / 150f;
                }
                yield 0;
            }
            case BOILER_PRESSURE ->
                stock instanceof LocomotiveSteam steam ? steam.getBoilerPressure() / steam.getDefinition().getMaxPSI(stock.gauge) : 0;
            case THROTTLE ->
                stock instanceof Locomotive loco ? loco.getThrottle() : 0;
            case REVERSER ->
                stock instanceof Locomotive loco ? (loco.getReverser() + 1) / 2 : 0;
            case TRAIN_BRAKE ->
                stock instanceof Locomotive loco ? loco.getTrainBrake() : 0;
            case TRAIN_BRAKE_LEVER ->
                stock.getDefinition().isLinearBrakeControl() ? TRAIN_BRAKE.getValue(stock) : lever;
            case INDEPENDENT_BRAKE ->
                stock instanceof EntityMoveableRollingStock moveable ? moveable.getIndependentBrake() : 0;
            case BRAKE_PRESSURE ->
                stock instanceof EntityMoveableRollingStock moveable ? moveable.getBrakePressure() : 0;
            case COUPLER_FRONT ->
                stock instanceof EntityCoupleableRollingStock coupleable && coupleable.isCouplerEngaged(CouplerType.FRONT) ? 1 : 0;
            case COUPLER_REAR ->
                stock instanceof EntityCoupleableRollingStock coupleable && coupleable.isCouplerEngaged(CouplerType.BACK) ? 1 : 0;
            case COUPLED_FRONT ->
                stock instanceof EntityCoupleableRollingStock coupleable && coupleable.isCoupled(CouplerType.FRONT) && coupleable.isCouplerEngaged(CouplerType.FRONT) ? 1 : 0;
            case COUPLED_REAR ->
                stock instanceof EntityCoupleableRollingStock coupleable && coupleable.isCoupled(CouplerType.BACK) && coupleable.isCouplerEngaged(CouplerType.BACK) ? 1 : 0;
            case COUPLER_SLACK_FRONT ->
                stock instanceof EntityCoupleableRollingStock coupleable ? coupleable.slackFrontPercent : 0;
            case COUPLER_SLACK_REAR ->
                stock instanceof EntityCoupleableRollingStock coupleable ? coupleable.slackRearPercent : 0;
            case BELL ->
                stock instanceof Locomotive loco && loco.getBell() > 0 ? 1 : 0;
            case WHISTLE, HORN ->
                stock instanceof Locomotive loco ? loco.hornPull : 0;
            case ENGINE ->
                stock instanceof LocomotiveDiesel diesel && diesel.isTurnedOn() ? 1 : 0;
            case FRONT_BOGEY_ANGLE ->
                stock instanceof EntityMoveableRollingStock moveable ? yawToPercent(stock.getDefinition().getModel().getFrontYaw(moveable), 90) : 0;
            case REAR_BOGEY_ANGLE ->
                stock instanceof EntityMoveableRollingStock moveable ? yawToPercent(stock.getDefinition().getModel().getRearYaw(moveable), 90) : 0;
            case FRONT_LOCOMOTIVE_ANGLE -> {
                StockModel<?, ?> stockModel = stock.getDefinition().getModel();
                yield stockModel instanceof LocomotiveModel<?, ?> locoModel && stock instanceof EntityMoveableRollingStock moveable
                      ? yawToPercent(locoModel.getFrontLocomotiveYaw(moveable), 90)
                      : 0.5f;
            }
            case REAR_LOCOMOTIVE_ANGLE -> {
                StockModel<?, ?> stockModel = stock.getDefinition().getModel();
                yield stockModel instanceof LocomotiveModel<?, ?> locoModel && stock instanceof EntityMoveableRollingStock moveable
                      ? yawToPercent(locoModel.getRearLocomotiveYaw(moveable), 90)
                      : 0.5f;
            }
            case CYLINDER_DRAIN ->
                stock instanceof LocomotiveSteam steam && steam.cylinderDrainsEnabled() ? 1 : 0;
            case CARGO_FILL ->
                stock instanceof Freight freight ? freight.getPercentCargoFull() / 100f : 0;
            case ENGINE_RPM ->
                stock instanceof LocomotiveDiesel diesel ? diesel.getRelativeRPM() : 0;
            case TENDER_FEED ->
                    stock instanceof LocomotiveSteam steam && steam.isAutoFeedEnabled() ? 1 : 0;
        };
    }

    private float yawToPercent(float yaw, float deltaYaw) {
        yaw = ((yaw % 360) + 360) % 360;
        // Yaw is now between 0 and 360 degrees

        if (yaw > 180) {
            yaw = yaw - 360;
        }
        // Yaw is now between -180 and 180

        return 0.5f + yaw / deltaYaw;
    }

    public void setValue(EntityRollingStock stock, float value) {
        switch (this) {
            case THROTTLE -> {
                if (stock instanceof Locomotive loco) {
                    loco.setThrottle(value);
                }
            }
            case REVERSER -> {
                if (stock instanceof Locomotive loco) {
                    loco.setReverser(value * 2 - 1);
                }
            }
            case TRAIN_BRAKE -> {
                if (stock instanceof Locomotive loco) {
                    loco.setTrainBrake(value);
                }
            }
            case TRAIN_BRAKE_LEVER -> {
                if (stock.getDefinition().isLinearBrakeControl()) {
                    TRAIN_BRAKE.setValue(stock, value);
                } else if (stock instanceof Locomotive loco) {
                    // Logic duplicated in Locomotive#onTick
                    loco.setTrainBrake(Math.clamp(loco.getTrainBrake() + (value - 0.5f) / 80, 0, 1));
                }
            }
            case INDEPENDENT_BRAKE -> {
                if (stock instanceof EntityMoveableRollingStock moveable) {
                    moveable.setIndependentBrake(value);
                }
            }
            case COUPLER_FRONT -> {
                if (stock instanceof EntityCoupleableRollingStock coupleable) {
                    coupleable.setCouplerEngaged(CouplerType.FRONT, value == 0);
                }
            }
            case COUPLER_REAR -> {
                if (stock instanceof EntityCoupleableRollingStock coupleable) {
                    coupleable.setCouplerEngaged(CouplerType.BACK, value == 0);
                }
            }
            case BELL -> {
                if (stock instanceof Locomotive loco) {
                    loco.setBell((int) (value * 10));
                }
            }
            case WHISTLE, HORN -> {
                if (stock instanceof Locomotive loco) {
                    if (value != 0) {
                        loco.setHorn(10000, value);
                    } else {
                        loco.setHorn(10, value);
                    }
                }
            }
            case ENGINE -> {
                if (stock instanceof LocomotiveDiesel diesel) {
                    diesel.setTurnedOn(!diesel.isTurnedOn());
                }
            }
            case CYLINDER_DRAIN -> {
                if (stock instanceof LocomotiveSteam steam) {
                    steam.setCylinderDrains(value > 0.9);
                }
            }
            case TENDER_FEED -> {
                if (stock instanceof LocomotiveSteam steam) {
                    steam.setAutoFeed(value > 0.9);
                }
            }
        }
    }
}