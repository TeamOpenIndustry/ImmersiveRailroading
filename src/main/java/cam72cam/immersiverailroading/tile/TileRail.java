package cam72cam.immersiverailroading.tile;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.opengl.GL11;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.blocks.BlockRail;
import cam72cam.immersiverailroading.library.TrackType;
import cam72cam.immersiverailroading.track.BuilderBase;
import cam72cam.immersiverailroading.track.TrackBase;

public class TileRail extends TileRailBase {
	private EnumFacing facing;
	private TrackType type;
	
	private BlockPos center;

	private double curveRadius;
	
	private double slopeHeight;
	private double slopeLength;
	private double slopeAngle;
	
	private boolean isVisible = true;
	private boolean switchActive = false;
	private BufferBuilder worldRenderer;


	@Override
	@SideOnly(Side.CLIENT)
	public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() {
		return INFINITE_EXTENT_AABB;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared()
	{
		return Double.MAX_VALUE;
	}

	public EnumFacing getFacing() {
		if (facing == EnumFacing.DOWN) {
			return EnumFacing.NORTH;
		}
		return facing;
	}
	
	public TrackType getType() {
		return this.type;
	}
	
	public boolean getSwitchState() {
		return switchActive;
	}
	public boolean isVisible() {
		return isVisible;
	}

	public Vec3i getCenter() {
		return center;
	}

	public double getRadius() {
		// TODO Auto-generated method stub
		return curveRadius;
	}
	
	public void setType(TrackType value) {
		this.type = value;
		this.markDirty();
	}
	public void setFacing(EnumFacing value) {
		this.facing = value;
		this.markDirty();
	}
	public void setVisible(Boolean value) {
		this.isVisible = value;
		this.markDirty();
	}
	public void setCenter(BlockPos center, float radius) {
		this.center = center;
		this.curveRadius = radius;
		this.markDirty();
	}
	public void setSlope(float slopeAngle, int slopeHeight, int slopeLength) {
		this.slopeAngle = slopeAngle;
		this.slopeHeight = slopeHeight;
		this.slopeLength = slopeLength;
		this.markDirty();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		facing = EnumFacing.getFront(nbt.getByte("facing"));
		type = TrackType.valueOf(nbt.getString("type"));
		
		center = getNBTBlockPos(nbt, "center");
		curveRadius = nbt.getDouble("r");
		
		slopeHeight = nbt.getDouble("slopeHeight");
		slopeLength = nbt.getDouble("slopeLength");
		slopeAngle = nbt.getDouble("slopeAngle");
		
		isVisible = nbt.getBoolean("isVisible");
		switchActive = nbt.getBoolean("switchActive");
		
		super.readFromNBT(nbt);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setByte("facing", (byte) facing.getIndex());
		nbt.setString("type", type.name());
		
		setNBTBlockPos(nbt, "center", center);
		nbt.setDouble("r", curveRadius);
		
		nbt.setDouble("slopeHeight", slopeHeight);
		nbt.setDouble("slopeLength", slopeLength);
		nbt.setDouble("slopeAngle", slopeAngle);
		
		nbt.setBoolean("isVisible", isVisible);
		nbt.setBoolean("switchActive", switchActive);
		
		return super.writeToNBT(nbt);
	}

	public IBlockState getBlockState() {
		// Functions without a block position
		return ImmersiveRailroading.BLOCK_RAIL.getDefaultState().withProperty(BlockRail.FACING, facing).withProperty(BlockRail.TRACK_TYPE, type);
	}
	
	private class ScaledModel implements IBakedModel {
		// I know this is evil and I love it :D
		
		private IBakedModel source;
		private float height;
		
		public ScaledModel(IBakedModel source, float height) {
			this.source = source;
			this.height = height + 0.02f;
		}

		@Override
		public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
			List<BakedQuad> quads = source.getQuads(state, side, rand);
			List<BakedQuad> newQuads = new ArrayList<BakedQuad>();
			for (BakedQuad quad : quads) {
				int[] newData = Arrays.copyOf(quad.getVertexData(), quad.getVertexData().length);

	            VertexFormat format = quad.getFormat();
				
				for (int i = 0; i < 4; ++i)
		        {
					int j = format.getIntegerSize() * i;
		            newData[j + 1] = Float.floatToRawIntBits(Float.intBitsToFloat(newData[j + 1]) * height);
		        }
				
				newQuads.add(new BakedQuad(newData, quad.getTintIndex(), quad.getFace(), quad.getSprite(), quad.shouldApplyDiffuseLighting(), quad.getFormat()));
			}
			
			return newQuads;
		}

		@Override
		public boolean isAmbientOcclusion() { return source.isAmbientOcclusion(); }
		@Override
		public boolean isGui3d() { return source.isGui3d(); }
		@Override
		public boolean isBuiltInRenderer() { return source.isBuiltInRenderer(); }
		@Override
		public TextureAtlasSprite getParticleTexture() { return source.getParticleTexture(); }
		@Override
		public ItemOverrideList getOverrides() { return source.getOverrides(); }
		
	}

	/*
	 * This returns a cached buffer as rails don't change their model often
	 * This drastically reduces the overhead of rendering these complex models
	 * 
	 * We also draw the railbed here since drawing a model for each gag eats FPS 
	 */
	protected BufferBuilder getModelBuffer() {
		if (worldRenderer != null) {
			return worldRenderer;
		}
		
		// Get model for current state
		final BlockRendererDispatcher blockRenderer = Minecraft.getMinecraft().getBlockRendererDispatcher();
		BlockPos blockPos = this.getPos();
		IBlockState state = getWorld().getBlockState(blockPos);
		state = this.getBlockState();
		IBakedModel model = blockRenderer.getBlockModelShapes().getModelForState(state);
		
		IBlockState gravelState = Blocks.GRAVEL.getDefaultState();
		IBakedModel gravelModel = blockRenderer.getBlockModelShapes().getModelForState(gravelState);
		
		// Create render targets
		worldRenderer = new BufferBuilder(2097152);

		// Reverse position which will be done render model
		worldRenderer.setTranslation(-blockPos.getX(), -blockPos.getY(), -blockPos.getZ());

		// Start drawing
		worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

		// From IE
		worldRenderer.color(255, 255, 255, 255);

		// Render block at position
		blockRenderer.getBlockModelRenderer().renderModel(getWorld(), model, state, blockPos, worldRenderer, false);
		
		// This is evil but really fast :D
		BuilderBase builder = type.getBuilder(world, new BlockPos(0,0,0), facing.getOpposite());
		for (TrackBase base : builder.getTracks()) {
			blockRenderer.getBlockModelRenderer().renderModel(getWorld(), new ScaledModel(gravelModel, base.getHeight()), gravelState, blockPos.add(base.getPos()), worldRenderer, false);
		}
		
		worldRenderer.finishDrawing();
		
		return worldRenderer;
	}

	/*
	@Override
	public void update() {
		if (world.isRemote) {
			return;
		}

		if (this.areEntitiesOnSwitch()) {
			// Force switch certain direction
			setSwitchState(true);
		} else {
			// Default to redstone
			setSwitchState(world.isBlockIndirectlyGettingPowered(this.pos) > 0);
		}
	}*/
	
	/*
	 * 
    
    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
    	TileRail tr = (TileRail) world.getTileEntity(pos);
    	if (tr != null) {
    		System.out.println(String.format("GOT FACING %s %s", tr.getFacing(), ((World)world).isRemote));
    		return state.withProperty(BlockRail.FACING, tr.getFacing()).withProperty(BlockRail.TRACK_TYPE, tr.getType());
    	}
    	System.out.println("TILE NOT FOUND");
    	return this.getDefaultState();
    }

	public void setSwitchState(boolean state) {
		if (!this.isSwitch()) {
			return;
		}

		if (switchActive == state) {
			return;
		}

		this.switchActive = state;

		this.markDirty();
		this.markBlockForUpdate();
		
		int i = this.pos.getX();
		int j = this.pos.getY();
		int k = this.pos.getZ();

		TileEntity te1 = null;
		TileEntity te2 = null;

		switch (facingMeta) {
		case 0:
			te1 = world.getTileEntity(new BlockPos(i, j, k + 1));
			te2 = world.getTileEntity(new BlockPos(i, j, k + 2));
			break;
		case 1:
			te1 = world.getTileEntity(new BlockPos(i - 1, j, k));
			te2 = world.getTileEntity(new BlockPos(i - 2, j, k));
			break;
		case 2:
			te1 = world.getTileEntity(new BlockPos(i, j, k - 1));
			te2 = world.getTileEntity(new BlockPos(i, j, k - 2));
			break;
		case 3:
			te1 = world.getTileEntity(new BlockPos(i + 1, j, k));
			te2 = world.getTileEntity(new BlockPos(i + 2, j, k));
		}

		if (!this.switchActive) {
			((TileRail) te1).setType(TrackType.STRAIGHT_SMALL);
			if (type == TrackType.MEDIUM_RIGHT_PARALLEL_SWITCH || type == TrackType.MEDIUM_LEFT_PARALLEL_SWITCH
					|| type == TrackType.LARGE_RIGHT_SWITCH || type == TrackType.LARGE_LEFT_SWITCH) {
				((TileRail) te2).setType(TrackType.STRAIGHT_SMALL);
			}
		} else {
			switch (type) {
			case MEDIUM_LEFT_PARALLEL_SWITCH:
				((TileRail) te2).setType(TrackType.MEDIUM_LEFT_TURN);
			case MEDIUM_LEFT_SWITCH:
				((TileRail) te1).setType(TrackType.MEDIUM_LEFT_TURN);
				break;

			case MEDIUM_RIGHT_PARALLEL_SWITCH:
				((TileRail) te2).setType(TrackType.MEDIUM_RIGHT_TURN);
			case MEDIUM_RIGHT_SWITCH:
				((TileRail) te1).setType(TrackType.MEDIUM_RIGHT_TURN);
				break;

			case LARGE_RIGHT_SWITCH:
				((TileRail) te2).setType(TrackType.TURN_LARGE_RIGHT);
				((TileRail) te1).setType(TrackType.TURN_LARGE_RIGHT);
				break;
			case LARGE_LEFT_SWITCH:
				((TileRail) te2).setType(TrackType.TURN_LARGE_LEFT);
				((TileRail) te1).setType(TrackType.TURN_LARGE_LEFT);
				break;
			default:
				break;
			}
		}
	}

	public boolean isTurnTrack() {
		return type.getType() == TrackItems.TURN || type.getType() == TrackItems.SWITCH && getSwitchState();
	}

	public boolean isStraightTrack() {
		return type.getType() == TrackItems.STRAIGHT || type.getType() == TrackItems.CROSSING || type.getType() == TrackItems.SWITCH
				&& !getSwitchState();
	}

	public boolean isTwoWaysCrossingTrack() {
		return type == TrackType.TWO_WAYS_CROSSING;
	}

	public boolean isSwitch() {
		return type.getType() == TrackItems.SWITCH;
	}

	public boolean isRightSwitch() {
		return TrackType.MEDIUM_RIGHT_SWITCH == type || TrackType.LARGE_RIGHT_SWITCH == type || TrackType.MEDIUM_RIGHT_PARALLEL_SWITCH == type;
	}

	public boolean isLeftSwitch() {
		return TrackType.MEDIUM_LEFT_SWITCH == type || TrackType.LARGE_LEFT_SWITCH == type || TrackType.MEDIUM_LEFT_PARALLEL_SWITCH == type;
	}

	public boolean isSlopeTrack() {
		return type.getType() == TrackItems.SLOPE;
	}

	public boolean areEntitiesOnSwitch() {
		float[] offsets = new float[] { 0, 0, 0, 0, 0, 0 };
		switch (facingMeta) {
		case 0:
			if (isLeftSwitch()) {
				offsets = new float[] { -2.0F, 0, 2.0F, f, 1.0F - f, 2.0F - f };
			} else {
				offsets = new float[] { +1.0F, 0, 1.0F, 2.0F - f, 1.0F - f, 2.0F - f };
			}
			break;

		case 1:
			if (isLeftSwitch()) {
				offsets = new float[] { -1.0F, 0, 1.0F, f, 1.0F - f, f };
			} else {

				offsets = new float[] { -1.0F, 0, 1.0F, f, 1.0F - f, 2.0F - f };
			}
			break;

		case 2:
			if (isLeftSwitch()) {
				offsets = new float[] { +1.0F, 0, 1.0F, 2.0F - f, 1.0F - f, f };
			} else {

				offsets = new float[] { -1.0F, 0, 1.0F, f, 1.0F - f, f };
			}
			break;

		case 3:
			if (isLeftSwitch()) {
				offsets = new float[] { +1.0F, 0, 1.0F, 3.0F - f, 1.0F - f, 2.0F - f };
			} else {
				offsets = new float[] { +1.0F, 0, 1.0F, 3.0F - f, 1.0F + f, f };
			}
			break;
		}
		return !world.getEntitiesWithinAABB(
				EntityMinecart.class,
				new AxisAlignedBB(this.pos.getX() + offsets[0], this.pos.getY() + offsets[1], this.pos.getZ() + offsets[2], this.pos.getX() + offsets[3],
						this.pos.getY() + offsets[4], this.pos.getZ() + offsets[5])).isEmpty();
	}*/
}
