package cam72cam.immersiverailroading.blocks;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.fluids.IRFluids;
import cam72cam.immersiverailroading.items.ItemTabs;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLiquid;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.BlockFluidClassic;

public class BlockSteamFluid extends BlockFluidClassic{

	public final String NAME = "block_steam";
	public static final Material materialFluidSteam = new MaterialLiquid(MapColor.SILVER);
	
	public BlockSteamFluid() {
		super(IRFluids.FLUID_STEAM, materialFluidSteam);
		
		setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
        setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
        setCreativeTab(ItemTabs.MAIN_TAB);

	}

}
