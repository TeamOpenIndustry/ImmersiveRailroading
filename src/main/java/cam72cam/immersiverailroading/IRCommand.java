package cam72cam.immersiverailroading;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.mod.text.Command;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.world.World;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
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
    public boolean opRequired()
    {
        return true;
    }

	@Override
	public boolean execute(World world, Consumer<PlayerMessage> sender, String[] args) {
		if (args.length != 1) {
			return false;
		}
		if (args[0].equals("reload")) {
			ImmersiveRailroading.warn("Reloading Immersive Railroading definitions");
			try {
				DefinitionManager.initDefinitions();
			} catch (IOException e) {
				ImmersiveRailroading.catching(e);
				// Might want to stop the server here...
			}
			ImmersiveRailroading.info("Done reloading Immersive Railroading definitions");
			return true;
		}
		
		if (args[0].equals("debug")) {
			List<EntityRollingStock> ents = world.getEntities(EntityRollingStock.class);
			ents.sort(Comparator.comparing(a -> a.getUUID().toString()));
			for (EntityRollingStock ent : ents) {
				sender.accept(PlayerMessage.direct(String.format("%s : %s - %s : %s", ent.getUUID(), ent.getId(), ent.getDefinitionID(), ent.getPosition())));
			}
			return true;
		}
		return false;
	}
}