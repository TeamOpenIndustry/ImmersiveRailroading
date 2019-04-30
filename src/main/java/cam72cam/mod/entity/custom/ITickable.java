package cam72cam.mod.entity.custom;

public interface ITickable {
    void onTick();

    static ITickable get(Object o) {
        if (o instanceof ITickable) {
            return (ITickable) o;
        }
        return NOP;
    }
    ITickable NOP = () -> {

    };
}
