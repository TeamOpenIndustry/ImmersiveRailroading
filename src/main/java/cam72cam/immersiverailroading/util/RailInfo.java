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
import cam72cam.mod.world.World;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.TagCompound;

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
				this.settings.isGradeCrossing,
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

	public RailInfo(World world, Vec3i pos, TagCompound nbt) {
		this(
				world,
				new RailSettings(nbt.get("settings")),
				new PlacementInfo(nbt.get("placement"), pos),
				new PlacementInfo(nbt.get("custom"), pos),
				SwitchState.values()[nbt.getInteger("switchState")],
				SwitchState.values()[nbt.getInteger("switchForced")],
				nbt.getDouble("tablePos")
		);
	}

	public TagCompound toNBT(Vec3i pos) {
		TagCompound nbt = new TagCompound();
		nbt.set("settings", settings.toNBT());
		nbt.set("placement", placementInfo.toNBT(pos));
		nbt.set("custom", customInfo.toNBT(pos));
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
	
	public RailInfo withTrack(String track) {
		RailSettings settings = this.settings.withTrack(track);
		return new RailInfo(world, settings, placementInfo, customInfo, switchState, switchForced, tablePos);
	}

	public Map<Vec3i, BuilderBase> builders = new HashMap<>();
	public BuilderBase getBuilder(Vec3i pos) {
		if (builders.containsKey(pos)) {
			return builders.get(pos);
		}
		builders.put(pos, constructBuilder(pos));
		return builders.get(pos);
	}
	private BuilderBase constructBuilder(Vec3i pos) {
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
		return getBuilder(Vec3i.ZERO);
	}

	private class MaterialManager {
		private final Function<ItemStack, Boolean> material;
		private final int count;
		private final ItemStack[] examples;
		private final boolean isDrop;

		MaterialManager(boolean isDrop, int count, Function<ItemStack, Boolean> material, ItemStack... examples) {
			this.material = material;
			this.count = count;
			this.examples = examples;
			this.isDrop = isDrop;
		}

		public MaterialManager(int count, Function<ItemStack, Boolean> material, List<ItemStack> examples) {
			this(true, count, material, examples.toArray(new ItemStack[0]));
		}

		private boolean checkMaterials(Player player) {
			int found = 0;
			for (int i = 0; i < player.getInventory().getSlotCount(); i++) {
				ItemStack stack = player.getInventory().get(i);
				if (material.apply(stack) && (!ItemGauge.has(stack) || ItemGauge.get(stack) == settings.gauge)) {
					found += stack.getCount();
				}
			}

			if (found < count) {
				Set<String> exStrs = Arrays.stream(examples).map(ItemStack::getDisplayName).collect(Collectors.toSet());
				String example = String.join(" | ", exStrs);
				if (exStrs.size() > 1) {
					example = "[ " + example + " ]";
				}
				player.sendMessage(ChatText.BUILD_MISSING.getMessage(count - found, example));
				return false;
			}
			return true;
		}

		private List<ItemStack> useMaterials(Player player) {
			List<ItemStack> drops = new ArrayList<>();
			int required = this.count;
			for (int i = 0; i < player.getInventory().getSlotCount(); i++) {
				ItemStack stack = player.getInventory().get(i);
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
			return this.isDrop ? drops : Collections.emptyList();
		}
	}

	public boolean build(Player player) {
		return this.build(player, true);
	}

	public boolean build(Player player, boolean placeTrack) {
		BuilderBase builder = getBuilder(new Vec3i(placementInfo.placementPosition));

		if (player.isCreative() && ConfigDamage.creativePlacementClearsBlocks && placeTrack) {
			if (world.isServer) {
				builder.clearArea();
			}
		}

		if (!placeTrack || (placeTrack && builder.canBuild())) {
			if (world.isServer) {
				if (player.isCreative() && placeTrack) {
					builder.build();
					return true;
				}

				// Survival check

				TrackDefinition def = getDefinition();

				List<MaterialManager> materials = new ArrayList<>();

				if (!settings.railBed.isEmpty()) {
					materials.add(new MaterialManager(true, builder.costBed(), settings.railBed::equals, settings.railBed));
				}
				if (!settings.railBedFill.isEmpty()) {
					materials.add(new MaterialManager(false, builder.costFill(), settings.railBedFill::equals, settings.railBedFill));
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
				if (placeTrack) builder.build();
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
