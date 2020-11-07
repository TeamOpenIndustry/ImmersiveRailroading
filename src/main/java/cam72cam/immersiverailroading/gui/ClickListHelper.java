package cam72cam.immersiverailroading.gui;

import cam72cam.mod.entity.Player;

import java.util.List;

public class ClickListHelper {
    public static <E> E next(List<E> values, E value, Player.Hand hand) {
        return values.get((values.indexOf(value) + values.size() + (hand == Player.Hand.PRIMARY ? 1 : -1)) % values.size());
    }

    public static <E extends Enum<?>> E next(E value, Player.Hand hand) {
        E[] values = (E[]) value.getClass().getEnumConstants();
        return values[(value.ordinal() + values.length + (hand == Player.Hand.PRIMARY ? 1 : -1)) % values.length];
    }
}
