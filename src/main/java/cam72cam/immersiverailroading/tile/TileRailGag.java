package cam72cam.immersiverailroading.tile;

public class TileRailGag extends TileRailBase {
	public void setFlexible(boolean flexible) {
		this.flexible = flexible;
	}
	
	@Override
	public boolean hasFastRenderer()
    {
        return false;
    }
	
	@Override
	public boolean updateRerender() {
		return true;
	}
}