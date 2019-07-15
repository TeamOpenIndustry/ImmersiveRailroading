package cam72cam.mod.model.obj;

import cam72cam.mod.util.Identifier;

import java.nio.FloatBuffer;

public class Material {
	public String name;
	
	public Identifier texKd;
	
	public FloatBuffer Ka;
	public FloatBuffer Kd;
	public FloatBuffer Ks;
}
