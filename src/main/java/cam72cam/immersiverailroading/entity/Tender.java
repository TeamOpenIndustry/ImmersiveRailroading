package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;
import java.util.List;

import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.registry.TenderDefinition;
import net.minecraft.world.World;
import net.minecraftforge.fluids.*;

public class Tender extends CarTank {
	
	public Tender(World world) {
		this(world, null);
	}

	public Tender(World world, String defID) {
		super(world, defID);
	}
	
	@Override
	public TenderDefinition getDefinition() {
		return super.getDefinition(TenderDefinition.class);
	}
	
	@Override
	public GuiTypes guiType() {
		return GuiTypes.TENDER;
	}

	@Override
	public List<Fluid> getFluidFilter() {
		List<Fluid> filter = new ArrayList<Fluid>();
		filter.add(FluidRegistry.WATER);
		return filter;
	}

	@Override
	public int getInventorySize() {
		return this.getDefinition().getInventorySize(gauge) + 2;
	}
	
	public int getInventoryWidth() {
		return this.getDefinition().getInventoryWidth(gauge);
	}
	
	@Override
	protected int[] getContainerInputSlots() {
		return new int[] { getInventorySize()-2 };
	}
	@Override
	protected int[] getContainertOutputSlots() {
		return new int[] { getInventorySize()-1 };
	}
}