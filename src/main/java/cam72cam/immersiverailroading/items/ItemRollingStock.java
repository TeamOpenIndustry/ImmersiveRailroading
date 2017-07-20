package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.registry.DefinitionManager;
import cam72cam.immersiverailroading.entity.registry.IDefinitionRollingStock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemRollingStock extends Item {
	public static final String NAME = "item_rolling_stock";
	
	public ItemRollingStock() {
		super();
		setUnlocalizedName(ImmersiveRailroading.MODID + ":" + NAME);
        setRegistryName(new ResourceLocation(ImmersiveRailroading.MODID, NAME));
	}
	
	public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (!worldIn.isRemote) {
			IDefinitionRollingStock def = DefinitionManager.getDefinition("rolling_stock/locomotives/shay.json");
			def.spawn(worldIn, pos.add(0, 0.6, 0), EnumFacing.fromAngle(player.rotationYawHead));
			System.out.println("SPAWNED SHAY");
		}
		return super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
	}
}
