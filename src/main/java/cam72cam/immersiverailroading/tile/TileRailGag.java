package cam72cam.immersiverailroading.tile;

public class TileRailGag extends TileRailBase {
	public void setFlexible(boolean flexible) {
		this.flexible = flexible;
	}
	
	public boolean hasFastRenderer()
    {
        return false;
    }
	
	public boolean updateRerender() {
		return true;
	}
}