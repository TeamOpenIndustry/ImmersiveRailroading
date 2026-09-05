package cam72cam.immersiverailroading.script.modules;

import cam72cam.immersiverailroading.entity.EntityScriptableRollingStock;
import cam72cam.immersiverailroading.script.LuaFunction;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.text.PlayerMessage;
import org.luaj.vm2.LuaValue;

public class StockDebugModule extends DebugModule {
    private final EntityScriptableRollingStock stock;

    public StockDebugModule(EntityScriptableRollingStock stock) {
        this.stock = stock;
    }

    @LuaFunction(module = "Debug")
    public void printToPassengerDialog(LuaValue arg) {
        stock.getPassengers().stream().filter(Entity::isPlayer)
                .map(Entity::asPlayer)
                .forEach(player -> player.sendMessage(PlayerMessage.direct(arg.tojstring())));
    }
}
