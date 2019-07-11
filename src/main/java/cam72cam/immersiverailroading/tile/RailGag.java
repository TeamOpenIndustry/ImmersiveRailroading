package cam72cam.immersiverailroading.tile;

import cam72cam.mod.block.tile.TileEntity;

public class RailGag extends RailBase {
	public RailGag(TileEntity internal) {
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