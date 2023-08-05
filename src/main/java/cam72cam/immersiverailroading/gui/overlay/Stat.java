package cam72cam.immersiverailroading.gui.overlay;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.mod.fluid.Fluid;

import java.util.Locale;

public enum Stat {
    SPEED,
    MAX_SPEED,
    UNITS_SPEED,
    LIQUID,
    MAX_LIQUID,
    UNITS_LIQUID,
    BOILER_PRESSURE,
    MAX_BOILER_PRESSURE,
    UNITS_BOILER_PRESSURE,
    TEMPERATURE,
    MAX_TEMPERATURE,
    UNITS_TEMPERATURE,
    BRAKE_PRESSURE,
    MAX_BRAKE_PRESSURE,
    UNITS_BRAKE_PRESSURE,
    ;

    public String getValue(EntityRollingStock stock) {
        switch (this) {
            case SPEED:
                if (stock instanceof EntityMoveableRollingStock) {
                    Speed speed = ((EntityMoveableRollingStock) stock).getCurrentSpeed();
                    switch (ConfigGraphics.speedUnit) {
                        case mph:
                            return String.format("%.2f", Math.abs(speed.imperial()));
                        case ms:
                            return String.format("%.2f", Math.abs(speed.metersPerSecond()));
                        case kmh:
                            return String.format("%.2f", Math.abs(speed.metric()));
                    }
                }
                return "";
            case MAX_SPEED:
                if (stock instanceof Locomotive) {
                    Speed speed = ((Locomotive)stock).getDefinition().getMaxSpeed(stock.gauge);
                    switch (ConfigGraphics.speedUnit) {
                        case mph:
                            return String.format("%.0f", Math.abs(speed.imperial()));
                        case ms:
                            return String.format("%.0f", Math.abs(speed.metersPerSecond()));
                        case kmh:
                            return String.format("%.0f", Math.abs(speed.metric()));
                    }
                }
                return "";
            case UNITS_SPEED:
                switch (ConfigGraphics.speedUnit) {
                    case kmh:
                        return "km/h";
                    case mph:
                        return "mph";
                    case ms:
                        return "m/s";
                }
                return "";

            case LIQUID:
                return stock instanceof FreightTank ?
                        String.format("%.1f",
                                ((FreightTank) stock).getLiquidAmount() / (float)Fluid.BUCKET_VOLUME)
                        : "";
            case MAX_LIQUID:
                return stock instanceof FreightTank ?
                        String.format("%.1f",
                                ((FreightTank)stock).getTankCapacity().MilliBuckets() / (float)Fluid.BUCKET_VOLUME)
                        : "";
            case UNITS_LIQUID:
                return "B";

            case BOILER_PRESSURE:
                return stock instanceof LocomotiveSteam ?
                        String.format("%.1f", ((LocomotiveSteam) stock).getBoilerPressure()) : "";
            case MAX_BOILER_PRESSURE:
                return stock instanceof LocomotiveSteam ?
                        String.format("%.1f", (float)((LocomotiveSteam) stock).getDefinition().getMaxPSI(stock.gauge))
                        : "";
            case UNITS_BOILER_PRESSURE:
                return "PSI";

            case TEMPERATURE:
                if (stock instanceof LocomotiveSteam) {
                    return String.format("%.1f", ((LocomotiveSteam) stock).getBoilerTemperature());
                }
                if (stock instanceof LocomotiveDiesel) {
                    return String.format("%.1f", ((LocomotiveDiesel) stock).getEngineTemperature());
                }
                return "";
            case MAX_TEMPERATURE:
                if (stock instanceof LocomotiveSteam) {
                    return String.format("%.1f", 100f);
                }
                if (stock instanceof LocomotiveDiesel) {
                    return String.format("%.1f", 150f);
                }
                return "";
            case UNITS_TEMPERATURE:
                return "C";
            case BRAKE_PRESSURE:
                if (stock instanceof EntityMoveableRollingStock) {
                    return String.format("%s", (int)(((EntityMoveableRollingStock) stock).getBrakePressure() * 100));
                }
                return "";
            case MAX_BRAKE_PRESSURE:
                return "100";
            case UNITS_BRAKE_PRESSURE:
                return "%";
        }
        return "";
    }
    @Override
    public String toString() {
        return "stat." + this.name().toLowerCase(Locale.ROOT);
    }
}
