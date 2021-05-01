package cam72cam.immersiverailroading;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.mod.entity.Player;
import cam72cam.mod.text.Command;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.world.World;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class IRCommand extends Command {

	@Override
	public String getPrefix() {
		return ImmersiveRailroading.MODID;
	}

	@Override
	public String getUsage() {
		return "Usage: " + ImmersiveRailroading.MODID + " (reload|debug)";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return PermissionLevel.LEVEL4;
	}

	@Override
	public boolean execute(Consumer<PlayerMessage> sender, Optional<Player> player, String[] args) {
		if (args.length != 1) {
			return false;
		}
		if (args[0].equals("reload")) {
			ImmersiveRailroading.warn("Reloading Immersive Railroading definitions");
			DefinitionManager.initDefinitions();
			ImmersiveRailroading.info("Done reloading Immersive Railroading definitions");
			return true;
		}
		
		if (args[0].equals("debug")) {
			if (player.isPresent()) {
				List<EntityRollingStock> ents = player.get().getWorld().getEntities(EntityRollingStock.class);
				ents.sort(Comparator.comparing(a -> a.getUUID().toString()));
				for (EntityRollingStock ent : ents) {
					sender.accept(PlayerMessage.direct(String.format("%s : %s - %s : %s", ent.getUUID(), ent.getId(), ent.getDefinitionID(), ent.getPosition())));
				}
			} else {
				sender.accept(PlayerMessage.direct("This command is not supported for non-players (yet)"));
			}
			return true;
		}
		return false;
	}
}