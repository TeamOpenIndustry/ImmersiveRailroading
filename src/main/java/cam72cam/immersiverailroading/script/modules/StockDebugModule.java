package cam72cam.immersiverailroading.script.modules;

import cam72cam.immersiverailroading.entity.EntityScriptableRollingStock;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.text.PlayerMessage;
import de.poizzy.luaj.LuaValue;
import de.poizzy.luaj.annotations.LuaFunction;

public class StockDebugModule extends DebugModule {
    private final EntityScriptableRollingStock stock;

    public StockDebugModule(EntityScriptableRollingStock stock) {
        this.stock = stock;
    }

    @LuaFunction
    public LuaValue printToPassengerDialog(LuaValue arg) {
        stock.getPassengers().stream().filter(Entity::isPlayer)
                .map(Entity::asPlayer)
                .forEach(player -> player.sendMessage(PlayerMessage.direct(arg.tojstring())));
        return LuaValue.NIL;
    }
}
