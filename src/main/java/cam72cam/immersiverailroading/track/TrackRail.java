package cam72cam.immersiverailroading.track;

import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.IRBlocks;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.mod.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.stream.Collectors;

public class TrackRail extends TrackBase {

	public TrackRail(BuilderBase builder, BlockPos rel) {
		super(builder, rel, IRBlocks.BLOCK_RAIL);
	}

	@Override
	public TileRailBase placeTrack(boolean actuallyPlace) {
		TileRail tileRail = (TileRail) super.placeTrack(actuallyPlace);

		tileRail.info = builder.info;
		tileRail.setDrops(builder.drops.stream().map((net.minecraft.item.ItemStack stack) -> new ItemStack(stack)).collect(Collectors.toList()));

		return tileRail;
	}
}
