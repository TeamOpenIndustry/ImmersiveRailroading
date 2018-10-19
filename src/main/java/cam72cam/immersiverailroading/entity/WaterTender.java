package cam72cam.immersiverailroading.entity;

import java.util.List;

import cam72cam.immersiverailroading.inventory.SlotFilter;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.registry.TenderDefinition;
import cam72cam.immersiverailroading.util.LiquidUtil;
import net.minecraft.world.World;
import net.minecraftforge.fluids.*;

public class WaterTender extends CarTank {
	
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
	public List<Fluid> getFluidFilter() {
		return LiquidUtil.getWater();
	}

