package cam72cam.immersiverailroading.render;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.items.nbt.ItemDefinition;
import cam72cam.immersiverailroading.items.nbt.ItemGauge;
import cam72cam.immersiverailroading.util.GLBoolTracker;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import util.Matrix4;

public class StockItemModel implements IBakedModel {
	private OBJRender model;
	private double scale;
	private ItemStack stack;
	private static boolean done = false;

	public StockItemModel() {
	}
	
	public StockItemModel(ItemStack stack) {
		scale = ItemGauge.get(stack).scale();
		this.stack = stack;
		String defID = ItemDefinition.getID(stack);
		model = StockRenderCache.getRender(defID);
		if (model == null) {
			stack.setCount(0);
		}
	}
	
	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		/*
		 * I am an evil wizard!
		 * 
		 * So it turns out that I can stick a draw call in here to
		 * render my own stuff. This subverts forge's entire baked model
		 * system with a single line of code and injects my own OpenGL
		 * payload. Fuck you modeling restrictions.
		 * 
		 * This is probably really fragile if someone calls getQuads
		 * before actually setting up the correct GL context.
		 */
		if (model != null) {
			GLBoolTracker tex = new GLBoolTracker(GL11.GL_TEXTURE_2D, model.hasTexture());
			GLBoolTracker cull = new GLBoolTracker(GL11.GL_CULL_FACE, false);
			
			GL11.glPushMatrix();
			double scale = 0.2 * Math.sqrt(this.scale);
			GL11.glScaled(scale, scale, scale);
			model.bindTexture();
			model.draw();
			model.restoreTexture();
			GL11.glPopMatrix();
			
			
			GL11.glPushMatrix();
			
			
			GL11.glLoadIdentity();
			
			Minecraft mc = Minecraft.getMinecraft();

            //GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);
            //GlStateManager.matrixMode(5889);
            //GlStateManager.loadIdentity();
            //GlStateManager.matrixMode(5888);
            //GlStateManager.loadIdentity();
            ScaledResolution scaledresolution = new ScaledResolution(mc);
            //GlStateManager.clear(256);
            //GlStateManager.matrixMode(5889);
            //GlStateManager.loadIdentity();
            //GlStateManager.ortho(0.0D, scaledresolution.getScaledWidth_double(), scaledresolution.getScaledHeight_double(), 0.0D, 1000.0D, 3000.0D);
            //GlStateManager.matrixMode(5888);
            //GlStateManager.loadIdentity();
            
            GlStateManager.translate(0, 0, -2000.0F);
            
            GL11.glTranslated(0, scaledresolution.getScaledHeight_double(), 0);
            
            int size = 32;
            
            
            
            double foo = 1.0/scaledresolution.getScaleFactor();
            GL11.glScaled(foo, foo, foo); 
            
            
            //double bar = 0.25; 
            //GL11.glScaled(bar, bar, bar);
            
            GlStateManager.translate(size, -size, 0);

            //GL11.glRotated(180, 0, 1, 0);
			
			GL11.glColor4f(1, 1, 1, 1);
			GL11.glBegin(GL11.GL_QUADS);
			GL11.glVertex3d(0, 0, 0);
			GL11.glVertex3d(0, size, 0);
			GL11.glVertex3d(-size, size, 0);
			GL11.glVertex3d(-size, 0, 0);
			GL11.glEnd();
			/*
			
			GL11.glTranslated(0, -0.5, 0);
			scale = 1;//1/model.model.widthOfGroups(model.model.groups()) * 0.4;
			GL11.glScaled(scale, scale, scale);*/
			scale = 5;
			GL11.glScaled(scale, scale, scale);
			model.draw();
			
			
		
			if (!done) {
				done = true;
				int width = size;
				int height = size;
				int bpp = 4;
				
				ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * bpp);
				GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer );
				File file = new File("/home/gilligan/foo.png"); // The file to save to.
				String format = "PNG"; // Example: "PNG" or "JPG"
				BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
				   
				for(int x = 0; x < width; x++) 
				{
				    for(int y = 0; y < height; y++)
				    {
				        int i = (x + (width * y)) * bpp;
				        int r = buffer.get(i) & 0xFF;
				        int g = buffer.get(i + 1) & 0xFF;
				        int b = buffer.get(i + 2) & 0xFF;
				        image.setRGB(x, height - (y + 1), (0xFF << 24) | (r << 16) | (g << 8) | b);
				    }
				}
				   
				try {
				    ImageIO.write(image, format, file);
				} catch (IOException e) { e.printStackTrace(); }
			}

			GL11.glPopMatrix();
			
			tex.restore();
			cull.restore();
		}
		return new ArrayList<BakedQuad>();
	}

	@Override
	public boolean isAmbientOcclusion() {
		return true;
	}

	@Override
	public boolean isGui3d() {
		return true;
	}

	@Override
	public boolean isBuiltInRenderer() {
		return false;
	}

	@Override
	public TextureAtlasSprite getParticleTexture() {
		return null;
	}

	public class ItemOverrideListHack extends ItemOverrideList {
		public ItemOverrideListHack() {
			super(new ArrayList<ItemOverride>());
		}

		public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity) {
			return new StockItemModel(stack);
		}
	}

	@Override
	public ItemOverrideList getOverrides() {
		return new ItemOverrideListHack();
	}

	@Override
	public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType cameraTransformType) {
		Pair<? extends IBakedModel, Matrix4f> defaultVal = ForgeHooksClient.handlePerspective(this, cameraTransformType);
		switch (cameraTransformType) {
		case THIRD_PERSON_LEFT_HAND:
		case THIRD_PERSON_RIGHT_HAND:
			return Pair.of(defaultVal.getLeft(),
					new Matrix4().rotate(Math.toRadians(60), 1, 0, 0).rotate(Math.toRadians(-90), 0, 1, 0).toMatrix4f());
		case FIRST_PERSON_LEFT_HAND:
		case FIRST_PERSON_RIGHT_HAND:
			return Pair.of(defaultVal.getLeft(),
					new Matrix4().rotate(Math.toRadians(10), 1, 0, 0).rotate(Math.toRadians(-90), 0, 1, 0).toMatrix4f());
		case GROUND:
			return Pair.of(defaultVal.getLeft(), new Matrix4().rotate(Math.toRadians(-90), 0, 1, 0).toMatrix4f());
		case FIXED:
			// Item Frame
			return Pair.of(defaultVal.getLeft(), new Matrix4().rotate(Math.toRadians(-90), 0, 1, 0).toMatrix4f());
		case GUI:
			return Pair.of(defaultVal.getLeft(), new Matrix4().translate(0.5, 0, 0).rotate(Math.toRadians(+5+90), 0, 1, 0).toMatrix4f());
		case HEAD:
			return Pair.of(defaultVal.getLeft(),
					new Matrix4().translate(0, 0, 0.5).rotate(Math.toRadians(-90), 0, 1, 0).toMatrix4f());
		case NONE:
			return defaultVal;
		}
		return defaultVal;
	}
}
