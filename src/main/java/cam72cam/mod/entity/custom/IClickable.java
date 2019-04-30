package cam72cam.mod.entity.custom;

import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.util.Hand;

public interface IClickable {
    ClickResult onClick(Player player, Hand hand);

    static IClickable get(Object o) {
        if (o instanceof IClickable) {
            return (IClickable)o;
        }
        return NOP;
    }
    IClickable NOP = (player, hand) -> ClickResult.PASS;
}
