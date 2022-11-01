package cam72cam.immersiverailroading.model;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.entity.LocomotiveDiesel;
import cam72cam.immersiverailroading.gui.overlay.Readouts;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.model.part.*;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.LocomotiveDieselDefinition;

import java.util.List;
import java.lang.Integer;

public class DieselLocomotiveModel extends LocomotiveModel<LocomotiveDiesel> {
    private List<ModelComponent> components;
    private DieselExhaust exhaust;
    private Horn horn;
    private final PartSound idle;
    private final PartSound motor;
    private final PartSound motor2x;
    private int div;
    private int previsnt2x=1;
    
    public DieselLocomotiveModel(LocomotiveDieselDefinition def) throws Exception {
        super(def);
        idle = def.isCabCar() ? null : new PartSound(stock -> ImmersiveRailroading.newSound(def.idle, true, 80, stock.soundGauge()));
        motor = def.isCabCar() ? null : new PartSound(stock -> ImmersiveRailroading.newSound(def.motor, true, 80, stock.soundGauge()));
        motor2x = def.isCabCar() ? null : new PartSound(stock -> ImmersiveRailroading.newSound(def.motor2x, true, 80, stock.soundGauge()));        
    }

    @Override
    protected void parseControllable(ComponentProvider provider, EntityRollingStockDefinition def) {
        super.parseControllable(provider, def);
        addGauge(provider, ModelComponentType.GAUGE_TEMPERATURE_X, Readouts.TEMPERATURE);
        addControl(provider, ModelComponentType.ENGINE_START_X);
        addControl(provider, ModelComponentType.HORN_CONTROL_X);
    }

    @Override
    protected void parseComponents(ComponentProvider provider, EntityRollingStockDefinition def) {
        components = provider.parse(
                ModelComponentType.FUEL_TANK,
                ModelComponentType.ALTERNATOR,
                ModelComponentType.ENGINE_BLOCK,
                ModelComponentType.CRANKSHAFT,
                ModelComponentType.GEARBOX,
                ModelComponentType.FLUID_COUPLING,
                ModelComponentType.FINAL_DRIVE,
                ModelComponentType.TORQUE_CONVERTER
        );

        components.addAll(
                provider.parseAll(
                        ModelComponentType.PISTON_X,
                        ModelComponentType.FAN_X,
                        ModelComponentType.DRIVE_SHAFT_X
                )
        );

        exhaust = DieselExhaust.get(provider);
        horn = Horn.get(provider, ((LocomotiveDieselDefinition)def).horn, ((LocomotiveDieselDefinition)def).getHornSus());

        super.parseComponents(provider, def);
    }
	
    void domotorsound(LocomotiveDiesel stock){

    }
    
    @Override
    protected void effects(LocomotiveDiesel stock) {
        super.effects(stock);
        exhaust.effects(stock);
        horn.effects(stock,
                stock.getHornTime() > 0 && (stock.isRunning() || stock.getDefinition().isCabCar())
                        ? stock.getDefinition().getHornSus() ? stock.getHornTime() / 10f : 1
                        : 0);
        if (idle != null) {
            idle.effects(stock, stock.isRunning() ? Math.max(0.1f, stock.getSoundThrottle()) : 0, 0.7f + stock.getSoundThrottle() / 4);
        }
        if(motor != null && motor2x != null){
		
		div = stock.getDefinition().motordiv;
                float adjust = Math.abs((float)stock.getCurrentSpeed().metric()) / div;
                float pitch = adjust;               
                float volume = ((0.01f + adjust) * stock.getSoundMotorThrottle() + ((0.01f + adjust) * stock.getTrainBrake()))*2;
		if(pitch<2){
                	if(previsnt2x==0){
                		motor2x.removed(stock);
                		previsnt2x=1;
                	}
            		motor.effects(stock, volume, pitch);
                }
                else{
                	if(previsnt2x==1){
                		motor.removed(stock);
                		previsnt2x=0;
                	}
                	motor2x.effects(stock, volume, pitch-1.0f);
                }

        }
    }

    @Override
    protected void removed(LocomotiveDiesel stock) {
        super.removed(stock);
        horn.removed(stock);
        if (idle != null) {
            idle.removed(stock);
        }
        if (motor != null) {
            motor.removed(stock);
        }
        if (motor2x != null) {
            motor2x.removed(stock);
        }
    }

    @Override
    protected void render(LocomotiveDiesel stock, ComponentRenderer draw, double distanceTraveled) {
        super.render(stock, draw, distanceTraveled);
        draw.render(components);
        horn.render(draw);
    }
} 
