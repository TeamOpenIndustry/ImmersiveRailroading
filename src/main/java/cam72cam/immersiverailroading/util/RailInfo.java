package cam72cam.immersiverailroading.util;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.Config.ConfigDamage;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.items.ItemTrackBlueprint;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.library.SwitchState;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.Gauge;
import cam72cam.immersiverailroading.library.TrackDirection;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.library.TrackPositionType;
import cam72cam.immersiverailroading.track.BuilderBase;
import cam72cam.immersiverailroading.track.BuilderCrossing;
import cam72cam.immersiverailroading.track.BuilderSlope;
import cam72cam.immersiverailroading.track.BuilderStraight;
import cam72cam.immersiverailroading.track.BuilderSwitch;
import cam72cam.immersiverailroading.track.BuilderTurn;
import cam72cam.immersiverailroading.track.BuilderTurnTable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class RailInfo {
	public BlockPos position;
	public World world;
	public EnumFacing facing;
	public TrackItems type;
	public TrackDirection direction;
	public int length;
	public int quarter;
	public int quarters;
	public Gauge gauge;
	public Vec3d placementPosition;
	public ItemStack railBed;
	public ItemStack railBedFill;
	public boolean gradeCrossing;

	// Used for tile rendering only
	public SwitchState switchState = SwitchState.NONE;
	public double tablePos = 0;
	public String renderIdCache; 
	
	
	public RailInfo(BlockPos position, World world, EnumFacing facing, TrackItems type, TrackDirection direction, int length, int quarter, int quarters, Gauge gauge, Vec3d placementPosition, ItemStack railBed, ItemStack railBedFill, SwitchState switchState, double tablePos, boolean gradeCrossing) {
		this.position = position;
		this.world = world;
		this.facing = facing;
		this.type = type;
		this.direction = direction;
		this.length = length;
		this.quarter = quarter;
		this.quarters = quarters;
		this.gauge = gauge;
		this.placementPosition = placementPosition;
		this.railBed = railBed;
		this.railBedFill = railBedFill;
		this.switchState = switchState;
		this.tablePos = tablePos;
		this.gradeCrossing = gradeCrossing;
	}
	
	public RailInfo(ItemStack stack, World worldIn, float yawHead, BlockPos pos, float hitX, float hitY, float hitZ) {
		position = pos;
		type = ItemTrackBlueprint.getType(stack);
		length = ItemTrackBlueprint.getLength(stack);
		quarters = ItemTrackBlueprint.getQuarters(stack);
		gauge = ItemGauge.get(stack);
		railBed = ItemTrackBlueprint.getBed(stack);
		railBedFill = ItemTrackBlueprint.getBedFill(stack);
		gradeCrossing = ItemTrackBlueprint.isGradeCrossing(stack);
		world = worldIn;
		TrackPositionType posType = ItemTrackBlueprint.getPosType(stack);
		direction = ItemTrackBlueprint.getDirection(stack);
		
		yawHead = yawHead % 360 + 360;
		if (direction == TrackDirection.NONE) {
			direction = (yawHead % 90 < 45) ? TrackDirection.LEFT : TrackDirection.RIGHT;
		}
		//quarter = MathHelper.floor((yawHead % 90f) /(90)*4);
		float yawPartial = (yawHead+3600) % 90f;
		if (direction == TrackDirection.RIGHT) {
			yawPartial = 90-yawPartial;
		}
		if (yawPartial < 90.0/8*1) {
			quarter = 0;
		} else if (yawPartial < 90.0/8*3) {
			quarter = 1;
		} else if (yawPartial < 90.0/8*5) {
			quarter = 2;
		} else if (yawPartial < 90.0/8*7){
			quarter = 3;
		} else {
			quarter = 0;
			if (direction == TrackDirection.RIGHT) {
				yawHead -= 90;
			} else {
				yawHead += 90;
			}
		}
		
		//facing = EnumFacing.fromAngle(yawHead);
		if (direction == TrackDirection.RIGHT) {
			facing = EnumFacing.fromAngle(yawHead + 45);
		} else {
			facing = EnumFacing.fromAngle(yawHead - 45);
		}

		
		switch(posType) {
		case FIXED:
			hitX = 0.5f;
			hitZ = 0.5f;
			break;
		case PIXELS:
			hitX = ((int)(hitX * 16)) / 16f;
			hitZ = ((int)(hitZ * 16)) / 16f;
			break;
		case PIXELS_LOCKED:
			hitX = ((int)(hitX * 16)) / 16f;
			hitZ = ((int)(hitZ * 16)) / 16f;
			
			if (quarter != 0) {
				break;
			}
			
			switch (facing) {
			case EAST:
			case WEST:
				hitZ = 0.5f;
				break;
			case NORTH:
			case SOUTH:
				hitX = 0.5f;
				break;
			default:
				break;
			}
			break;
		case SMOOTH:
			// NOP
			break;
		case SMOOTH_LOCKED:
			if (quarter != 0) {
				break;
			}
			
			switch (facing) {
			case EAST:
			case WEST:
				hitZ = 0.5f;
				break;
			case NORTH:
			case SOUTH:
				hitX = 0.5f;
				break;
			default:
				break;
			}
			break;
		}
		
		placementPosition = new Vec3d(pos).addVector(hitX, 0, hitZ);
		
		if (BlockUtil.canBeReplaced(world, pos.down(), true)) {
			pos = pos.down();
		}
	}
	
	@Override
	public RailInfo clone() {
		RailInfo c = new RailInfo(position, world, facing, type, direction, length, quarter, quarters, gauge, placementPosition, railBed, railBedFill, switchState, tablePos, gradeCrossing);
		return c;
	}
	
	public BuilderBase getBuilder(BlockPos pos) {
		switch (type) {
		case STRAIGHT:
			return new BuilderStraight(this, pos);
		case CROSSING:
			return new BuilderCrossing(this, pos);
		case SLOPE:
			return new BuilderSlope(this, pos);
		case TURN:
			return new BuilderTurn(this, pos);
		case SWITCH:
			return new BuilderSwitch(this, pos);
		case TURNTABLE:
			return new BuilderTurnTable(this, pos);
		}
		return null;
	}
	
	private BuilderBase builder;
	public BuilderBase getBuilder() {
		if (builder == null) {
			builder = getBuilder(new BlockPos(0,0,0));
		}
		return builder;
	}
	
	public boolean build(EntityPlayer player, BlockPos pos) {
		BuilderBase builder = getBuilder(pos);
		
		if (player.isCreative() && ConfigDamage.creativePlacementClearsBlocks) {
			if (!world.isRemote) {
				builder.clearArea();
			}
		}
		

		if (builder.canBuild()) {
			if (!world.isRemote) {
				if (player.isCreative()) {
					builder.build();
					return true;
				}
				
				// Survival check
				
				int ties = 0;
				int rails = 0;
				int bed = 0;
				int fill = 0;
				
				for (ItemStack playerStack : player.inventory.mainInventory) {
					if (playerStack.getItem() == IRItems.ITEM_RAIL && ItemGauge.get(playerStack) == builder.gauge) {
						rails += playerStack.getCount();
					}
					if (OreHelper.IR_TIE.matches(playerStack, false)) {
						ties += playerStack.getCount();
					}
					if (railBed.getItem() != Items.AIR && railBed.getItem() == playerStack.getItem() && railBed.getMetadata() == playerStack.getMetadata()) {
						bed += playerStack.getCount();
					}
					if (railBedFill.getItem() != Items.AIR && railBedFill.getItem() == playerStack.getItem() && railBedFill.getMetadata() == playerStack.getMetadata()) {
						fill += playerStack.getCount();
					}
				}
				
				boolean isOk = true;
				
				if (ties < builder.costTies()) {
					player.sendMessage(ChatText.BUILD_MISSING_TIES.getMessage(builder.costTies() - ties));
					isOk = false;
				}
				
				if (rails < builder.costRails()) {
					player.sendMessage(ChatText.BUILD_MISSING_RAILS.getMessage(builder.costRails() - rails));
					isOk = false;
				}
				
				if (railBed.getItem() != Items.AIR && bed < builder.costBed()) {
					player.sendMessage(ChatText.BUILD_MISSING_RAIL_BED.getMessage(builder.costBed() - bed));
					isOk = false;
				}
				
				if (railBedFill.getItem() != Items.AIR && fill < builder.costFill()) {
					player.sendMessage(ChatText.BUILD_MISSING_RAIL_BED_FILL.getMessage(builder.costFill() - fill));
					isOk = false;
				}
				
				if (!isOk) {
					return false;
				}

				ties = builder.costTies();
				rails = builder.costRails();
				bed = builder.costBed();
				fill = builder.costFill();
				List<ItemStack> drops = new ArrayList<ItemStack>();
				
				for (ItemStack playerStack : player.inventory.mainInventory) {
					if (playerStack.getItem() == IRItems.ITEM_RAIL && ItemGauge.get(playerStack) == builder.gauge) {
						if (rails > playerStack.getCount()) {
							rails -= playerStack.getCount();
							ItemStack copy = playerStack.copy();
							copy.setCount(playerStack.getCount());
							drops.add(copy); 
							playerStack.setCount(0);
						} else if (rails != 0) {
							ItemStack copy = playerStack.copy();
							copy.setCount(rails);
							drops.add(copy); 
							playerStack.setCount(playerStack.getCount() - rails);
							rails = 0;
						}
					}
					if (OreHelper.IR_TIE.matches(playerStack, false)) {
						if (ties > playerStack.getCount()) {
							ties -= playerStack.getCount();
							ItemStack copy = playerStack.copy();
							copy.setCount(playerStack.getCount());
							drops.add(copy);  
							playerStack.setCount(0);
						} else if (ties != 0) {
							ItemStack copy = playerStack.copy();
							copy.setCount(ties);
							drops.add(copy); 
							playerStack.setCount(playerStack.getCount() - ties);
							ties = 0;
						}
					}
					if (railBed.getItem() != Items.AIR && railBed.getItem() == playerStack.getItem() && railBed.getMetadata() == playerStack.getMetadata()) {
						if (bed > playerStack.getCount()) {
							bed -= playerStack.getCount();
							ItemStack copy = playerStack.copy();
							copy.setCount(playerStack.getCount());
							drops.add(copy);  
							playerStack.setCount(0);
						} else if (bed != 0) {
							ItemStack copy = playerStack.copy();
							copy.setCount(bed);
							drops.add(copy); 
							playerStack.setCount(playerStack.getCount() - bed);
							bed = 0;
						}
					}
					if (railBedFill.getItem() != Items.AIR && railBedFill.getItem() == playerStack.getItem() && railBedFill.getMetadata() == playerStack.getMetadata()) {
						if (fill > playerStack.getCount()) {
							fill -= playerStack.getCount();
							ItemStack copy = playerStack.copy();
							copy.setCount(playerStack.getCount());
							//drops.add(copy);  
							playerStack.setCount(0);
						} else if (fill != 0) {
							ItemStack copy = playerStack.copy();
							copy.setCount(fill);
							//drops.add(copy); 
							playerStack.setCount(playerStack.getCount() - fill);
							fill = 0;
						}
					}
				}
				builder.setDrops(drops);
				builder.build();
				return true;
			}
		}
		return false;
	}
}
