package cam72cam.immersiverailroading.gui;

import cam72cam.immersiverailroading.entity.CarFreight;
import cam72cam.immersiverailroading.entity.CarTank;
import cam72cam.immersiverailroading.library.GuiTypes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class GuiProxy implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int entityID, int nop1, int nop2) {
    	switch(GuiTypes.values()[ID]) {
		case FREIGHT:
	    	return new FreightContainer(player.inventory, (CarFreight) world.getEntityByID(entityID));
		case TANK:
	    	return new TankContainer(player.inventory, (CarTank) world.getEntityByID(entityID));
		case TENDER:
			return null;
		default:
			return null;
    	}
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int entityID, int nop1, int nop2) {
    	switch(GuiTypes.values()[ID]) {
		case FREIGHT:
	    	return new FreightContainerGui((CarFreight) world.getEntityByID(entityID), new FreightContainer(player.inventory, (CarFreight) world.getEntityByID(entityID)));
		case TANK:
	    	return new TankContainerGui((CarTank) world.getEntityByID(entityID), new TankContainer(player.inventory, (CarTank) world.getEntityByID(entityID)));
		case TENDER:
			return null;
		default:
			return null;
    	}
    }
}