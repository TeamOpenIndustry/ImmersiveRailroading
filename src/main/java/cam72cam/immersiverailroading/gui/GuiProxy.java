package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.Freight;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class GuiProxy implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int entityID, int nop1, int nop2) {
    	return new FreightContainer(player.inventory, (Freight) world.getEntityByID(entityID));
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int entityID, int nop1, int nop2) {
    	return new FreightContainerGui((Freight) world.getEntityByID(entityID), new FreightContainer(player.inventory, (Freight) world.getEntityByID(entityID)));
    }
}