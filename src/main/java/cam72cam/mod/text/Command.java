package cam72cam.mod.text;

import cam72cam.mod.ModCore;
import cam72cam.mod.world.World;
import net.minecraft.command.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.server.FMLServerHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class Command {
    private static final List<Command> commands = new ArrayList<>();

    static {
        ModCore.onServerStarting(() -> {
            CommandHandler ch = (CommandHandler) FMLCommonHandler.instance().getMinecraftServerInstance().getCommandManager();
            for (Command command : commands) {
                ch.registerCommand(command.internal);
            }
        });
    }

    public static void register(Command cmd) {
        commands.add(cmd);
    }


    private final ICommand internal;
    protected Command() {
        this.internal = new CommandBase() {
            @Override
            public String getName() {
                return Command.this.getPrefix();
            }

            @Override
            public String getUsage(ICommandSender sender) {
                return Command.this.getUsage();
            }

            @Override
            public int getRequiredPermissionLevel() {
                return opRequired() ? 2 : 4;
            }

            @Override
            public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
                if (!Command.this.execute(World.get(sender.getEntityWorld()), m -> sender.sendMessage(m.internal), args)) {
                    throw new CommandException(getUsage(sender));
                }
            }
        };
    }

    public abstract String getPrefix();

    public abstract String getUsage();

    public abstract boolean opRequired();

    public abstract boolean execute(World world, Consumer<PlayerMessage> sender, String[] args);
}
