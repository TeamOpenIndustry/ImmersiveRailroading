package cam72cam.immersiverailroading.track;

import java.util.ArrayList;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

//TODO @cam72cam use BlockPos and Vec3i

@SuppressWarnings("incomplete-switch")
public abstract class BuilderBase {
	protected ArrayList<TrackBase> tracks = new ArrayList<TrackBase>();
	
	public World world;
	int x;
	int y;
	int z;
	EnumFacing rotation;
	
	private int[] translation;

	public BuilderBase(World world, int x, int y, int z, EnumFacing rotation) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.rotation = rotation;
		this.world = world;
	}
	
	public void setRelativeTranslate(int x, int y, int z) {
		translation = new int[]{x, y, z};
	}	
	
	public class PosRot extends BlockPos{
		private EnumFacing rot;
		
		public EnumFacing getRotation() {
			return rot;
		}

		public PosRot(BlockPos pos, EnumFacing rot) {
			super(pos);
			this.rot = rot;
		}
	}
	
	public PosRot convertRelativeCenterPositions(int rel_x, int rel_y, int rel_z, EnumFacing rel_rotation) {
		if (rel_x >= 1) {
			switch(rotation) {
			case SOUTH:
				rel_x += 0;
				rel_z += 0;
				break;
			case WEST:
				rel_x += 0;
				rel_z -= 1;
				break;
			case NORTH:
				rel_x -= 1;
				rel_z -= 1;
				break;
			case EAST:
				rel_x -= 1;
				rel_z -= 0;
				break;
			}
		} else {
			switch(rotation) {
			case EAST:
				rel_x += 0;
				rel_z += 0;
				break;
			case NORTH:
				rel_x += 0;
				rel_z -= 1;
				break;
			case WEST:
				rel_x += 1;
				rel_z -= 1;
				break;
			case SOUTH:
				rel_x += 1;
				rel_z += 0;
				break;
			}
		}
		return convertRelativePositions(rel_x, rel_y, rel_z, rel_rotation);
	}

	public PosRot convertRelativePositions(int rel_x, int rel_y, int rel_z, EnumFacing rel_rotation) {
		if (translation != null) {
			rel_x += translation[0];
			rel_y += translation[1];
			rel_z += translation[2];
		}
		
		EnumFacing newrot = EnumFacing.fromAngle(rel_rotation.getHorizontalAngle() + rotation.getHorizontalAngle());
		
		switch (rotation) {
		case SOUTH:
			// 270*
			return new PosRot(new BlockPos(x + rel_x, y + rel_y, z + rel_z), newrot);
		case WEST:
			// 180*
			return new PosRot(new BlockPos(x - rel_z, y + rel_y, z + rel_x), newrot);
		case NORTH:
			//  90*
			return new PosRot(new BlockPos(x - rel_x, y + rel_y, z - rel_z), newrot);
		case EAST:
			//   0*
			return new PosRot(new BlockPos(x + rel_z, y + rel_y, z - rel_x), newrot);
		}
		return null;
	}
	
	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}
	public int getZ() {
		return z;
	}

	public boolean canBuild() {
		for(TrackBase track : tracks) {
			if (!track.canPlaceTrack()) {
				return false;
			}
		}
		return true;
	}
	
	public void build() {
		if (!canBuild()) {
			return ;
		}
		for(TrackBase track : tracks) {
			track.placeTrack();
		}
	}
	
	public ArrayList<TrackBase> getTracks() {
		return this.tracks;
	}

	public BlockPos getPos() {
		return new BlockPos(x, y, z);
	}
}
