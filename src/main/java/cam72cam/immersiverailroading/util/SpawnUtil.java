package cam72cam.immersiverailroading.util;

import java.util.List;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.MovementSimulator;
import cam72cam.immersiverailroading.entity.TickPos;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.entity.EntityBuildableRollingStock;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.tile.TileRailBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SpawnUtil {
	public static EnumActionResult placeStock(EntityPlayer player, EnumHand hand, World worldIn, BlockPos pos, EntityRollingStockDefinition def, List<ItemComponentType> list) {
		double offset = def.getCouplerPosition(CouplerType.BACK) - Config.couplerRange;
		float yaw = EnumFacing.fromAngle(player.rotationYawHead).getHorizontalAngle();
		TickPos tp = new MovementSimulator(worldIn, new TickPos(0, Speed.fromMinecraft(0), new Vec3d(pos.add(0, 0.7, 0)), yaw, yaw, yaw, 0, false, false), def.getBogeyFront(), def.getBogeyRear()).nextPosition(offset);
		
		TileEntity te = worldIn.getTileEntity(new BlockPos(tp.position));
		if (te instanceof TileRailBase && !((TileRailBase)te).getParentTile().getType().isTurn()) {
			if (!worldIn.isRemote) {
				EntityRollingStock stock = def.spawn(worldIn, tp.position, EnumFacing.fromAngle(player.rotationYawHead));
				
				if (stock instanceof EntityBuildableRollingStock) {
					((EntityBuildableRollingStock)stock).setComponents(list);
				}
				
				if (stock instanceof EntityMoveableRollingStock) {
					// snap to track
					EntityMoveableRollingStock mrs = (EntityMoveableRollingStock)stock;
					mrs.initPositions();
				}
			}
			if (!player.isCreative()) {
				ItemStack stack = player.getHeldItem(hand);
				stack.setCount(stack.getCount()-1);
				player.setHeldItem(hand, stack);
			}
			return EnumActionResult.PASS;
		}
		if (worldIn.isRemote) {
			player.sendMessage(ChatText.STOCK_PLACEMENT.getMessage());
		}
		return EnumActionResult.FAIL;
	}
}
