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
    INDEPENDENT_BRAKE,
    BRAKE_PRESSURE,
    COUPLER_FRONT,
    COUPLER_REAR,
    COUPLED_FRONT,
    COUPLED_REAR,
    BELL,
    WHISTLE,
    HORN,
    ENGINE,
    FRONT_BOGEY_ANGLE,
    REAR_BOGEY_ANGLE,
    FRONT_LOCOMOTIVE_ANGLE,
    REAR_LOCOMOTIVE_ANGLE,
    ;

    public float getValue(EntityRollingStock stock) {
        switch (this) {
            case LIQUID:
                return stock instanceof FreightTank ? ((FreightTank) stock).getPercentLiquidFull() / 100f : 0;
            case SPEED:
                return stock instanceof Locomotive ? (float) (
                        Math.abs(((Locomotive) stock).getCurrentSpeed().metric()) /
                                ((Locomotive) stock).getDefinition().getMaxSpeed(stock.gauge).metric()) : 0;
            case TEMPERATURE:
                if (stock instanceof LocomotiveSteam) {
                    return ((LocomotiveSteam) stock).getBoilerTemperature() / 100f;
                }
                if (stock instanceof LocomotiveDiesel) {
                    return ((LocomotiveDiesel) stock).getEngineTemperature() / 150f;
                }
                return 0;
            case BOILER_PRESSURE:
                return stock instanceof LocomotiveSteam ?
                        ((LocomotiveSteam) stock).getBoilerPressure() / ((LocomotiveSteam) stock).getDefinition().getMaxPSI(stock.gauge) : 0;
            case THROTTLE:
                return stock instanceof Locomotive ? ((Locomotive) stock).getThrottle() : 0;
            case REVERSER:
                return stock instanceof Locomotive ? (((Locomotive) stock).getReverser() + 1) / 2 : 0;
            case TRAIN_BRAKE:
                return stock instanceof Locomotive ? ((Locomotive) stock).getTrainBrake() : 0;
            case INDEPENDENT_BRAKE:
                return stock instanceof EntityMoveableRollingStock ? ((EntityMoveableRollingStock) stock).getIndependentBrake() : 0;
            case BRAKE_PRESSURE:
                return stock instanceof EntityMoveableRollingStock ? ((EntityMoveableRollingStock) stock).getBrakePressure() : 0;
            case COUPLER_FRONT:
                return stock instanceof EntityCoupleableRollingStock ? ((EntityCoupleableRollingStock) stock).isCouplerEngaged(CouplerType.FRONT) ? 1 : 0 : 0;
            case COUPLER_REAR:
                return stock instanceof EntityCoupleableRollingStock ? ((EntityCoupleableRollingStock) stock).isCouplerEngaged(CouplerType.BACK) ? 1 : 0 : 0;
            case COUPLED_FRONT:
                return stock instanceof EntityCoupleableRollingStock ? ((EntityCoupleableRollingStock) stock).isCoupled(CouplerType.FRONT) && ((EntityCoupleableRollingStock) stock).isCouplerEngaged(CouplerType.FRONT) ? 1 : 0 : 0;
            case COUPLED_REAR:
                return stock instanceof EntityCoupleableRollingStock ? ((EntityCoupleableRollingStock) stock).isCoupled(CouplerType.BACK) && ((EntityCoupleableRollingStock) stock).isCouplerEngaged(CouplerType.BACK) ? 1 : 0 : 0;
            case BELL:
                return stock instanceof Locomotive ? ((Locomotive) stock).getBell() > 0 ? 1 : 0 : 0;
            case WHISTLE:
            case HORN:
                return stock instanceof Locomotive ? ((Locomotive) stock).getHornTime() > 0 ? 1 : 0 : 0;
            case ENGINE:
                return stock instanceof LocomotiveDiesel ? ((LocomotiveDiesel) stock).isTurnedOn() ? 1 : 0 : 0;
            case FRONT_BOGEY_ANGLE:
                return yawToPercent(stock.getDefinition().getModel().getFrontYaw((EntityMoveableRollingStock) stock), 90);
            case REAR_BOGEY_ANGLE:
                return yawToPercent(stock.getDefinition().getModel().getRearYaw((EntityMoveableRollingStock) stock), 90);
            case FRONT_LOCOMOTIVE_ANGLE:
                StockModel<?, ?> front = stock.getDefinition().getModel();
                return front instanceof LocomotiveModel ? yawToPercent(((LocomotiveModel<?, ?>)front).getFrontLocomotiveYaw((EntityMoveableRollingStock) stock), 90) : 0.5f;
            case REAR_LOCOMOTIVE_ANGLE:
                StockModel<?, ?> rear = stock.getDefinition().getModel();
                return rear instanceof LocomotiveModel ? yawToPercent(((LocomotiveModel<?, ?>)rear).getRearLocomotiveYaw((EntityMoveableRollingStock) stock), 90) : 0.5f;
        }
        return 0;
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
            case THROTTLE:
                if (stock instanceof Locomotive) {
                    ((Locomotive) stock).setThrottle(value);
                }
                break;
            case REVERSER:
                if (stock instanceof Locomotive) {
                    ((Locomotive) stock).setReverser(value * 2 - 1);
                }
                break;
            case TRAIN_BRAKE:
                if (stock instanceof Locomotive) {
                    ((Locomotive) stock).setTrainBrake(value);
                }
                break;
            case INDEPENDENT_BRAKE:
                if (stock instanceof EntityMoveableRollingStock) {
                    ((Locomotive) stock).setIndependentBrake(value);
                }
                break;
            case COUPLER_FRONT:
                if (stock instanceof EntityCoupleableRollingStock) {
                    ((EntityCoupleableRollingStock) stock).setCouplerEngaged(EntityCoupleableRollingStock.CouplerType.FRONT, value == 0);
                }
                break;
            case COUPLER_REAR:
                if (stock instanceof EntityCoupleableRollingStock) {
                    ((EntityCoupleableRollingStock) stock).setCouplerEngaged(EntityCoupleableRollingStock.CouplerType.BACK, value == 0);
                }
                break;
            case BELL:
                if (stock instanceof Locomotive) {
                    ((Locomotive) stock).setBell(10);
                }
                break;
            case WHISTLE:
            case HORN:
                if (stock instanceof Locomotive) {
                    ((Locomotive) stock).setHorn(40, Locomotive.AUTOMATED_PLAYER);
                }
                break;
            case ENGINE:
                if (stock instanceof LocomotiveDiesel) {
                    ((LocomotiveDiesel) stock).setTurnedOn(!((LocomotiveDiesel) stock).isTurnedOn());
                }
                break;
        }
    }
}
