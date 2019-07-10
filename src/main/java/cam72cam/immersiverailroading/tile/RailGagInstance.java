package cam72cam.immersiverailroading.tile;

import cam72cam.mod.block.BlockEntity;

public class RailGagInstance extends RailBaseInstance {
	public RailGagInstance(BlockEntity.Internal internal) {
		super(internal);
	}

	public void setFlexible(boolean flexible) {
		this.flexible = flexible;
	}

	/* TODO RENDER
	@Override
	public boolean hasFastRenderer()
    {
        return false;
    }
	
	@Override
	public boolean updateRerender() {
		return true;
	}
	*/
}