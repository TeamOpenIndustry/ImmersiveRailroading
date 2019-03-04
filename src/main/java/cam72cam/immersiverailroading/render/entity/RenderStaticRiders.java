package cam72cam.immersiverailroading.render.entity;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.entity.EntityRidableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRidableRollingStock.StaticPassenger;
import cam72cam.immersiverailroading.util.VecUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.Vec3d;

public class RenderStaticRiders {
	public static void render(EntityRidableRollingStock stock, float partialTicks) {
		for (StaticPassenger pass : stock.staticPassengers) {
			if (pass.cache == null) {
				EntityLiving cached = pass.respawn(stock.world, stock.getPositionVector());
				pass.cache = cached;
			}
			Vec3d pos = stock.passengerPositions.get(pass.uuid);
			if (pos == null) {
				continue;
			}
			pos = pos.add(stock.getDefinition().getPassengerCenter(stock.gauge));
			pos = VecUtil.rotateWrongYaw(pos, stock.rotationYaw);
			//TileEntityMobSpawnerRenderer
			Entity ent = (Entity) pass.cache;
			GL11.glPushMatrix();
			{
				GL11.glTranslated(pos.x, pos.y - 0.5 + 0.35, pos.z);
				GL11.glRotated(pass.rotation, 0, 1, 0);
	            Minecraft.getMinecraft().getRenderManager().renderEntity(ent, 0, 0, 0, 0, 0, false);
			}
			GL11.glPopMatrix();
		}
	}
}
