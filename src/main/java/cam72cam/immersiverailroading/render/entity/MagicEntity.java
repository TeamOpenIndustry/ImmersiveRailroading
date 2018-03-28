package cam72cam.immersiverailroading.render.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MagicEntity extends Entity {
	/*
	 * Minecraft does NOT support rendering entities which overlap with the field of view but don't exist in it
	 * 
	 * For large entities this breaks in awesome ways, like walking past the center of a rail car
	 * 
	 * To fix this we create an entity which is always rendered and render all of our stuff inside of that 
	 * 
	 */

	public MagicEntity(World worldIn) {
		super(worldIn);
		this.forceSpawn = true;
	}
	
	@Override
	public void onUpdate() {
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		if (player == null) {
			return;
		}
		Vec3d pos = player.getPositionVector();
		pos = pos.add(player.getLookVec());
		this.setPosition(pos.x, pos.y, pos.z);
	}

	@Override
	public AxisAlignedBB getEntityBoundingBox() {
		return new AxisAlignedBB(0, 0, 0, 0, 0, 0);
	}
	
	@Override
	public AxisAlignedBB getCollisionBoundingBox() {
		return null;
	}
	
	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return new AxisAlignedBB(0, 0, 0, 1, 1, 1);
	}
	
	public boolean shouldRenderInPass(int pass)
    {
		return true;
    }
	
	@Override
	public boolean isInRangeToRender3d(double x, double y, double z) {
		return true;
	}

	@Override
	protected void entityInit() {
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound compound) {
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound compound) {
	}
}
