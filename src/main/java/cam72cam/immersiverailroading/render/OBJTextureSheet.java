package cam72cam.immersiverailroading.render;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.model.obj.Face;
import cam72cam.immersiverailroading.model.obj.OBJModel;
import cam72cam.immersiverailroading.model.obj.Vec2f;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

public class OBJTextureSheet {
	public Map<String, SubTexture> mappings;
	private int sheetWidth = 0;
	private int sheetHeight = 0;
	public final int textureID;
	
	private  class SubTexture {
		private int realWidth;
		private int realHeight;
		private int sheetWidth;
		private int sheetHeight;
		private int originX;
		private int originY;
		private int minU = 0;
		private int minV = 0;
		private int maxU = 1;
		private int maxV = 1;

		private BufferedImage image;
		private ResourceLocation tex;

		
		SubTexture(ResourceLocation tex) throws IOException {
			InputStream input = ImmersiveRailroading.proxy.getResourceStream(tex);
			this.tex = tex;
			this.image = TextureUtil.readBufferedImage(input);
			realWidth = image.getWidth();
			realHeight = image.getHeight();
			//System.out.println(tex);
			//System.out.println(realWidth);
			//System.out.println(realHeight);
		}
		public void extendSpace(Vec2f tex) {
			float u = tex.x;
			float v = -tex.y;
			minU = MathHelper.floor(Math.min(minU, u));
			maxU = MathHelper.ceil(Math.max(maxU, u));
			minV = MathHelper.floor(Math.min(minV, v));
			maxV = MathHelper.ceil(Math.max(maxV, v));
		}
		public void upload(int textureID, int originX, int originY, int sheetWidth, int sheetHeight) {
			this.originX = originX;
			this.originY = originY;
			this.sheetWidth = sheetWidth;
			this.sheetHeight = sheetHeight;
			for (int cU = 0; cU < copiesU(); cU++) {
				for (int cV = 0; cV < copiesV(); cV++) {
					int offX = originX + this.realWidth * cU;
					int offY = originY + this.realHeight * cV;
					TextureUtil.uploadTextureImageSub(textureID, image, offX, offY, false, false);
				}
			}
			image = null;
		}
		public int copiesU() {
			return (maxU - minU);
		}
		public int copiesV() {
			return (maxV - minV);
		}
		public int getAbsoluteWidth() {
			return realWidth * copiesU();
		}
		public int getAbsoluteHeight() {
			return realHeight * copiesV();
		}
		
		public float convertU(float relativeU) {
			return originX / (float)sheetWidth + (relativeU - minU) * ((float)this.realWidth / sheetWidth);
		}
		
		public float convertV(float relativeV) {
			return originY / (float)sheetHeight + (relativeV - minV) * ((float)this.realHeight / sheetHeight);
		}
	}
	
	public OBJTextureSheet(OBJModel model) {
		mappings = new HashMap<String, SubTexture>();
		for (String groupName : model.groups.keySet()) {
			List<Face> quads = model.groups.get(groupName);
			for (Face face : quads) {
				String mtlName = face.mtl;
				if (model.materials.get(mtlName).texKd == null) {
					continue;
				}
				if (!mappings.containsKey(mtlName)) {
					try {
						mappings.put(mtlName, new SubTexture(model.materials.get(mtlName).texKd));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				for (int[] point : face.points) {
					Vec2f vt = point[1] != -1 ? model.vertexTextures.get(point[1]) : null;
					if (vt != null) {
						mappings.get(mtlName).extendSpace(vt);
					}
				}
			}
		}
		/*
		for (SubTexture tex : mappings.values()) {
			this.sheetWidth += tex.getAbsoluteWidth();
			this.sheetHeight += tex.getAbsoluteHeight();
		}*/
		int maxSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
		
		int currentX = 0;
		int currentY = 0;
		int rowHeight = 0;
		
		for (SubTexture tex : mappings.values()) {
			if (currentX + tex.getAbsoluteWidth() > maxSize) {
				currentX = 0;
				currentY += rowHeight;
				rowHeight = 0;
			}
			rowHeight = Math.max(rowHeight, tex.getAbsoluteHeight());
			currentX += tex.getAbsoluteWidth();
			currentY += tex.getAbsoluteHeight();
			this.sheetWidth = Math.max(this.sheetWidth, currentX);
			this.sheetHeight = Math.max(this.sheetHeight, currentY); 
		}
		
		textureID = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
		
		currentX = 0;
		currentY = 0;
		rowHeight = 0;
		

		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		
		TextureUtil.allocateTexture(textureID, sheetWidth, sheetHeight);
		
		for (SubTexture tex : mappings.values()) {
			if (tex.getAbsoluteWidth() > maxSize) {
				System.out.println("ASJDKLAJSKLDJASLDKJASKLJFKLADJFKLSDJAKLFJSDLKJSDKLA");
			}
			if (currentX + tex.getAbsoluteWidth() > maxSize) {
				currentX = 0;
				currentY += rowHeight;
				rowHeight = 0;
				System.out.println(String.format("NEWLINE %s %s", currentX, currentY));
			}
			rowHeight = Math.max(rowHeight, tex.getAbsoluteHeight());
			tex.upload(textureID, currentX, currentY, sheetWidth, sheetHeight);
			currentX += tex.getAbsoluteWidth();
			currentY += tex.getAbsoluteHeight();
		}
	}
	
	public float convertU(String mtlName, float u) {
		if (mappings.containsKey(mtlName)) {
			return mappings.get(mtlName).convertU(u);
		}
		return 0;
	}
	public float convertV(String mtlName, float v) {
		if (mappings.containsKey(mtlName)) {
			return mappings.get(mtlName).convertV(v);
		}
		return 0;
	}
}
