package cam72cam.immersiverailroading.items;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.library.GuiText;
import cam72cam.immersiverailroading.library.GuiTypes;
import cam72cam.immersiverailroading.multiblock.Multiblock;
import cam72cam.immersiverailroading.multiblock.MultiblockRegistry;
import cam72cam.mod.entity.Player;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.item.CreativeTab;
import cam72cam.mod.item.CustomItem;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Rotation;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.serialization.TagMapper;
import cam72cam.mod.util.Facing;
import cam72cam.mod.world.World;

import java.util.Collections;
import java.util.List;

public class ItemMultiblockBlueprint extends CustomItem {
    public ItemMultiblockBlueprint() {
        super(ImmersiveRailroading.MODID, "item_multiblock");
    }

    @Override
    public int getStackSize() {
        return 1;
    }

    @Override
    public List<CreativeTab> getCreativeTabs() {
        return Collections.singletonList(ItemTabs.MAIN_TAB);
    }

    @Override
    public List<String> getTooltip(ItemStack stack) {
        Multiblock mb = new ItemMultiblockBlueprint.Data(stack).multiblock;
        if (mb == null) {
            return super.getTooltip(stack);
        }
        return Collections.singletonList(GuiText.SELECTOR_TYPE.toString(mb.getName()));
    }

    @Override
    public void onClickAir(Player player, World world, Player.Hand hand) {
        if(world.isClient && hand == Player.Hand.PRIMARY){
            GuiTypes.MULTIBLOCK_SELECTOR.open(player);
        }
    }

    @Override
    public ClickResult onClickBlock(Player player, World world, Vec3i pos, Player.Hand hand, Facing facing, Vec3d hit) {
        if (world.isServer && hand == Player.Hand.PRIMARY) {
            ItemStack item = player.getHeldItem(hand);
            Multiblock current = new Data(item).multiblock;
            if (current == null) {
                return ClickResult.ACCEPTED;
            }
            Vec3i realPos = pos;
            if (facing == Facing.DOWN) {
                realPos = realPos.down();
            }
            if (facing == Facing.UP) {
                realPos = realPos.up();
            }
            current.place(world, player, realPos, Rotation.from(Facing.fromAngle(player.getYawHead() + 180)));
        }
        return ClickResult.ACCEPTED;
    }

    public static class Data extends ItemDataSerializer {
        @TagField(value = "name", mapper = MBTagMapper.class)
        public Multiblock multiblock;

        private static class MBTagMapper implements TagMapper<Multiblock> {
            @Override
            public TagAccessor<Multiblock> apply(Class<Multiblock> type, String fieldName, TagField tag) {
                return new TagAccessor<>(
                        (d, m) -> d.setString(fieldName, m != null ? m.getName() : null),
                        d -> {
                            String name = d.getString(fieldName);
                            return name != null ? MultiblockRegistry.get(name) : null;
                        }
                );
            }
        }

        public Data(ItemStack stack) {
            super(stack);

            if (multiblock == null) {
                multiblock = MultiblockRegistry.registered().isEmpty() ? null : MultiblockRegistry.registered().get(0);
            }
        }
    }
}
