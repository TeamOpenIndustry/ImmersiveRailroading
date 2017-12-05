package cam72cam.immersiverailroading.blocks;

import net.minecraftforge.common.property.IUnlistedProperty;

public class PropertyEnum<T extends Enum<T>> implements IUnlistedProperty<T> {
	
	private String name;
	private Class<T> classy;

	public PropertyEnum(String name, Class<T> classy) {
		this.name = name;
		this.classy = classy;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isValid(T value) {
		return true;
	}

	@Override
	public Class<T> getType() {
		return classy;
	}

	@Override
	public String valueToString(T value) {
		return value != null ? value.toString() : "null";
	}
}
