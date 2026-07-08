package cam72cam.immersiverailroading.library.unit;

public enum TemperatureDisplayType {
    celcius,
    kelvin,
    farenheit;

    public float convertFromCelcius(float value) {
        switch (this) {
            default:
            case celcius:
                return value;
            case kelvin:
                return value + 273.15f;
            case farenheit:
                return (value * 9f/5f) + 32f;
        }
    }

    public String toUnitString() {
        switch (this) {
            default:
            case celcius:
                return "°C";
            case kelvin:
                return "K";
            case farenheit:
                return "°F";
        }
    }
}
