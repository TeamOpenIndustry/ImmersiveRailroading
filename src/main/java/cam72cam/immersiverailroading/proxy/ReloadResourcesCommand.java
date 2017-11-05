package cam72cam.immersiverailroading.proxy;

import java.io.IOException;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

public class ReloadResourcesCommand extends CommandBase {

	@Override
	public String getName() {
		return ImmersiveRailroading.MODID;
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "Usage: " + ImmersiveRailroading.MODID + " reload";
	}
	
	@Override
    public int getRequiredPermissionLevel()
    {
        return 2;
    }

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length != 1 || !args[0].equals("reload")) {
			throw new CommandException(getUsage(sender));
		}
		ImmersiveRailroading.warn("Reloading Immersive Railroading definitions");
		try {
			DefinitionManager.initDefinitions();
		} catch (IOException e) {
			ImmersiveRailroading.catching(e);
			// Might want to stop the server here...
		}
		ImmersiveRailroading.info("Done reloading Immersive Railroading definitions");
	}
}