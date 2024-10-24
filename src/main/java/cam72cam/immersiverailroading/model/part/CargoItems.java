package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.entity.Freight;
import cam72cam.immersiverailroading.items.ItemRollingStock;
import cam72cam.immersiverailroading.items.ItemRollingStockComponent;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.item.Fuzzy;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.obj.OBJRender;
import cam72cam.mod.render.opengl.RenderState;
import org.apache.commons.lang3.tuple.Pair;
import util.Matrix4;

import java.util.*;

public class CargoItems {
    private final Map<UUID, StandardModel> cache = new HashMap<>();
    private Map<UUID, Long> lastUpdate = new HashMap<>();

    private final List<ModelComponent> components;
    private final List<Pair<Vec3d, Vec3d>> hitBox;

    public static CargoItems get(ComponentProvider provider) {
        List<ModelComponent> found = provider.parseAll(ModelComponentType.CARGO_ITEMS_X);
        return found.isEmpty() ? null : new CargoItems(found);
    }

    public CargoItems(List<ModelComponent> components) {
        this.components = components;
        this.hitBox = new LinkedList<>();
        this.components.forEach(modelComponent -> hitBox.add(Pair.of(modelComponent.min, modelComponent.max)));
    }

    public boolean checkInBound(Vec3d pos) {
        return this.hitBox.stream().anyMatch(box -> IBoundingBox.from(box.getLeft(), box.getRight()).contains(pos));
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
                double xSize = comp.length();
                double ySize = comp.height();
                double zSize = comp.width();

                double modelVolume = xSize * ySize * zSize;
                int cargoSlots = (int) Math.ceil(stock.cargoItems.getSlotCount() * modelVolume/totalVolume);

                // Goal: We want the biggest cubes to fill in the space
                // Define fitment: How close each of the dimensions are to their reference given a specific scale
                // These numbers are small enough that we can test each permutation.

                // The "ideal" storage ratio
                double ratio = Math.pow(cargoSlots / modelVolume, 0.33333);
                double xItemsIdeal = ratio * xSize;
                double yItemsIdeal = ratio * ySize;
                double zItemsIdeal = ratio * zSize;

                double bestFit = Double.MAX_VALUE;
                int xItems = (int)Math.round(xItemsIdeal);
                int yItems = (int)Math.round(yItemsIdeal);
                int zItems = (int)Math.round(zItemsIdeal);

                for (int x = 1; x < cargoSlots; x++) {
                    for (int y = 1; y < cargoSlots; y++) {
                        for (int z = 1; z < cargoSlots; z++) {
                            int currentSlots = x * y * z;
                            if (currentSlots < cargoSlots) {
                                continue;
                            }
                            double fitSize = Math.abs(1 - x / xItemsIdeal) + Math.abs(1 - y / yItemsIdeal) + Math.abs(1 - z / zItemsIdeal);
                            double fitRatio = Math.max(xSize / x, Math.max(ySize / y, zSize / z));
                            int fitItems = Math.abs(1 - currentSlots / cargoSlots);
                            double fit = fitSize + fitItems + fitRatio;
                            if (fit < bestFit) {
                                bestFit = fit;
                                xItems = x;
                                yItems = y;
                                zItems = z;
                            }
                        }
                    }
                }

                double xScale = xSize / xItems;
                double yScale = ySize / yItems;
                double zScale = zSize / zItems;
                if (!comp.key.contains("STRETCHED")) {
                    xScale = yScale = zScale = Math.min(xScale, Math.min(yScale, zScale));
                }

                // Center X, Z
                Vec3d offset = comp.min.add((xSize - xItems * xScale)/2,0,(zSize - zItems * zScale)/2);

                int renderSlot = 0;
                for (int i = slotOffset; i < Math.min(slotOffset+ cargoSlots, stock.cargoItems.getSlotCount()); i++) {
                    ItemStack stack = stock.cargoItems.get(i);
                    if (stack.isEmpty()) {
                        continue;
                    }


                    if (comp.key.contains("IRSIZE")) {
                        if (stack.is(IRItems.ITEM_ROLLING_STOCK)) {
                            ItemRollingStock.Data data = new ItemRollingStock.Data(stack);
                            Vec3d pos = new Vec3d(comp.center.x, comp.min.y, comp.center.z);//.scale(stock.gauge.scale());
                            model.addCustom((s, pt) -> {
                                s.translate(pos);
                                s.scale(1 / stock.gauge.scale(), 1 / stock.gauge.scale(), 1 / stock.gauge.scale());
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
                            Vec3d pos = new Vec3d(comp.center.x, comp.min.y, comp.center.z);//.scale(stock.gauge.scale());
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
                                s.scale(1 / stock.gauge.scale(), 1 / stock.gauge.scale(), 1 / stock.gauge.scale());
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
                    int z = renderSlot % zItems;
                    int x = (renderSlot / zItems) % xItems;
                    int y = (renderSlot / zItems / xItems) % yItems;

                    // Fill from center Z and X
                    z = zItems/2 + ((z % 2 == 0) ? z/2 : -(z+1)/2);
                    x = xItems/2 + ((x % 2 == 0) ? x/2 : -(x+1)/2);

                    Matrix4 matrix = new Matrix4()
                            //.scale(stock.gauge.scale(), stock.gauge.scale(), stock.gauge.scale())
                            .translate(offset.x, offset.y, offset.z)
                            .scale(xScale, yScale, zScale)
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
                slotOffset += cargoSlots;
            }
            cache.put(stock.getUUID(), model);
        }
        model.render(state);
    }
}
