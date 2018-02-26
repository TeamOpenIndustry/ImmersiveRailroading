package cam72cam.immersiverailroading.util;

import java.util.List;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.ItemComponentType;
import cam72cam.immersiverailroading.physics.MovementSimulator;
import cam72cam.immersiverailroading.physics.TickPos;
import cam72cam.immersiverailroading.entity.EntityBuildableRollingStock;
import cam72cam.immersiverailroading.entity.EntityCoupleableRollingStock.CouplerType;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import trackapi.lib.ITrack;
import trackapi.lib.Util;

public class SpawnUtil {
	public static EnumActionResult placeStock(EntityPlayer player, EnumHand hand, World worldIn, BlockPos pos, EntityRollingStockDefinition def, List<ItemComponentType> list) {
		ITrack initte = Util.getTileEntity(worldIn, new Vec3d(pos.add(0, 0.7, 0)), true);
		if (initte == null) {
			return EnumActionResult.FAIL;
		}
		double trackGauge = initte.getTrackGauge();
		Gauge gauge = Gauge.from(trackGauge);
		
		
		if (!player.isCreative() && gauge != ItemGauge.get(player.getHeldItem(hand))) {
			player.sendMessage(ChatText.STOCK_WRONG_GAUGE.getMessage());
			return EnumActionResult.FAIL;
		}
		
		double offset = def.getCouplerPosition(CouplerType.BACK, gauge) - Config.couplerRange;
		float yaw = player.rotationYawHead;
		TickPos tp = new MovementSimulator(worldIn, new TickPos(0, Speed.ZERO, new Vec3d(pos.add(0, 0.7, 0)).addVector(0.5, 0, 0.5), yaw, yaw, yaw, 0, false), def.getBogeyFront(gauge), def.getBogeyRear(gauge), gauge.value()).nextPosition(offset);
		
		if (!tp.isOffTrack) {
			if (!worldIn.isRemote) {
				EntityRollingStock stock = def.spawn(worldIn, tp.position, EnumFacing.fromAngle(player.rotationYawHead), gauge);
				
				if (stock instanceof EntityBuildableRollingStock) {
					((EntityBuildableRollingStock)stock).setComponents(list);
				}
				
				if (stock instanceof EntityMoveableRollingStock) {
					// snap to track
					EntityMoveableRollingStock mrs = (EntityMoveableRollingStock)stock;
					tp.speed = Speed.ZERO;
					mrs.initPositions(tp);
				}
			}
			if (!player.isCreative()) {
				ItemStack stack = player.getHeldItem(hand);
				stack.setCount(stack.getCount()-1);
				player.setHeldItem(hand, stack);
			}
			return EnumActionResult.PASS;
		}
		return EnumActionResult.FAIL;
	}
}
