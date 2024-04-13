package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.model.components.ModelComponent;

import java.util.List;

public class ElectricExhaust extends DieselExhaust{

    private final List<ModelComponent> components;

    public ElectricExhaust(List<ModelComponent> components) {
        super(components);
        this.components = components;
    }
}
