package cam72cam.immersiverailroading.track;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.TrackType;
import cam72cam.immersiverailroading.tile.TileRail;

public class TrackRail extends TrackBase {

	private TrackType type;
	private BlockPos center;
	private float radius;
	private boolean hasModel = true;
	private int slopeHeight = 0;
	private int slopeLength = 0;

	public TrackRail(BuilderBase builder, int rel_x, int rel_y, int rel_z, EnumFacing rel_rotation, TrackType type) {
		super(builder, rel_x, rel_y, rel_z, ImmersiveRailroading.BLOCK_RAIL, rel_rotation);
		this.type = type;
	}
	
	public void setRotationCenter(int rel_cx, int rel_cy, int rel_cz, float d) {
		center = builder.convertRelativeCenterPositions(rel_cx, rel_cy, rel_cz, EnumFacing.NORTH);
		this.radius = d;
	}
	
	public void setHasModel(boolean hasModel) {
		this.hasModel = hasModel;
	}
	
	public void setSlope(int height, int length) {
		slopeHeight = height;
		slopeLength = length;
	}
	
	@Override
	public TileEntity placeTrack() {
		TileRail tileRail = (TileRail) super.placeTrack();
		
		tileRail.setFacing(super.getFacing()); //REMOVEME?
		tileRail.setParent(builder.getPos());
		tileRail.setCenter(center, radius);
		tileRail.setVisible(hasModel); //REMOVEME?
		tileRail.setType(type); //REMOVEME?
		tileRail.setSlope(slopeHeight, slopeLength);
		
		return tileRail;
	}
}
