package cam72cam.immersiverailroading.entity.registry;

import java.util.Collection;

import cam72cam.immersiverailroading.entity.EntityRollingStock;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IDefinitionRollingStock {
	public EntityRollingStock spawn(World world, BlockPos pos, EnumFacing facing);
	void render(EntityRollingStock stock, double x, double y, double z, float entityYaw, float partialTicks);
	public void renderItem();
	public Collection<ResourceLocation> getTextures();
	public IBakedModel getInventoryModel();
}
