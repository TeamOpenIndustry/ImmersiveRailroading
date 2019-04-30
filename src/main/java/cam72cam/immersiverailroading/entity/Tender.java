package cam72cam.immersiverailroading.entity;

import java.util.List;

import cam72cam.immersiverailroading.inventory.SlotFilter;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.registry.TenderDefinition;
import cam72cam.immersiverailroading.util.LiquidUtil;
import cam72cam.mod.entity.ModdedEntity;
import net.minecraftforge.fluids.*;

public class Tender extends CarTank {
	
	public Tender(ModdedEntity entity) {
		super(entity);
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
		return LiquidUtil.getWater();
	}

	@Override
	public int getInventorySize() {
		return this.getDefinition().getInventorySize(gauge) + 2;
	}
	
	public int getInventoryWidth() {
		return this.getDefinition().getInventoryWidth(gauge);
	}
	
	@Override
	protected void initContainerFilter() {
		cargoItems.filter.clear();
		cargoItems.filter.put(getInventorySize()-2, SlotFilter.FLUID_CONTAINER);
		cargoItems.filter.put(getInventorySize()-1, SlotFilter.FLUID_CONTAINER);
		cargoItems.defaultFilter = SlotFilter.BURNABLE;
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