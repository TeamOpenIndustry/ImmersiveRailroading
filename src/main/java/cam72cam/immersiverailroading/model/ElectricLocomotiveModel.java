package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.ConfigSound;
import cam72cam.immersiverailroading.entity.LocomotiveElectric;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.LocomotiveModel;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.model.part.DieselExhaust;
import cam72cam.immersiverailroading.model.part.Horn;
import cam72cam.immersiverailroading.model.part.PartSound;
import cam72cam.immersiverailroading.registry.LocomotiveElectricDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ElectricLocomotiveModel extends LocomotiveModel<LocomotiveElectric, LocomotiveElectricDefinition> {
    private List<ModelComponent> components;
    private Horn horn;
    private final PartSound idle;
    private final PartSound running;
    private final Map<UUID, Float> runningFade = new HashMap<>();

    public ElectricLocomotiveModel(LocomotiveElectricDefinition def) throws Exception {
        super(def);
        this.idle = def.isCabCar() ? null : new PartSound(def.idle, true, 80.0F, ConfigSound.SoundCategories.Locomotive.Diesel::idle);
        this.running = !def.isCabCar() && def.running != null ? new PartSound(def.running, true, 80.0F, ConfigSound.SoundCategories.Locomotive.Diesel::running) : null;
    }

    protected void parseControllable(ComponentProvider provider, LocomotiveElectricDefinition def) {
        super.parseControllable(provider, def);
        this.addGauge(provider, ModelComponentType.GAUGE_TEMPERATURE_X, Readouts.TEMPERATURE);
        this.addControl(provider, ModelComponentType.ENGINE_START_X);
        this.addControl(provider, ModelComponentType.HORN_CONTROL_X);
    }

    protected void parseComponents(ComponentProvider provider, LocomotiveElectricDefinition def) {
        this.components = provider.parse(ModelComponentType.FUEL_TANK, ModelComponentType.ALTERNATOR, ModelComponentType.ENGINE_BLOCK, ModelComponentType.CRANKSHAFT, ModelComponentType.GEARBOX, ModelComponentType.FLUID_COUPLING, ModelComponentType.FINAL_DRIVE, ModelComponentType.TORQUE_CONVERTER);
        this.components.addAll(provider.parseAll(ModelComponentType.PISTON_X, ModelComponentType.FAN_X, ModelComponentType.DRIVE_SHAFT_X));
        this.rocking.include(this.components);
        this.horn = Horn.get(provider, this.rocking, def.horn, def.getHornSus());
        super.parseComponents(provider, def);
    }

    protected void effects(LocomotiveElectric stock) {
        super.effects(stock);
        this.horn.effects(stock, stock.getHornTime() <= 0 || !stock.isRunning() && !stock.getDefinition().isCabCar() ? 0.0F : (stock.getDefinition().getHornSus() ? (float)stock.getHornTime() / 10.0F : 1.0F));
        if (this.idle != null) {
            if (stock.isRunning()) {
                float volume = Math.max(0.1F, stock.getRelativeRPM());
                float pitchRange = stock.getDefinition().getEnginePitchRange();
                float pitch = 1.0F - pitchRange + stock.getRelativeRPM() * pitchRange;
                if (this.running == null) {
                    this.idle.effects(stock, volume, pitch);
                } else {
                    boolean isThrottledUp = (double)stock.getRelativeRPM() > 0.01;
                    float fade = (Float)this.runningFade.getOrDefault(stock.getUUID(), 0.0F);
                    fade += 0.05F * (float)(isThrottledUp ? 1 : -1);
                    fade = Math.min(Math.max(fade, 0.0F), 1.0F);
                    this.runningFade.put(stock.getUUID(), fade);
                    this.idle.effects(stock, 1.0F - fade + 0.01F, 1.0F);
                    this.running.effects(stock, fade + 0.01F, pitch);
                }
            } else {
                this.idle.effects(stock, false);
                if (this.running != null) {
                    this.running.effects(stock, false);
                    this.runningFade.put(stock.getUUID(), 0.0F);
                }
            }
        }

    }

    protected void removed(LocomotiveElectric stock) {
        super.removed(stock);
        this.horn.removed(stock);
        if (this.idle != null) {
            this.idle.removed(stock);
        }

        if (this.running != null) {
            this.running.removed(stock);
        }
    }
}
