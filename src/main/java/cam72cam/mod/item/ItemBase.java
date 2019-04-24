package cam72cam.mod.item;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.World;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ItemBase extends Item {
    private final CreativeTab[] creativeTabs;

    public ItemBase(String modID, String name, int stackSize, CreativeTab ... tabs) {
        setUnlocalizedName(modID + ":" + name);
        setRegistryName(new ResourceLocation(modID, name));
        setMaxStackSize(stackSize);
        setCreativeTab(tabs[0].internal);
        this.creativeTabs = tabs;
    }

    /* Overrides */

    @Override
    public final void getSubItems(CreativeTabs tab, NonNullList<net.minecraft.item.ItemStack> items) {
        CreativeTab myTab = tab != CreativeTabs.SEARCH ? new CreativeTab(tab) : null;
        items.addAll(getItemVariants(myTab).stream().map((ItemStack stack) -> stack.internal).collect(Collectors.toList()));
    }
    public List<ItemStack> getItemVariants(CreativeTab creativeTab) {
        List<ItemStack> res = new ArrayList<>();
        if (creativeTab == null || creativeTab.internal == this.getCreativeTab()) {
            res.add(new ItemStack(this, 1));
        }
        return res;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public final void addInformation(net.minecraft.item.ItemStack stack, @Nullable net.minecraft.world.World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        applyCustomName(new ItemStack(stack));
        this.addInformation(new ItemStack(stack), tooltip);
    }
    public void addInformation(ItemStack itemStack, List<String> tooltip) {
    }

    @Override
    public final EnumActionResult onItemUse(EntityPlayer player, net.minecraft.world.World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        return this.onClickBlock(new Player(player), new World(worldIn), new Vec3i(pos), Hand.from(hand), Facing.from(facing), new Vec3d(hitX, hitY, hitZ)).internal;
    }
    public ClickResult onClickBlock(Player player, World world, Vec3i vec3i, Hand from, Facing from1, Vec3d vec3d) {
        return ClickResult.PASS;
    }

    @Override
    public final ActionResult<net.minecraft.item.ItemStack> onItemRightClick(net.minecraft.world.World world, EntityPlayer player, EnumHand hand) {
        onClickAir(new Player(player), new World(world), Hand.from(hand));
        return super.onItemRightClick(world, player, hand);
    }
    public void onClickAir(Player player, World world, Hand hand) {

    }

    @Override
    public final boolean isValidArmor(net.minecraft.item.ItemStack stack, EntityEquipmentSlot armorType, net.minecraft.entity.Entity entity) {
        return this.isValidArmor(new ItemStack(stack), ArmorSlot.from(armorType), new Entity(entity));
    }
    public boolean isValidArmor(ItemStack itemStack, ArmorSlot from, Entity entity) {
        return super.isValidArmor(itemStack.internal, from.internal, entity.internal);
    }

    /* Name Hacks */

    public String getCustomName(ItemStack stack) {
        return null;
    }

    public final void applyCustomName(ItemStack stack) {
        String custom = getCustomName(stack);
        if (custom != null) {
            stack.internal.setStackDisplayName(TextFormatting.RESET + custom);
        }
    }

    @Override
    public final String getUnlocalizedName(net.minecraft.item.ItemStack stack) {
        applyCustomName(new ItemStack(stack));
        return super.getUnlocalizedName(stack);
    }

    @Override
    public final CreativeTabs[] getCreativeTabs()
    {
        return Arrays.stream(this.creativeTabs).map((CreativeTab tab) -> tab.internal).toArray(CreativeTabs[]::new);
    }


}
