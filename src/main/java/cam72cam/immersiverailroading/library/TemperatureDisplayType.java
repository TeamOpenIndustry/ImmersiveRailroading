package cam72cam.immersiverailroading.library;

public enum TemperatureDisplayType {
    celcius,
    farenheit,
    kelvin,;

    public float convertFromCelcius(float value) {
        switch (this) {
            default:
            case celcius:
                return value;
            case farenheit:
                return (value * 9f/5f) + 32f;
            case kelvin:
                return value + 270f;
        }
    }

    public String toUnitString() {
        switch (this) {
            default:
            case celcius:
                return "C";
            case farenheit:
                return "F";
            case kelvin:
                return "K";
        }
    }
}
