package cam72cam.immersiverailroading.proxy;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.mod.World;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.text.TextComponentString;

public class IRCommand extends CommandBase {

	@Override
	public String getName() {
		return ImmersiveRailroading.MODID;
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "Usage: " + ImmersiveRailroading.MODID + " (reload|debug)";
	}
	
	@Override
    public int getRequiredPermissionLevel()
    {
        return 2;
    }

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length != 1) {
			throw new CommandException(getUsage(sender));
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
			return;
		}
		
		if (args[0].equals("debug")) {
			List<EntityRollingStock> ents = World.get(sender.getEntityWorld()).getEntities(EntityRollingStock.class);
			ents.sort(Comparator.comparing(a -> a.getUUID().toString()));
			for (EntityRollingStock ent : ents) {
				sender.sendMessage(new TextComponentString(String.format("%s : %s - %s : %s", ent.getUUID(), ent.internal.getEntityId(), ent.getDefinitionID(), ent.getPosition())));
			}
			return;
		}
		throw new CommandException(getUsage(sender));
	}
}