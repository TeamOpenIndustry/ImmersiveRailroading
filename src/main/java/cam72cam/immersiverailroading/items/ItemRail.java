package cam72cam.immersiverailroading.items;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.blocks.BlockRailBase;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.library.TrackItems;
import cam72cam.immersiverailroading.library.TrackPositionType;
import cam72cam.immersiverailroading.tile.TileRailPreview;
import cam72cam.immersiverailroading.track.BuilderBase;
import cam72cam.immersiverailroading.util.RailInfo;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

public class ItemRail extends ItemBlock {

	public ItemRail(Block block) {
		super(block);
		this.setMaxDamage(0);
        this.setMaxStackSize(1);
	}
	
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
		if (worldIn.isRemote) {
            playerIn.openGui(ImmersiveRailroading.instance, GuiTypes.RAIL.ordinal(), worldIn, (int) playerIn.posX, (int) playerIn.posY, (int) playerIn.posZ);
        }
        return super.onItemRightClick(worldIn, playerIn, handIn);
	}

	@Override
	public String getUnlocalizedName(ItemStack stack) {
		return super.getUnlocalizedName(stack);
	}
	
	@Override
	public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState)
    {
		if (ItemRail.isPreview(stack)) {
			world.setBlockState(pos, ImmersiveRailroading.BLOCK_RAIL_PREVIEW.getDefaultState());
			TileRailPreview te = (TileRailPreview) world.getTileEntity(pos);
			te.init(stack, player.getRotationYawHead(), hitX, hitY, hitZ);
			return true;
		}
		if (player.getEntityWorld().getBlockState(pos.down()).getBlock() instanceof BlockRailBase) {
			pos = pos.down();
		}
		
		RailInfo info = new RailInfo(stack, player.world, player.getRotationYawHead(), pos, hitX, hitY, hitZ); 
		
		BuilderBase builder = info.getBuilder(pos);
		
		if (builder.canBuild()) {
			if (!world.isRemote) {
				if (player.isCreative()) {
					builder.build();
					return false;
				}
				
				// Survival check
				
				int ties = 0;
				int rails = 0;
				
				for (ItemStack playerStack : player.inventory.mainInventory) {
					if (OreDictionaryContainsMatch(false, OreDictionary.getOres("stickSteel"), playerStack)) {
						rails += playerStack.getCount();
					}
					if (OreDictionaryContainsMatch(false, OreDictionary.getOres("plankTreatedWood"), playerStack)) {
						ties += playerStack.getCount();
					}
				}
				
				if (ties < builder.costTies()) {
					player.sendMessage(new TextComponentString("Missing " + (builder.costTies() - ties) + " ties"));
					return false;
				}
				
				if (rails < builder.costRails()) {
					player.sendMessage(new TextComponentString("Missing " + (builder.costRails() - rails) + " ties"));
					return false;
				}

				ties = builder.costTies();
				rails = builder.costRails();
				List<ItemStack> drops = new ArrayList<ItemStack>();
				
				for (ItemStack playerStack : player.inventory.mainInventory) {
					if (OreDictionaryContainsMatch(false, OreDictionary.getOres("stickSteel"), playerStack)) {
						if (rails > playerStack.getCount()) {
							rails -= playerStack.getCount();
							ItemStack copy = playerStack.copy();
							copy.setCount(playerStack.getCount());
							drops.add(copy); 
							playerStack.setCount(0);
						} else {
							ItemStack copy = playerStack.copy();
							copy.setCount(rails);
							drops.add(copy); 
							playerStack.setCount(playerStack.getCount() - rails);
						}
					}
					if (OreDictionaryContainsMatch(false, OreDictionary.getOres("plankTreatedWood"), playerStack)) {
						if (ties > playerStack.getCount()) {
							ties -= playerStack.getCount();
							ItemStack copy = playerStack.copy();
							copy.setCount(playerStack.getCount());
							drops.add(copy);  
							playerStack.setCount(0);
						} else {
							ItemStack copy = playerStack.copy();
							copy.setCount(ties);
							drops.add(copy); 
							playerStack.setCount(playerStack.getCount() - ties);
						}
					}
				}
				builder.setDrops(drops);
				builder.build();
			}
		}
		// don't decrement stack regardless
		return false;
    }
	
	private boolean OreDictionaryContainsMatch(boolean strict, NonNullList<ItemStack> ores, ItemStack playerStack) {
        for (ItemStack target : ores)
        {
            if (OreDictionary.itemMatches(target, playerStack, strict))
            {
                return true;
            }
        }
        return false;
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add("Type:     " + getType(stack));
        tooltip.add("Length:   " + getLength(stack));
        tooltip.add("Quarters: " + getQuarters(stack));
        tooltip.add("Position: " + getPosType(stack));
        tooltip.add("Rail Bed: " + getBed(stack).getDisplayName());
        tooltip.add("Preview:  " + isPreview(stack));
	}

	public static void setType(ItemStack stack, TrackItems type) {
		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setInteger("type", type.ordinal());
	}
	
	public static TrackItems getType(ItemStack stack) {
		if (stack.getTagCompound() != null){
			return TrackItems.values()[stack.getTagCompound().getInteger("type")];
		}
		return TrackItems.STRAIGHT;
	}
	
	public static void setLength(ItemStack stack, int length) {
		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setInteger("length", length);
	}
	
	public static int getLength(ItemStack stack) {
		if (stack.getTagCompound() != null){
			return stack.getTagCompound().getInteger("length");
		}
		return 10;
	}
	
	public static void setQuarters(ItemStack stack, int quarters) {
		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setInteger("quarters", quarters);
	}
	
	public static int getQuarters(ItemStack stack) {
		if (stack.getTagCompound() != null){
			return stack.getTagCompound().getInteger("quarters");
		}
		return 3;
	}
	
	public static void setPosType(ItemStack stack, TrackPositionType posType) {
		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setInteger("pos_type", posType.ordinal());
	}
	
	public static TrackPositionType getPosType(ItemStack stack) {
		if (stack.getTagCompound() != null){
			return TrackPositionType.values()[stack.getTagCompound().getInteger("pos_type")];
		}
		return TrackPositionType.FIXED;
	}

	public static ItemStack getBed(ItemStack stack) {
		if (stack.getTagCompound() != null && stack.getTagCompound().hasKey("bedItem")) { 
			return new ItemStack(stack.getTagCompound().getCompoundTag("bedItem"));
		} else {
			return new ItemStack(Items.AIR);
		}
	}
	
	public static void setBed(ItemStack stack, ItemStack base) {
		stack.getTagCompound().setTag("bedItem", base.serializeNBT());
	}

	public static boolean getBedFill(ItemStack stack) {
		if (stack.getTagCompound() != null && stack.getTagCompound().hasKey("bedFill")) { 
			return stack.getTagCompound().getBoolean("bedFill");
		} else {
			return false;
		}
	}
	public static void setBedFill(ItemStack stack, boolean value) {
		stack.getTagCompound().setBoolean("bedFill", value);
	}

	public static boolean isPreview(ItemStack stack) {
		if (stack.getTagCompound() != null && stack.getTagCompound().hasKey("isPreview")) { 
			return stack.getTagCompound().getBoolean("isPreview");
		} else {
			return false;
		}
	}
	public static void setPreview(ItemStack stack, boolean value) {
		stack.getTagCompound().setBoolean("isPreview", value);
	}
}
