package cam72cam.immersiverailroading.entity;

import java.util.List;

import cam72cam.immersiverailroading.inventory.SlotFilter;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.registry.TenderDefinition;
import cam72cam.immersiverailroading.util.LiquidUtil;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.gui.GuiRegistry;

public class Tender extends CarTank {
	@Override
	public TenderDefinition getDefinition() {
		return super.getDefinition(TenderDefinition.class);
	}
	
	@Override
	public GuiRegistry.EntityGUI guiType() {
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
		cargoItems.filter.put(0, SlotFilter.FLUID_CONTAINER);
		cargoItems.filter.put(1, SlotFilter.FLUID_CONTAINER);
		cargoItems.defaultFilter = SlotFilter.BURNABLE;
	}

	@Override
	public boolean internalLightsEnabled() {
		return hasElectricalPower();
	}

	@Override
	protected int[] getContainerInputSlots() {
		return new int[] { 0 };
	}
	@Override
	protected int[] getContainertOutputSlots() {
		return new int[] { 1 };
	}
}