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
    CARGO_FILL,
    MAX_CARGO_FILL,
    UNITS_CARGO_FILL,
    ;

    public String getValue(EntityRollingStock stock) {
        Float temp = null;

        switch (this) {
            case SPEED:
                if (stock instanceof EntityMoveableRollingStock) {
                    Speed speed = ((EntityMoveableRollingStock) stock).getCurrentSpeed();
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
                return ConfigGraphics.speedUnit.toUnitString();
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
                        String.format("%.1f", ConfigGraphics.pressureUnit.convertFromPSI(((LocomotiveSteam) stock).getBoilerPressure())) : "";
            case MAX_BOILER_PRESSURE:
                return stock instanceof LocomotiveSteam ?
                        String.format("%.1f", ConfigGraphics.pressureUnit.convertFromPSI((float)((LocomotiveSteam) stock).getDefinition().getMaxPSI(stock.gauge)))
                        : "";
            case UNITS_BOILER_PRESSURE:
                return ConfigGraphics.pressureUnit.toUnitString();

            case TEMPERATURE:
                if (stock instanceof LocomotiveSteam) {
                    temp = ((LocomotiveSteam) stock).getBoilerTemperature();
                }
                if (stock instanceof LocomotiveDiesel) {
                    temp = ((LocomotiveDiesel) stock).getEngineTemperature();
                }
                return temp != null ? String.format("%.1f", ConfigGraphics.temperatureUnit.convertFromCelcius(temp)) : "";
            case MAX_TEMPERATURE:
                if (stock instanceof LocomotiveSteam) {
                    temp = 100f;
                }
                if (stock instanceof LocomotiveDiesel) {
                    temp = 150f;
                }
                return temp != null ? String.format("%.1f", ConfigGraphics.temperatureUnit.convertFromCelcius(temp)) : "";
            case UNITS_TEMPERATURE:
                return ConfigGraphics.temperatureUnit.toUnitString();
            case BRAKE_PRESSURE:
                if (stock instanceof EntityMoveableRollingStock) {
                    return String.format("%s", (int)(((EntityMoveableRollingStock) stock).getBrakePressure() * 100));
                }
                return "";
            case MAX_BRAKE_PRESSURE:
                return "100";
            case UNITS_BRAKE_PRESSURE:
                return "%";
            case CARGO_FILL:
                if (stock instanceof Freight) {
                    return String.format("%s", ((Freight) stock).getPercentCargoFull());
                }
                return "";
            case MAX_CARGO_FILL:
                return "100";
            case UNITS_CARGO_FILL:
                return "%";
        }
        return "";
    }
    @Override
    public String toString() {
        return "stat." + this.name().toLowerCase(Locale.ROOT);
    }
}
