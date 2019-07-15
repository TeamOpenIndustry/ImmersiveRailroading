package cam72cam.mod.model.obj;

import cam72cam.mod.resource.Identifier;

import java.nio.FloatBuffer;

public class Material {
	public String name;
	
	public Identifier texKd;
	
	public FloatBuffer Ka;
	public FloatBuffer Kd;
	public FloatBuffer Ks;
}
