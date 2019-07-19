package cam72cam.mod.item;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.world.World;
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
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber
public class ItemBase {
    private static List<Consumer<RegistryEvent.Register<Item>>> registrations = new ArrayList<>();

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event)
    {
        registrations.forEach((consumer) -> consumer.accept(event));
    }


    private final CreativeTab[] creativeTabs;
    public final Item internal;

    public ItemBase(String modID, String name, int stackSize, CreativeTab ... tabs) {
        internal = new ItemInternal();
        internal.setUnlocalizedName(modID + ":" + name);
        internal.setRegistryName(new ResourceLocation(modID, name));
        internal.setMaxStackSize(stackSize);
        internal.setCreativeTab(tabs[0].internal);
        this.creativeTabs = tabs;

        registrations.add((event) -> event.getRegistry().register(internal));
    }

    @Optional.Interface(iface = "mezz.jei.api.ingredients.ISlowRenderItem", modid = "jei")
    private class ItemInternal extends Item {
        @Override
        public final void getSubItems(CreativeTabs tab, NonNullList<net.minecraft.item.ItemStack> items) {
            CreativeTab myTab = tab != CreativeTabs.SEARCH ? new CreativeTab(tab) : null;
            items.addAll(getItemVariants(myTab).stream().map((ItemStack stack) -> stack.internal).collect(Collectors.toList()));
        }
        @Override
        @SideOnly(Side.CLIENT)
        public final void addInformation(net.minecraft.item.ItemStack stack, @Nullable net.minecraft.world.World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
            super.addInformation(stack, worldIn, tooltip, flagIn);
            applyCustomName(new ItemStack(stack));
            ItemBase.this.addInformation(new ItemStack(stack), tooltip);
        }
        @Override
        public final EnumActionResult onItemUse(EntityPlayer player, net.minecraft.world.World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
            return ItemBase.this.onClickBlock(new Player(player), World.get(worldIn), new Vec3i(pos), Hand.from(hand), Facing.from(facing), new Vec3d(hitX, hitY, hitZ)).internal;
        }
        @Override
        public final ActionResult<net.minecraft.item.ItemStack> onItemRightClick(net.minecraft.world.World world, EntityPlayer player, EnumHand hand) {
            onClickAir(new Player(player), World.get(world), Hand.from(hand));
            return super.onItemRightClick(world, player, hand);
        }
        @Override
        public final boolean isValidArmor(net.minecraft.item.ItemStack stack, EntityEquipmentSlot armorType, net.minecraft.entity.Entity entity) {
            return ItemBase.this.isValidArmor(new ItemStack(stack), ArmorSlot.from(armorType), new Entity(entity));
        }

        @Override
        public final String getUnlocalizedName(net.minecraft.item.ItemStack stack) {
            applyCustomName(new ItemStack(stack));
            return super.getUnlocalizedName(stack);
        }

        @Override
        public final CreativeTabs[] getCreativeTabs()
        {
            return Arrays.stream(ItemBase.this.creativeTabs).map((CreativeTab tab) -> tab.internal).toArray(CreativeTabs[]::new);
        }
    }

    /* Overrides */

    public List<ItemStack> getItemVariants(CreativeTab creativeTab) {
        List<ItemStack> res = new ArrayList<>();
        if (creativeTab == null || creativeTab.internal == internal.getCreativeTab()) {
            res.add(new ItemStack(internal, 1));
        }
        return res;
    }

    public void addInformation(ItemStack itemStack, List<String> tooltip) {
    }

    public ClickResult onClickBlock(Player player, World world, Vec3i vec3i, Hand from, Facing from1, Vec3d vec3d) {
        return ClickResult.PASS;
    }

    public void onClickAir(Player player, World world, Hand hand) {

    }

    public boolean isValidArmor(ItemStack itemStack, ArmorSlot from, Entity entity) {
        return internal.isValidArmor(itemStack.internal, from.internal, entity.internal);
    }

    /* Name Hacks */

    public String getCustomName(ItemStack stack) {
        return null;
    }

    protected final void applyCustomName(ItemStack stack) {
        String custom = getCustomName(stack);
        if (custom != null) {
            stack.internal.setStackDisplayName(TextFormatting.RESET + custom);
        }
    }


    public Identifier getRegistryName() {
        return new Identifier(internal.getRegistryName());
    }
}
