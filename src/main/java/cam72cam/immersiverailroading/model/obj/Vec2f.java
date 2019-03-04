package cam72cam.immersiverailroading.model.obj;

public class Vec2f {

	public static final Vec2f ZERO = new Vec2f(0, 0);

	public final float x;
	public final float y;

	public Vec2f(float x, float y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Vec2f)) {
			return false;
		} else {
			Vec2f v = (Vec2f) other;
			return v.x == x && v.y == y;
		}
	}

	@Override
	public int hashCode() {
		return Float.hashCode(x) * 31 + Float.hashCode(y);
	}

}
