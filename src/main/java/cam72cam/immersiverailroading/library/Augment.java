package cam72cam.immersiverailroading.library;

import cam72cam.mod.render.Color;
import cam72cam.mod.serialization.SerializationException;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapped;
import cam72cam.mod.text.TextUtil;

public enum Augment {
	SPEED_RETARDER,
	WATER_TROUGH,
	LOCO_CONTROL,
	ITEM_LOADER,
	ITEM_UNLOADER,
	FLUID_LOADER,
	FLUID_UNLOADER,
	DETECTOR,
	COUPLER,
	ACTUATOR,
	;
	
	public Color color() {
		switch (this) {
		case DETECTOR:
			return Color.RED;
		case FLUID_LOADER:
			return Color.BLUE;
		case FLUID_UNLOADER:
			return Color.LIGHT_BLUE;
		case ITEM_LOADER:
			return Color.GREEN;
		case ITEM_UNLOADER:
			return Color.LIME;
		case LOCO_CONTROL:
			return Color.BLACK;
		case SPEED_RETARDER:
			return Color.GRAY;
		case WATER_TROUGH:
			return Color.CYAN;
		case COUPLER:
			return Color.ORANGE;
		case ACTUATOR:
			return Color.SILVER;
		}
		return Color.WHITE;
	}

	@Override
	public String toString() {
		return TextUtil.translate("item.immersiverailroading:item_augment." + this.name() + ".name");
	}

	@TagMapped(PropertyMapper.class)
	public static class Properties {
		public static final Properties EMPTY = new Properties("", "", "",
															  CouplerAugmentMode.ENGAGED,
															  LocoControlMode.THROTTLE,
															  RedstoneMode.ENABLED,
															  true,
															  StockDetectorMode.SIMPLE);

		public String positiveFilter;
		public String negativeFilter;
		public String doorActuatorFilter;
		public CouplerAugmentMode couplerAugmentMode;
		public LocoControlMode locoControlMode;
		public RedstoneMode redstoneMode;
		public boolean pushpull;
		public StockDetectorMode stockDetectorMode;

		public Properties() {
		}

		public Properties(String positiveFilter, String negativeFilter, String doorActuatorFilter, CouplerAugmentMode couplerAugmentMode,
						  LocoControlMode locoControlMode, RedstoneMode redstoneMode, boolean pushpull, StockDetectorMode stockDetectorMode) {
			this.positiveFilter = positiveFilter;
			this.negativeFilter = negativeFilter;
			this.doorActuatorFilter = doorActuatorFilter;
			this.couplerAugmentMode = couplerAugmentMode;
			this.locoControlMode = locoControlMode;
			this.redstoneMode = redstoneMode;
			this.pushpull = pushpull;
			this.stockDetectorMode = stockDetectorMode;
		}

		public TagCompound toNBT() {
			TagCompound compound = new TagCompound();
			compound.setString("positive", positiveFilter);
			compound.setString("negative", negativeFilter);
			compound.setString("door_actuator", doorActuatorFilter);
			compound.setString("coupler", couplerAugmentMode.name());
			compound.setString("loco", locoControlMode.name());
			compound.setString("redstone", redstoneMode.name());
			compound.setBoolean("pushpull", pushpull);
			compound.setString("detector", stockDetectorMode.name());
			return compound;
		}

		public static Properties fromNBT(TagCompound compound) {
			Properties properties = new Properties(
					compound.getString("positive"),
					compound.getString("negative"),
					compound.getString("door_actuator"),
					CouplerAugmentMode.valueOf(compound.getString("coupler")),
					LocoControlMode.valueOf(compound.getString("loco")),
					RedstoneMode.valueOf(compound.getString("redstone")),
					compound.getBoolean("pushpull"),
					StockDetectorMode.valueOf(compound.getString("detector")));
			return properties;
		}
	}

	public static class PropertyMapper implements cam72cam.mod.serialization.TagMapper<Properties> {
		@Override
		public TagAccessor<Properties> apply(Class<Properties> type, String fieldName, TagField tag) throws SerializationException {
			return new TagAccessor<>(
					(t, p) -> {
						if (p == null) {
							t.remove(fieldName);
						} else {
							t.set(fieldName, p.toNBT());
						}
					},
					t -> t.hasKey(fieldName) ? Properties.fromNBT(t.get(fieldName)) : Properties.EMPTY
			);
		}
	}
}
