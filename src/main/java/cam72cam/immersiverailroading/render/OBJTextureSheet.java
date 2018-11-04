package cam72cam.immersiverailroading.render;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ConfigGraphics;
import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.model.obj.Material;
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
	private OBJModel model;
	
	private  class SubTexture {
		public int realWidth;
		private int realHeight;
		private int sheetWidth;
		private int sheetHeight;
		private int originX;
		private int originY;
		private int minU = 0;
		private int minV = 0;
		private int maxU = 1;
		private int maxV = 1;
		public ResourceLocation tex;

		private boolean isFlatMaterial;
		private int[] pixels;
		
		public final int sampPx;

		
		SubTexture(ResourceLocation tex, ResourceLocation fallback, Function<Integer, Integer> scale) throws IOException {
			InputStream input;
			try {
				input = ImmersiveRailroading.proxy.getResourceStream(tex);
			} catch (FileNotFoundException ex) {
				input = ImmersiveRailroading.proxy.getResourceStream(fallback);
			}
			BufferedImage image = TextureUtil.readBufferedImage(input);
			input.close();
			
			realWidth = image.getWidth();
			realHeight = image.getHeight();
			
			if (scale != null) {
				image = convertToBufferedImage(image.getScaledInstance(scale.apply(realWidth), scale.apply(realHeight), BufferedImage.SCALE_FAST));
				realWidth = image.getWidth();
				realHeight = image.getHeight();
			}
					
			this.tex = tex;
			isFlatMaterial = false;

			pixels = new int[realWidth * realHeight];
			image.getRGB(0, 0, realWidth, realHeight, pixels, 0, realWidth);
	        sampPx = pixels[0];
		}
		SubTexture(String name, int r, int g, int b, int a) {
			BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
			for (int x = 0; x < 8; x ++) {
				for (int y = 0; y < 8; y ++) {					
					image.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
				}
			}
			realWidth = image.getWidth();
			realHeight = image.getHeight();
			minU = 0;
			maxU = 1;
			minV = 0;
			maxV = 1;
			this.tex = new ResourceLocation("generated:" + name);
			isFlatMaterial = true;
			
			pixels = new int[realWidth * realHeight];
			image.getRGB(0, 0, realWidth, realHeight, pixels, 0, realWidth);
	        
	        sampPx = pixels[0];
		}
		
		public Vec2f extendSpace(List<Vec2f> vts) {
			float vminU = vts.get(0).x;
			float vmaxU = vts.get(0).x;
			float vminV = -vts.get(0).y;
			float vmaxV = -vts.get(0).y;
			
			for (Vec2f vt : vts) {
				float u = vt.x;
				float v = -vt.y;
				vminU = Math.min(vminU, u);
				vmaxU = Math.max(vmaxU, u);
				vminV = Math.min(vminV, v);
				vmaxV = Math.max(vmaxV, v);
			}
			
			Vec2f offset = new Vec2f((float)Math.floor(vminU), (float)Math.floor(vminV));
			
			vminU -= offset.x;
			vmaxU -= offset.x;
			vminV -= offset.y;
			vmaxV -= offset.y;
			
			minU = MathHelper.floor(Math.min(minU, vminU));
			maxU = MathHelper.ceil(Math.max(maxU, vmaxU));
			minV = MathHelper.floor(Math.min(minV, vminV));
			maxV = MathHelper.ceil(Math.max(maxV, vmaxV));
			
			return offset;
		}
		
		public BufferedImage convertToBufferedImage(Image image)
		{
		    BufferedImage newImage = new BufferedImage(
		        image.getWidth(null), image.getHeight(null),
		        BufferedImage.TYPE_INT_ARGB);
		    Graphics2D g = newImage.createGraphics();
		    g.drawImage(image, 0, 0, null);
		    g.dispose();
		    return newImage;
		}
		
		public void upload(int textureID, int originX, int originY, int sheetWidth, int sheetHeight) {
			this.originX = originX;
			this.originY = originY;
			this.sheetWidth = sheetWidth;
			this.sheetHeight = sheetHeight;
			
	        ByteBuffer buffer = BufferUtils.createByteBuffer(realWidth * realHeight * 4);
	        for(int y = 0; y < realHeight; y++){
	            for(int x = 0; x < realWidth; x++){
	                int pixel = pixels[y * realWidth + x];
	                buffer.put((byte) ((pixel >> 16) & 0xFF));
	                buffer.put((byte) ((pixel >> 8) & 0xFF));
	                buffer.put((byte) ((pixel >> 0)& 0xFF));
	                buffer.put((byte) ((pixel >> 24) & 0xFF));
	            }
	        }
	        buffer.flip();
			
			
			for (int cU = 0; cU < copiesU(); cU++) {
				for (int cV = 0; cV < copiesV(); cV++) {
					int offX = originX + this.realWidth * cU;
					int offY = originY + this.realHeight * cV;
					
					if (offX + realWidth > this.sheetWidth) {
						return;
					}
					if (offY + realHeight > this.sheetHeight) {
						return;
					}
					
					GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, offX, offY, realWidth, realHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
				}
			}
			
			pixels = null;
		}
		public int copiesU() {
			return maxU - minU;
		}
		public int copiesV() {
			return maxV - minV;
		}
		public int getAbsoluteWidth() {
			return realWidth * copiesU();
		}
		public int getAbsoluteHeight() {
			return realHeight * copiesV();
		}
		
		public float convertU(float relativeU) {
			if (isFlatMaterial) {
				relativeU = 0.5f;
			}
			return originX / (float)sheetWidth + (relativeU - minU) * ((float)this.realWidth / sheetWidth);
		}
		
		public float convertV(float relativeV) {
			if (isFlatMaterial) {
				relativeV = 0.5f;
			}
			return originY / (float)sheetHeight + (relativeV - minV) * ((float)this.realHeight / sheetHeight);
		}
		
		public Integer size() {
			return getAbsoluteHeight();
		}
	}
	
	public OBJTextureSheet(OBJModel model) {
		this(model, null);
	}
	
	public OBJTextureSheet(OBJModel model, String texPrefix) {
		this.model = model;
		

		model.offsetU =  new byte[model.faceVerts.length / 9];
		model.offsetV =  new byte[model.faceVerts.length / 9];
		
		Function<Integer, Integer> scaleFn = null;
		if (ConfigGraphics.textureScale != 1) {
			scaleFn = (Integer val) -> {
				if (val == 1) {
					return 1;
				}
				return (int)Math.ceil(val/ConfigGraphics.textureScale);
			};
		}
		
		mappings = new HashMap<String, SubTexture>();
		Set<String> missing = new HashSet<String>();
		for (String groupName : model.groups.keySet()) {
			int[] quads = model.groups.get(groupName);
			for (int face : quads) {
				String mtlName = model.faceMTLs[face];
				if (missing.contains(mtlName)) {
					// Already warned about it
					continue;
				}
				
				if (!model.materials.containsKey(mtlName)) {
					ImmersiveRailroading.warn("Missing material %s", mtlName);
					missing.add(mtlName);
					continue;
				}
				
				if (model.materials.get(mtlName).texKd != null) {
					String key = model.materials.get(mtlName).texKd.toString();
					if (!mappings.containsKey(key)) {
						try {
							ResourceLocation kd = model.materials.get(mtlName).texKd;
							if (texPrefix != null) {
								String[] sp = kd.toString().split("/");
								String fname = sp[sp.length-1];
								kd = new ResourceLocation(kd.toString().replaceAll(fname, texPrefix + "/" + fname));
							}
							mappings.put(key, new SubTexture(kd, model.materials.get(mtlName).texKd, scaleFn));
						} catch (IOException e) {
							e.printStackTrace();
							missing.add(mtlName);
							continue;
						}
					}
					List<Vec2f> vts = new ArrayList<Vec2f>();
					for (int[] point : model.points(face)) {
						Vec2f vt = point[1] != -1 ? model.vertexTextures(point[1]) : null;
						if (vt != null) {
							vts.add(vt);
						}
					}
					if (vts.size() != 0) {
						Vec2f offset = mappings.get(key).extendSpace(vts);
						model.offsetU[face] = (byte) offset.x;
						model.offsetV[face] = (byte) offset.y;
					}
				} else if (model.materials.get(mtlName).Kd != null) {
					if (!mappings.containsKey(mtlName)) {
						Material currentMTL = model.materials.get(mtlName);
						int r = (int) (Math.max(0, currentMTL .Kd.get(0) - model.darken) * 255);
						int g = (int) (Math.max(0, currentMTL.Kd.get(1) - model.darken) * 255);
						int b = (int) (Math.max(0, currentMTL.Kd.get(2) - model.darken) * 255);
						int a = (int) (currentMTL.Kd.get(3) * 255);
						mappings.put(mtlName, new SubTexture(mtlName, r,g,b,a));
					}
				}
			}
		}
		int maxSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
		if (ConfigGraphics.overrideGPUTexSize != -1) {
			maxSize = ConfigGraphics.overrideGPUTexSize;
		}
		
		int currentX = 0;
		int currentY = 0;
		int rowHeight = 0;
		
		List<SubTexture> texs = new ArrayList<SubTexture>();
		texs.addAll(mappings.values());
		
		Collections.sort(texs, (SubTexture a, SubTexture b) -> { return b.size().compareTo(a.size()); });
		
		for (SubTexture tex : texs) {
			if (currentX + tex.getAbsoluteWidth() > maxSize) {
				currentX = 0;
				currentY += rowHeight;
				rowHeight = 0;
			}
			rowHeight = Math.max(rowHeight, tex.getAbsoluteHeight());
			currentX += tex.getAbsoluteWidth();
			this.sheetWidth = Math.max(this.sheetWidth, currentX);
			this.sheetHeight = Math.max(this.sheetHeight, currentY + rowHeight); 
		}
		
		textureID = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
		
		currentX = 0;
		currentY = 0;
		rowHeight = 0;

		TextureUtil.allocateTexture(textureID, sheetWidth, sheetHeight);
		
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		
        ImmersiveRailroading.debug("Max Tex Size: %s", maxSize);
        if (sheetWidth > maxSize || sheetHeight > maxSize)
        	ImmersiveRailroading.warn("Sheet WxH: %sx%s", sheetWidth, sheetHeight);

		for (SubTexture tex : texs) {
			ImmersiveRailroading.debug("%s copies %s x %s", tex.tex, tex.copiesU(), tex.copiesV());
			ImmersiveRailroading.debug("%s  actual %s x %s", tex.tex, tex.getAbsoluteWidth(), tex.getAbsoluteHeight());
			if (tex.getAbsoluteWidth() > maxSize) {
				ImmersiveRailroading.error("BAD TEXTURE, HACKING...");
			}
			if (currentX + tex.getAbsoluteWidth() > maxSize) {
				currentX = 0;
				currentY += rowHeight;
				rowHeight = 0;
				ImmersiveRailroading.debug("NEXT_LINE");
			}
			rowHeight = Math.max(rowHeight, tex.getAbsoluteHeight());
			tex.upload(textureID, currentX, currentY, sheetWidth, sheetHeight);
			currentX += tex.getAbsoluteWidth();
		}
	}
	
	public float convertU(String mtlName, float u) {
		if (model.materials.containsKey(mtlName)) {
			ResourceLocation kd = model.materials.get(mtlName).texKd;
			if (kd != null) {
				mtlName = kd.toString();
			}
		}
		if (mappings.containsKey(mtlName)) {
			return mappings.get(mtlName).convertU(u);
		}
		return 0;
	}
	public float convertV(String mtlName, float v) {
		if (model.materials.containsKey(mtlName)) {
			ResourceLocation kd = model.materials.get(mtlName).texKd;
			if (kd != null) {
				mtlName = kd.toString();
			}
		}
		if (mappings.containsKey(mtlName)) {
			return mappings.get(mtlName).convertV(v);
		}
		return 0;
	}
	
	public void freeGL() {
		GL11.glDeleteTextures(textureID);
	}

	public int samp(String mtlName) {
		if (model.materials.containsKey(mtlName)) {
			ResourceLocation kd = model.materials.get(mtlName).texKd;
			if (kd != null) {
				mtlName = kd.toString();
			}
		}
		if (mappings.containsKey(mtlName)) {
			return mappings.get(mtlName).sampPx;
		}
		return 0;
	}

	public boolean isFlatMaterial(String mtlName) {
		if (model.materials.containsKey(mtlName)) {
			ResourceLocation kd = model.materials.get(mtlName).texKd;
			if (kd != null) {
				mtlName = kd.toString();
			}
		}
		if (mappings.containsKey(mtlName)) {
			return mappings.get(mtlName).isFlatMaterial;
		}
		return false;
	}
}
