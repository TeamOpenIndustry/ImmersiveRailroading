package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockUtil {
	private static List<Fuzzy> whitelist;

	public static boolean canBeReplaced(World world, Vec3i pos, boolean allowFlex) {
		if (world.isReplaceable(pos)) {
			return true;
		}
		
		if (world.isBlock(pos, IRBlocks.BLOCK_RAIL_PREVIEW)) {
			return true;
		}
		if (allowFlex && isIRRail(world, pos)) {
			TileRailBase te = world.getBlockEntity(pos, TileRailBase.class);
			return te != null && te.isFlexible();
		}
		return false;
	}

	public static boolean isIRRail(World world, Vec3i pos) {
		return world.isBlock(pos, IRBlocks.BLOCK_RAIL_GAG) || world.isBlock(pos, IRBlocks.BLOCK_RAIL);
	}

	public static boolean isWhitelisted(World world, Vec3i pos) {
		if (whitelist == null) {
			whitelist = new ArrayList<>();
			Arrays.stream(Config.ConfigDamage.TrainsIgnoreBlocks).forEach(s -> whitelist.add(Fuzzy.get(s)));
		}

		ItemStack stack = world.getItemStack(pos);

		return whitelist.stream().anyMatch(fuzzy -> fuzzy.matches(stack));
	}
}
