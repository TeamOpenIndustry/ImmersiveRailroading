package cam72cam.mod.render.obj;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.mod.model.obj.Material;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.model.obj.Vec2f;
import cam72cam.mod.render.GLTexture;
import cam72cam.mod.render.GPUInfo;
import cam72cam.mod.resource.Identifier;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;

public class OBJTextureSheet {
	private final GLTexture texture;
	public final GLTexture icon;
	private Map<String, SubTexture> mappings;
	private int sheetWidth = 0;
	private int sheetHeight = 0;
	private OBJModel model;

	private  class SubTexture {
		private BufferedImage image;
		int realWidth;
		private int realHeight;
		private int originX;
		private int originY;
		private int minU = 0;
		private int minV = 0;
		private int maxU = 1;
		private int maxV = 1;
		public Identifier tex;

		private boolean isFlatMaterial;

		SubTexture(Identifier tex, Identifier fallback) throws IOException {
			InputStream input;
			try {
				input = tex.getResourceStream();
			} catch (FileNotFoundException ex) {
				input = fallback.getResourceStream();
			}
			BufferedImage image = TextureUtil.readBufferedImage(input);
			input.close();
			
			realWidth = image.getWidth();
			realHeight = image.getHeight();

			this.tex = tex;
			isFlatMaterial = false;

			this.image = image;
		}
		SubTexture(String name, int r, int g, int b, int a) {
			image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
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
			this.tex = new Identifier("generated:" + name);
			isFlatMaterial = true;
		}
		
		Vec2f extendSpace(List<Vec2f> vts) {
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

		void upload(Graphics2D graphics, int originX, int originY) {
			this.originX = originX;
			this.originY = originY;

			for (int cU = 0; cU < copiesU(); cU++) {
				for (int cV = 0; cV < copiesV(); cV++) {
					int offX = originX + this.realWidth * cU;
					int offY = originY + this.realHeight * cV;
					
					if (offX + realWidth > sheetWidth) {
						return;
					}
					if (offY + realHeight > sheetHeight) {
						return;
					}
					
					graphics.drawImage(this.image, null, offX, offY);
				}
			}

			this.image = null;
		}
		int copiesU() {
			return maxU - minU;
		}
		int copiesV() {
			return maxV - minV;
		}
		int getAbsoluteWidth() {
			return realWidth * copiesU();
		}
		int getAbsoluteHeight() {
			return realHeight * copiesV();
		}
		
		float convertU(float relativeU) {
			if (isFlatMaterial) {
				relativeU = 0.5f;
			}
			return originX / (float)sheetWidth + (relativeU - minU) * ((float)this.realWidth / sheetWidth);
		}
		
		float convertV(float relativeV) {
			if (isFlatMaterial) {
				relativeV = 0.5f;
			}
			return originY / (float)sheetHeight + (relativeV - minV) * ((float)this.realHeight / sheetHeight);
		}
		
		public Integer size() {
			return getAbsoluteHeight();
		}
	}
	
	OBJTextureSheet(OBJModel model) {
		this(model, null);
	}
	
	OBJTextureSheet(OBJModel model, String texPrefix) {
		this.model = model;


		model.offsetU =  new byte[model.faceVerts.length / 9];
		model.offsetV =  new byte[model.faceVerts.length / 9];
		
		mappings = new HashMap<>();
		Set<String> missing = new HashSet<>();
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
							Identifier kd = model.materials.get(mtlName).texKd;
							if (texPrefix != null) {
								String[] sp = kd.toString().split("/");
								String fname = sp[sp.length-1];
								kd = new Identifier(kd.toString().replaceAll(fname, texPrefix + "/" + fname));
							}
							mappings.put(key, new SubTexture(kd, model.materials.get(mtlName).texKd));
						} catch (IOException e) {
							e.printStackTrace();
							missing.add(mtlName);
							continue;
						}
					}
					List<Vec2f> vts = new ArrayList<>();
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
		int currentX = 0;
		int currentY = 0;
		int rowHeight = 0;

		List<SubTexture> texs = new ArrayList<>(mappings.values());
		
		texs.sort((SubTexture a, SubTexture b) -> b.size().compareTo(a.size()));
		
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

		sheetWidth = Math.max(sheetWidth, 1);
		sheetHeight = Math.max(sheetHeight, 1);
		BufferedImage image = new BufferedImage(sheetWidth, sheetHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();


		currentX = 0;
		currentY = 0;
		rowHeight = 0;

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
			tex.upload(graphics, currentX, currentY);
			currentX += tex.getAbsoluteWidth();
		}

		String path = model.modelLoc.getPath().replace("/", ".") + texPrefix;
		this.texture = new GLTexture(path + ".png", image, 5, false);

		int iconSize = 1024;
		if (image.getWidth() * image.getHeight() > iconSize * iconSize) {
			float scale = (float)(iconSize * iconSize) / (image.getWidth() * image.getHeight());
			System.out.println(scale);
			icon = new GLTexture(path + "_icon.png", scaleImage(image, (int)(image.getWidth() * scale), (int) (image.getHeight() * scale)), 30, true);
		} else {
			icon = new GLTexture(path + "_icon.png", image, 30, true);
		}

		ImmersiveRailroading.info(GPUInfo.debug().replace("%", "%%"));
	}

	private BufferedImage scaleImage(BufferedImage image, int x, int y) {
		BufferedImage target = new BufferedImage(x, y, image.getType());
		Graphics2D g = target.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(image, 0, 0, x, y, 0, 0, image.getWidth(), image.getHeight(), null);
		return target;
	}

	float convertU(String mtlName, float u) {
		if (model.materials.containsKey(mtlName)) {
			Identifier kd = model.materials.get(mtlName).texKd;
			if (kd != null) {
				mtlName = kd.toString();
			}
		}
		if (mappings.containsKey(mtlName)) {
			return mappings.get(mtlName).convertU(u);
		}
		return 0;
	}
	float convertV(String mtlName, float v) {
		if (model.materials.containsKey(mtlName)) {
			Identifier kd = model.materials.get(mtlName).texKd;
			if (kd != null) {
				mtlName = kd.toString();
			}
		}
		if (mappings.containsKey(mtlName)) {
			return mappings.get(mtlName).convertV(v);
		}
		return 0;
	}

	boolean isFlatMaterial(String mtlName) {
		if (model.materials.containsKey(mtlName)) {
			Identifier kd = model.materials.get(mtlName).texKd;
			if (kd != null) {
				mtlName = kd.toString();
			}
		}
		if (mappings.containsKey(mtlName)) {
			return mappings.get(mtlName).isFlatMaterial;
		}
		return false;
	}

	void freeGL() {
		texture.freeGL();
		icon.freeGL();
	}
	public void dealloc() {
		texture.dealloc();
		icon.dealloc();
	}


	int bind() {
		if (!texture.isLoaded()) {
			icon.tryUpload(); //hit the queue first
		}

		if (texture.tryUpload()) {
			return texture.bind();
		}
		System.out.println("DEFER...");
		return bindIcon();
	}

	int bindIcon() {
		return icon.bind();
	}
}
