package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.tile.TileRail;
import cam72cam.immersiverailroading.tile.TileRailBase;
import cam72cam.immersiverailroading.track.IIterableTrack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;

public class SwitchUtil {
	public static SwitchState getSwitchState(TileRail rail) {
		return getSwitchState(rail, null);
	}

	public static SwitchState getSwitchState(TileRail rail, Vec3d position) {
		if (rail == null) {
			return SwitchState.NONE;
		}

		if (rail.info.settings.type != TrackItems.TURN && rail.info.settings.type != TrackItems.CUSTOM && rail.info.settings.type != TrackItems.CUBICPARABOLA && rail.info.settings.type != TrackItems.MULTISWITCH) {
			return SwitchState.NONE;
		}

		TileRail parent = rail.getParentTile();
		if (parent == null) {
			return SwitchState.NONE;
		}
		if (parent.info.settings.type != TrackItems.SWITCH && parent.info.settings.type != TrackItems.MULTISWITCH) {
			return SwitchState.NONE;
		}

		if (position != null && parent.info.settings.type != TrackItems.MULTISWITCH) {
			IIterableTrack switchBuilder = (IIterableTrack) parent.info.getBuilder(rail.getWorld());
			IIterableTrack turnBuilder = (IIterableTrack) rail.info.getBuilder(rail.getWorld());
			double isOnStraight = switchBuilder.offsetFromTrack(parent.info, parent.getPos(), position);
			double isOnTurn = turnBuilder.offsetFromTrack(rail.info, rail.getPos(), position);

			if (Math.abs(isOnStraight - isOnTurn) > rail.info.settings.gauge.scale() / 16) {
				if (isOnStraight > isOnTurn) {
					return SwitchState.NONE;
				} else {
					return SwitchState.STRAIGHT;
				}
			}
		}

		if(position != null && parent.info.settings.type == TrackItems.MULTISWITCH){
			//find target
			SwitchState targetState;
			if (parent.isSwitchForced()) {
				targetState = parent.info.switchForced;
			}else{
				targetState =  fromRedStoneLevel(RailPoweredLevel(rail));
			}

			TileRail targetRail;
			int targetMidState = -1;
			switch (targetState){
				case NONE:
				case STRAIGHT:
					break;
				case MID1:
					targetMidState = 0;
					break;
				case MID2:
					targetMidState = 1;
					break;
				case MID3:
					targetMidState = 2;
					break;
				case MID4:
					targetMidState = 3;
					break;
				case TURN:
					targetMidState = 4;
					break;
			}
			if(targetMidState != -1 && targetMidState < parent.info.multiSwitchInfo.wayList.size()){
				targetRail = parent.getChildWayTile(targetMidState);
			}else if(targetMidState == -1 ){
				targetRail = parent;//NONE and STRAIGHT
			}else{
				targetRail = parent.getChildWayTile(parent.info.multiSwitchInfo.wayList.size()-1);//convert to the biggest one
			}
			IIterableTrack targetBuilder = (IIterableTrack) targetRail.info.getBuilder(rail.getWorld());

			//offset
			double targetOffset = targetBuilder.offsetFromTrack(targetRail.info, targetRail.getPos(), position);
			double currentOffset = 0x3f3f3f;

			//find current
			SwitchState currentState = SwitchState.NONE;
			int currentStateInt = 0;//0,1,2,3,4,5,6=NONE,STRAIGHT,MID1,MID2,MID3,MID4,TURN
			for(int i = 0; i<parent.info.multiSwitchInfo.wayList.size(); i++){
				TileRail currentRail = parent.getChildWayTile(i);
				IIterableTrack currentBuilder = (IIterableTrack) currentRail.info.getBuilder(rail.getWorld());
				double delta = currentBuilder.offsetFromTrack(currentRail.info, currentRail.getPos(), position);
				if(delta<currentOffset){
					currentStateInt = i+2;//STRAIGHT=1,MID1=2(i=0),MID2=3(i=1),...
					currentOffset = delta;
				}
			}
			IIterableTrack straightBuilder = (IIterableTrack) parent.info.getBuilder(rail.getWorld());
			double straightOffset = straightBuilder.offsetFromTrack(parent.info, parent.getPos(), position);
			if(straightOffset<currentOffset){
				currentStateInt = 1;
				currentOffset = straightOffset;
			}
			currentState = fromInt(currentStateInt);

			//compare
			if(targetOffset > rail.info.settings.gauge.scale() / 16){
				if(targetState == SwitchState.TURN) {//应该改成最后一个？
					return currentState;
				}else {
					if(currentState == SwitchState.TURN){
						return SwitchState.NONE;
					}else {
						return currentState;
					}
				}
			}
		}

		if (parent.isSwitchForced()) {
			return parent.info.switchForced;
		}

		if(parent.info.settings.type == TrackItems.MULTISWITCH){
			return fromRedStoneLevel(RailPoweredLevel(rail));
		}else{
			if (isRailPowered(rail)) {
				return SwitchState.TURN;
			}
		}

		return SwitchState.STRAIGHT;
	}

	private static SwitchState fromRedStoneLevel(int redStoneLevel) {
		switch (redStoneLevel){
			case 0:
				return SwitchState.STRAIGHT;
			case 1:
			case 2:
			case 3:
				return SwitchState.MID1;
			case 4:
			case 5:
			case 6:
				return SwitchState.MID2;
			case 7:
			case 8:
			case 9:
				return SwitchState.MID3;
			case 10:
			case 11:
			case 12:
				return SwitchState.MID4;
			case 13:
			case 14:
			case 15:
				return SwitchState.TURN;
		}
		return SwitchState.STRAIGHT;
	}

	private static SwitchState fromInt(int i) {
		switch(i) {
			case 1:
				return SwitchState.STRAIGHT;
			case 2:
				return SwitchState.MID1;
			case 3:
				return SwitchState.MID2;
			case 4:
				return SwitchState.MID3;
			case 5:
				return SwitchState.MID4;
			case 6:
				return SwitchState.TURN;
		}
		return SwitchState.NONE;
	}

	public static boolean isRailPowered(TileRail rail) {
		Vec3d redstoneOrigin = rail.info.placementInfo.placementPosition.add(rail.getPos());
		double horiz = rail.info.settings.gauge.scale() * 1.1;
		if (Config.ConfigDebug.oldNarrowWidth && rail.info.settings.gauge.value() < 1) {
			horiz = horiz/2;
		}
		int scale = (int)Math.round(horiz);
		for (int x = -scale; x <= scale; x++) {
			for (int z = -scale; z <= scale; z++) {
				Vec3i gagPos = new Vec3i(redstoneOrigin.add(new Vec3d(x, 0, z)));
				TileRailBase gagRail = rail.getWorld().getBlockEntity(gagPos, TileRailBase.class);
				if (gagRail != null && (rail.getPos().equals(gagRail.getParent()) || gagRail.getReplaced() != null)) {
					if (rail.getWorld().getRedstone(gagPos) > 0) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static int RailPoweredLevel(TileRail rail) {
		Vec3d redstoneOrigin = rail.info.placementInfo.placementPosition.add(rail.getPos());
		double horiz = rail.info.settings.gauge.scale() * 1.1;
		if (Config.ConfigDebug.oldNarrowWidth && rail.info.settings.gauge.value() < 1) {
			horiz = horiz/2;
		}

		int maxPower = 0;

		int scale = (int)Math.round(horiz);
		for (int x = -scale; x <= scale; x++) {
			for (int z = -scale; z <= scale; z++) {
				Vec3i gagPos = new Vec3i(redstoneOrigin.add(new Vec3d(x, 0, z)));
				TileRailBase gagRail = rail.getWorld().getBlockEntity(gagPos, TileRailBase.class);
				if (gagRail != null && (rail.getPos().equals(gagRail.getParent()) || gagRail.getReplaced() != null)) {
					maxPower = Math.max(maxPower,rail.getWorld().getRedstone(gagPos));
				}
			}
		}
		if(maxPower>0)System.out.println("maxPower:"+maxPower);
		return maxPower;
	}
}
