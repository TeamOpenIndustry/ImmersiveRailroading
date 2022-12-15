package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.Freight;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.StandardModel;
import cam72cam.mod.render.opengl.RenderState;
import util.Matrix4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CargoItems {
    private final Map<UUID, StandardModel> cache = new HashMap<>();
    private long lastUpdate = 0;

    private final List<ModelComponent> areas;

    public static CargoItems get(ComponentProvider provider) {
        List<ModelComponent> found = provider.parseAll(ModelComponentType.CARGO_ITEMS_X);
        return found.isEmpty() ? null : new CargoItems(found);
    }

    public CargoItems(List<ModelComponent> areas) {
        this.areas = areas;
    }

    public <T extends Freight> void postRender(T stock, RenderState state) {
        if (stock.getWorld().getTicks() > lastUpdate + 40) {
            cache.clear();
            lastUpdate = stock.getWorld().getTicks();
        }

        StandardModel model = cache.get(stock.getUUID());
        if (model == null) {
            model = new StandardModel();

            double totalVolume = areas.stream().mapToDouble(a -> a.length() * a.width() * a.height()).sum();
            int slotOffset = 0;
            for (ModelComponent area : areas) {
                double itemsX = area.length();
                double itemsY = area.height();
                double itemsZ = area.width();

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
                    int x = renderSlot % (int) itemsX;
                    int z = (renderSlot / (int) itemsX) % (int) itemsZ;
                    int y = (renderSlot / (int) itemsX / (int) itemsZ) % (int) itemsY;

                    model.addItem(
                            stack,
                            new Matrix4().
                                    translate(area.min.x, area.min.y, area.min.z)
                                    .scale(area.length() / itemsX, area.height() / itemsY, area.width() / itemsZ)
                                    .translate(0.5, 0.5, 0.5)
                                    .translate(x, y, z)
                    );
                    renderSlot++;
                }
                slotOffset += cargoVolume;
            }
            cache.put(stock.getUUID(), model);
        }
        model.render(state);
    }
}
