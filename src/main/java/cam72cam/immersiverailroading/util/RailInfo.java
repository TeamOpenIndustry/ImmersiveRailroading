package cam72cam.immersiverailroading.util;

import cam72cam.immersiverailroading.Config.ConfigDamage;
import cam72cam.immersiverailroading.items.ItemTrackBlueprint;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.items.nbt.RailSettings;
import cam72cam.immersiverailroading.library.*;
import cam72cam.immersiverailroading.model.TrackModel;
import cam72cam.immersiverailroading.registry.DefinitionManager;
import cam72cam.immersiverailroading.registry.TrackDefinition;
import cam72cam.immersiverailroading.track.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RailInfo {
	public final World world;
	public final RailSettings settings;
	public final PlacementInfo placementInfo;
	public final PlacementInfo customInfo;

	// Used for tile rendering only
	public final SwitchState switchState;
	public final SwitchState switchForced;
	public final double tablePos;
	public final String uniqueID;


	public RailInfo(World world, RailSettings settings, PlacementInfo placementInfo, PlacementInfo customInfo, SwitchState switchState, SwitchState switchForced, double tablePos) {
		if (customInfo == null) {
			customInfo = placementInfo;
		}

		this.world = world;
		this.settings = settings;
		this.placementInfo = placementInfo;
		this.customInfo = customInfo;
		this.switchState = switchState;
		this.switchForced = switchForced;
		this.tablePos = tablePos;

		Object[] props = new Object [] {
				this.settings.type,
				this.settings.length,
				this.settings.quarters,
				this.settings.railBed,
				this.settings.gauge,
				this.settings.track,
				this.switchState,
				this.switchForced,
				this.tablePos,
				this.placementInfo.yaw,
				this.placementInfo.direction,
				this.customInfo.yaw,
				this.customInfo.direction,
		};
		String id = Arrays.toString(props);
		if (!placementInfo.placementPosition.equals(customInfo.placementPosition) || this.settings.posType != TrackPositionType.FIXED) {
			id += placementInfo.placementPosition.subtract(customInfo.placementPosition);
		}
		if (placementInfo.control != null) {
			id += placementInfo.control;
		}
		if (customInfo.control != null) {
			id += customInfo.control;
		}
		uniqueID = id;
	}

	public RailInfo(World world, ItemStack settings, PlacementInfo placementInfo, PlacementInfo customInfo) {
		this(world, ItemTrackBlueprint.settings(settings), placementInfo, customInfo, SwitchState.NONE, SwitchState.NONE, 0);
	}

	public RailInfo(World world, BlockPos pos, NBTTagCompound nbt) {
		this(
				world,
				new RailSettings(nbt.getCompoundTag("settings")),
				new PlacementInfo(nbt.getCompoundTag("placement"), pos),
				new PlacementInfo(nbt.getCompoundTag("custom"), pos),
				SwitchState.values()[nbt.getInteger("switchState")],
				SwitchState.values()[nbt.getInteger("switchForced")],
				nbt.getDouble("tablePos")
		);
	}

	public NBTTagCompound toNBT(BlockPos pos) {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setTag("settings", settings.toNBT());
		nbt.setTag("placement", placementInfo.toNBT(pos));
		nbt.setTag("custom", customInfo.toNBT(pos));
		nbt.setInteger("switchState", switchState.ordinal());
		nbt.setInteger("switchForced", switchForced.ordinal());
		nbt.setDouble("tablePos", tablePos);
		return nbt;
	}

	@Override
	public RailInfo clone() {
		return new RailInfo(world, settings, placementInfo, customInfo, switchState, switchForced, tablePos);
	}

	public RailInfo withLength(int length) {
		RailSettings settings = this.settings.withLength(length);
		return new RailInfo(world, settings, placementInfo, customInfo, switchState, switchForced, tablePos);
	}

	public RailInfo withType(TrackItems type) {
		RailSettings settings = this.settings.withType(type);
		return new RailInfo(world, settings, placementInfo, customInfo, switchState, switchForced, tablePos);
	}

	public Map<BlockPos, BuilderBase> builders = new HashMap<>();
	public BuilderBase getBuilder(BlockPos pos) {
		if (builders.containsKey(pos)) {
			return builders.get(pos);
		}
		builders.put(pos, constructBuilder(pos));
		return builders.get(pos);
	}
	private BuilderBase constructBuilder(BlockPos pos) {
		switch (settings.type) {
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
		case CUSTOM:
			return new BuilderCubicCurve(this, pos);
		}
		return null;
	}

	public BuilderBase getBuilder() {
		return getBuilder(BlockPos.ORIGIN);
	}

	private class MaterialManager {
		private final Function<ItemStack, Boolean> material;
		private final int count;
		private final ItemStack[] examples;

		MaterialManager(int count, Function<ItemStack, Boolean> material, ItemStack ...examples) {
			this.material = material;
			this.count = count;
			this.examples = examples;
		}

		public MaterialManager(int count, Function<ItemStack, Boolean> material, List<ItemStack> examples) {
			this(count, material, examples.toArray(new ItemStack[0]));
		}

		private boolean checkMaterials(EntityPlayer player) {
			int found = 0;
			for (ItemStack stack : player.inventory.mainInventory) {
				if (material.apply(stack) && (!ItemGauge.has(stack) || ItemGauge.get(stack) == settings.gauge)) {
					found += stack.getCount();
				}
			}

			if (found < count) {
				String example = Arrays.stream(examples).map(ItemStack::getDisplayName).collect(Collectors.joining(" | "));
				if (examples.length > 1) {
					example = "[ " + example + " ]";
				}
				player.sendMessage(ChatText.BUILD_MISSING.getMessage(count - found, example));
				return false;
			}
			return true;
		}

		private List<ItemStack> useMaterials(EntityPlayer player) {
			List<ItemStack> drops = new ArrayList<>();
			int required = this.count;
			for (ItemStack stack : player.inventory.mainInventory) {
				if (material.apply(stack) && (!ItemGauge.has(stack) || ItemGauge.get(stack) == settings.gauge)) {
					if (required > stack.getCount()) {
						required -= stack.getCount();
						ItemStack copy = stack.copy();
						copy.setCount(stack.getCount());
						drops.add(copy);
						stack.setCount(0);
					} else if (required != 0) {
						ItemStack copy = stack.copy();
						copy.setCount(required);
						drops.add(copy);
						stack.setCount(stack.getCount() - required);
						required = 0;
					}
				}
			}
			return drops;
		}
	}

	public boolean build(EntityPlayer player) {
		BuilderBase builder = getBuilder(new BlockPos(placementInfo.placementPosition));

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

				TrackDefinition def = getDefinition();

				List<MaterialManager> materials = new ArrayList<>();

				if (settings.railBed.getItem() != Items.AIR) {
					materials.add(new MaterialManager(builder.costBed(), settings.railBed::isItemEqual, settings.railBed));
				}
				if (settings.railBedFill.getItem() != Items.AIR) {
					materials.add(new MaterialManager(builder.costFill(), settings.railBedFill::isItemEqual, settings.railBedFill));
				}

				List<TrackDefinition.TrackMaterial> tieParts = def.materials.get(TrackComponent.TIE);
				List<TrackDefinition.TrackMaterial> railParts = def.materials.get(TrackComponent.RAIL);
				List<TrackDefinition.TrackMaterial> bedParts = def.materials.get(TrackComponent.BED);

				if (tieParts != null) {
					for (TrackDefinition.TrackMaterial tiePart : tieParts) {
						materials.add(new MaterialManager((int) Math.ceil(builder.costTies() * tiePart.cost), tiePart::matches, tiePart.examples()));
					}
				}
				if (railParts != null) {
					for (TrackDefinition.TrackMaterial railPart : railParts) {
						materials.add(new MaterialManager((int) Math.ceil(builder.costRails() * railPart.cost), railPart::matches, railPart.examples()));
					}
				}
				if (bedParts != null) {
					for (TrackDefinition.TrackMaterial bedPart : bedParts) {
						materials.add(new MaterialManager((int) Math.ceil(builder.costBed() * bedPart.cost), bedPart::matches, bedPart.examples()));
					}
				}

				boolean isOk = true;
				for (MaterialManager material : materials) {
					isOk = isOk & material.checkMaterials(player);
				}
				if (!isOk) {
					return false;
				}

				List<ItemStack> drops = new ArrayList<>();

				for (MaterialManager material : materials) {
					drops.addAll(material.useMaterials(player));
				}

				builder.setDrops(drops);
				builder.build();
				return true;
			}
		}
		return false;
	}

	public TrackDefinition getDefinition() {
		return DefinitionManager.getTrack(settings.track);
	}

	public TrackModel getTrackModel() {
		return DefinitionManager.getTrack(settings.track, settings.gauge.value());
	}

	private double trackHeight = -1;
	public double getTrackHeight() {
		if (trackHeight == -1) {
			TrackModel model = getTrackModel();
			trackHeight = model.getHeight();
		}
		return trackHeight;
	}

}
