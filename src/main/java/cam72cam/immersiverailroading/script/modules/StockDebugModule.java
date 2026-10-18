package cam72cam.immersiverailroading.script.modules;

import cam72cam.immersiverailroading.entity.EntityScriptableRollingStock;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.text.PlayerMessage;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.LuaFunction;

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
