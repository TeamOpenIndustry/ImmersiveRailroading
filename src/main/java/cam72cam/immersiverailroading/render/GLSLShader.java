package cam72cam.immersiverailroading.render;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import net.minecraft.util.ResourceLocation;

public class GLSLShader {
	private int program;
	private int oldProc;
	public GLSLShader(String vert, String frag) {
		int vertShader = ARBShaderObjects.glCreateShaderObjectARB(ARBVertexShader.GL_VERTEX_SHADER_ARB);
		int fragShader = ARBShaderObjects.glCreateShaderObjectARB(ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);
		ARBShaderObjects.glShaderSourceARB(vertShader, readShader(vert));
		ARBShaderObjects.glCompileShaderARB(vertShader);
        if (ARBShaderObjects.glGetObjectParameteriARB(vertShader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE)
        throw new RuntimeException("Error creating shader: " + getLogInfo(vertShader));
		
		ARBShaderObjects.glShaderSourceARB(fragShader, readShader(frag));
		ARBShaderObjects.glCompileShaderARB(fragShader);
        if (ARBShaderObjects.glGetObjectParameteriARB(fragShader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE)
        throw new RuntimeException("Error creating shader: " + getLogInfo(fragShader));
		
		program = ARBShaderObjects.glCreateProgramObjectARB();
		ARBShaderObjects.glAttachObjectARB(program, vertShader);
		ARBShaderObjects.glAttachObjectARB(program, fragShader);
		ARBShaderObjects.glLinkProgramARB(program);
	    if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
	        throw new RuntimeException("Error creating shader: " + getLogInfo(program));
        }
	    ARBShaderObjects.glValidateProgramARB(program);
        if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_VALIDATE_STATUS_ARB) == GL11.GL_FALSE) {
	        throw new RuntimeException("Error creating shader: " + getLogInfo(program));
        }
	}
	
	public void bind() {
		oldProc = ARBShaderObjects.glGetHandleARB(ARBShaderObjects.GL_PROGRAM_OBJECT_ARB);
		ARBShaderObjects.glUseProgramObjectARB(program);
	}
	public void unbind() {
		ARBShaderObjects.glUseProgramObjectARB(oldProc);
	}
	
	public void paramFloat(String name, float... params) {
		switch(params.length) {
		case 1:
			ARBShaderObjects.glUniform1fARB(ARBShaderObjects.glGetUniformLocationARB(program, name), params[0]);
			break;
		case 3:
			ARBShaderObjects.glUniform3fARB(ARBShaderObjects.glGetUniformLocationARB(program, name), params[0], params[1], params[2]);
			break;
		}
	}
	
	
	private String readShader(String fname) {
		InputStream input;
		try {
			input = ImmersiveRailroading.proxy.getResourceStream(new ResourceLocation("immersiverailroading:particles/" + fname));
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Error reading shader " + fname);
		}
		Scanner reader = new Scanner(input);
		String text = "";
		while(reader.hasNextLine()) {
			text = text + reader.nextLine();
		}
		reader.close(); // closes input
		return text;
	}
	private static String getLogInfo(int obj) {
	    return ARBShaderObjects.glGetInfoLogARB(obj, ARBShaderObjects.glGetObjectParameteriARB(obj, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB));
    }
}
