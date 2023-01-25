package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.entity.Freight;
import cam72cam.immersiverailroading.items.ItemRollingStock;
import cam72cam.immersiverailroading.items.ItemRollingStockComponent;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.render.opengl.RenderState;
import util.Matrix4;

import java.util.*;

public class CargoItems {
    private final Map<UUID, StandardModel> cache = new HashMap<>();
    private Map<UUID, Long> lastUpdate = new HashMap<>();

    private final List<ModelComponent> components;

    public static CargoItems get(ComponentProvider provider) {
        List<ModelComponent> found = provider.parseAll(ModelComponentType.CARGO_ITEMS_X);
        return found.isEmpty() ? null : new CargoItems(found);
    }

    public CargoItems(List<ModelComponent> components) {
        this.components = components;
    }

    public <T extends Freight> void postRender(T stock, RenderState state) {
        if (stock.getWorld().getTicks() > lastUpdate.getOrDefault(stock.getUUID(), 0L) + 40) {
            cache.clear();
            lastUpdate.put(stock.getUUID(), stock.getWorld().getTicks());
        }

        StandardModel model = cache.get(stock.getUUID());
        if (model == null) {
            model = new StandardModel();

            double totalVolume = components.stream().mapToDouble(a -> a.length() * a.width() * a.height()).sum();
            int slotOffset = 0;

            Double minY = null;
            for (int i = 0; i < stock.cargoItems.getSlotCount(); i++) {
                ItemStack stack = stock.cargoItems.get(i);
                if (stack.is(IRItems.ITEM_ROLLING_STOCK_COMPONENT)) {
                    ItemRollingStockComponent.Data data = new ItemRollingStockComponent.Data(stack);
                    for (ModelComponentType r : data.componentType.render) {
                        List<ModelComponent> mc = data.def.getComponents(r);
                        if (mc == null) {
                            continue;
                        }
                        for (int j = 0; j < Math.min(mc.size(), stack.getCount()); j++) {
                            if (minY == null || minY > mc.get(j).min.y) {
                                minY = mc.get(j).min.y;
                            }
                        }
                    }
                }
            }
            double componentOffset = minY == null ? 0 : minY;


            for (ModelComponent comp : components) {
                double itemsX = comp.length();
                double itemsY = comp.height();
                double itemsZ = comp.width();

                double modelVolume = itemsX * itemsY * itemsZ;
                int cargoVolume = (int) Math.ceil(stock.cargoItems.getSlotCount() * modelVolume/totalVolume);

                double volumeRatio = Math.pow(cargoVolume / modelVolume, 0.3333);
                itemsY = Math.ceil(itemsY * volumeRatio);

                modelVolume = itemsX * itemsY * itemsZ;
                volumeRatio = Math.pow(cargoVolume / modelVolume, 0.3333);
                itemsZ = Math.ceil(itemsZ * volumeRatio);

                modelVolume = itemsX * itemsY * itemsZ;
                volumeRatio = cargoVolume / modelVolume;
                itemsX = Math.ceil(itemsX * volumeRatio);

                int renderSlot = 0;
                for (int i = slotOffset; i < Math.min(slotOffset+ cargoVolume, stock.cargoItems.getSlotCount()); i++) {
                    ItemStack stack = stock.cargoItems.get(i);
                    if (stack.isEmpty()) {
                        continue;
                    }


                    if (comp.key.contains("IRSIZE")) {
                        if (stack.is(IRItems.ITEM_ROLLING_STOCK)) {
                            ItemRollingStock.Data data = new ItemRollingStock.Data(stack);
                            Vec3d pos = new Vec3d(comp.center.x, comp.min.y, comp.center.z).scale(stock.gauge.scale());
                            model.addCustom((s, pt) -> {
                                s.translate(pos);
                                s.scale(data.gauge.scale(), data.gauge.scale(), data.gauge.scale());
                                try (OBJRender.Binding binder = data.def.getModel().binder().texture(data.texture).bind(s)) {
                                    binder.draw(data.def.itemGroups);
                                }
                            });
                            renderSlot++;
                            continue;
                        }
                        if (stack.is(IRItems.ITEM_ROLLING_STOCK_COMPONENT)) {
                            ItemRollingStockComponent.Data data = new ItemRollingStockComponent.Data(stack);
                            Vec3d pos = new Vec3d(comp.center.x, comp.min.y, comp.center.z).scale(stock.gauge.scale());
                            List<String> groups = new ArrayList<>();

                            for (ModelComponentType r : data.componentType.render) {
                                List<ModelComponent> mc = data.def.getComponents(r);
                                if (mc == null || r == ModelComponentType.CARGO_FILL_X || r == ModelComponentType.CARGO_FILL_POS_X) {
                                    continue;
                                }
                                for (int j = 0; j < Math.min(mc.size(), stack.getCount()); j++) {
                                    groups.addAll(mc.get(j).modelIDs);
                                }
                            }
                            model.addCustom((s, pt) -> {
                                s.translate(pos);
                                s.scale(data.gauge.scale(), data.gauge.scale(), data.gauge.scale());
                                s.translate(0, -componentOffset, 0);
                                try (OBJRender.Binding binder = data.def.getModel().binder().texture(data.texture).bind(s)) {
                                    binder.draw(groups);
                                }
                            });
                            renderSlot++;
                            continue;
                        }
                    }
                    int x = renderSlot % (int) itemsX;
                    int z = (renderSlot / (int) itemsX) % (int) itemsZ;
                    int y = (renderSlot / (int) itemsX / (int) itemsZ) % (int) itemsY;

                    double scaleX = comp.length() / itemsX;
                    double scaleY = comp.height() / itemsY;
                    double scaleZ = comp.width() / itemsZ;
                    if (!comp.key.contains("STRETCHED")) {
                        scaleX = scaleY = scaleZ = Math.min(comp.length() / itemsX, Math.min(comp.height() / itemsY, comp.width() / itemsZ));
                    }

                    Matrix4 matrix = new Matrix4()
                            .scale(stock.gauge.scale(), stock.gauge.scale(), stock.gauge.scale())
                            .translate(comp.min.x, comp.min.y, comp.min.z)
                            .scale(scaleX, scaleY, scaleZ)
                            .translate(0.5, 0.5, 0.5)
                            .translate(x, y, z);

                    if (stack.is(IRItems.ITEM_ROLLING_STOCK) || stack.is(IRItems.ITEM_ROLLING_STOCK_COMPONENT)) {
                        matrix.rotate(Math.toRadians(-90), 0, 1, 0);
                    }
                    if (stack.is(Fuzzy.LOG_WOOD)) {
                        matrix.rotate(Math.toRadians(90), 0, 0, 1);
                    }
                    model.addItem(stack, matrix);
                    renderSlot++;
                }
                slotOffset += cargoVolume;
            }
            cache.put(stock.getUUID(), model);
        }
        model.render(state);
    }
}
