package cam72cam.immersiverailroading.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.server.FMLServerHandler;

@EventBusSubscriber(Side.SERVER)
public class ServerProxy extends CommonProxy {

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int entityID, int nop1, int nop2) {
    	return null;
    }

    public World getWorld(int dimension)  {
		return FMLServerHandler.instance().getServer().getWorld(dimension);
	}

	@Override
	public InputStream getResourceStream(ResourceLocation location) throws IOException {
		// TODO support server side config
		String s = "/assets/" + location.getResourceDomain() + "/" + location.getResourcePath();

        URL url = ImmersiveRailroading.class.getResource(s);
		return url != null ? ImmersiveRailroading.class.getResourceAsStream(s) : null;
	}
}
