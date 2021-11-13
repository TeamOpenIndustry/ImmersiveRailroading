package cam72cam.immersiverailroading.gui.overlay;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.entity.*;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.mod.fluid.Fluid;

import java.util.Locale;

public enum Stat {
    SPEED,
    LIQUID,
    MAX_LIQUID,
    BOILER_PSI,
    MAX_BOILER_PSI,
    TEMPERATURE,
    MAX_TEMPERATURE,
    ;
    public String getValue(EntityRollingStock stock) {
        switch (this) {
            case SPEED:
                if (stock instanceof EntityMoveableRollingStock) {
                    double current = Math.abs(((EntityMoveableRollingStock) stock).getCurrentSpeed().metric());
                    switch (ConfigGraphics.speedUnit) {
                        case mph:
                            return String.format("%.2f mph", current * 0.621371);
                        case ms:
                            return String.format("%.2f m/s", current / 3.6);
                        case kmh:
                        default:
                            return String.format("%.2f km/h", current);
                    }
                }
                return "";
            case LIQUID:
                return stock instanceof FreightTank ?
                        String.format("%.1fB",
                                ((FreightTank) stock).getLiquidAmount() / (float)Fluid.BUCKET_VOLUME)
                        : "";
            case MAX_LIQUID:
                return stock instanceof FreightTank ?
                        String.format("%.1fB",
                                ((FreightTank)stock).getTankCapacity().MilliBuckets() / (float)Fluid.BUCKET_VOLUME)
                        : "";
            case BOILER_PSI:
                return stock instanceof LocomotiveSteam ?
                        String.format("%.1fPSI", ((LocomotiveSteam) stock).getBoilerPressure()) : "";
            case MAX_BOILER_PSI:
                return stock instanceof LocomotiveSteam ?
                        String.format("%.1fPSI", (float)((LocomotiveSteam) stock).getDefinition().getMaxPSI(stock.gauge))
                        : "";
            case TEMPERATURE:
                if (stock instanceof LocomotiveSteam) {
                    return String.format("%.1fC", ((LocomotiveSteam) stock).getBoilerTemperature());
                }
                if (stock instanceof LocomotiveDiesel) {
                    return String.format("%.1fC", ((LocomotiveDiesel) stock).getEngineTemperature());
                }
                return "";
            case MAX_TEMPERATURE:
                if (stock instanceof LocomotiveSteam) {
                    return String.format("%.1fC", 100f);
                }
                if (stock instanceof LocomotiveDiesel) {
                    return String.format("%.1fC", 150f);
                }
                return "";
            default:
                return "";
        }
    }
    @Override
    public String toString() {
        return "stat." + this.name().toLowerCase(Locale.ROOT);
    }
}
