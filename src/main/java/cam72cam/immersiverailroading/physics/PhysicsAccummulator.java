package cam72cam.immersiverailroading.physics;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.entity.EntityRollingStock;
import cam72cam.immersiverailroading.entity.Locomotive;
import cam72cam.immersiverailroading.util.Speed;

public class PhysicsAccummulator {
	//http://evilgeniustech.com/idiotsGuideToRailroadPhysics/HorsepowerAndTractiveEffort/
	//http://www.republiclocomotive.com/locomotive-power-calculations.html
	//http://www.wplives.org/forms_and_documents/Air_Brake_Principles.pdf

	public double tractiveEffortNewtons = 0;
	public double airBrakeEffect = 0;
	//lbs
	public double rollingResistanceNewtons = 0;
	public double gradeForceNewtons = 0;
	public double massToMoveKg = 0;
	public double wheelAdhesionNewtons = 0;
	public int count = 0;
	private TickPos pos;

	private static int debugTick = 0;
	
	public PhysicsAccummulator(TickPos pos) {
		this.pos = pos;
	}

	// accumulates all the forces currently acting on this train car
	public void accumulate(EntityRollingStock stock, Boolean direction) {
		count++;

		double poundToNewton = 4.44822;
		
		massToMoveKg += stock.getWeight();
		
		if (!(stock instanceof EntityMoveableRollingStock)){
			return;
		}
		
		EntityMoveableRollingStock movable = ((EntityMoveableRollingStock)stock);

		double stockMassLb = 2.20462 * stock.getWeight();
		rollingResistanceNewtons += 0.0015 * stockMassLb * poundToNewton;

		// see http://hyperphysics.phy-astr.gsu.edu/hbase/mincl.html for slope physics
		// negative because a positive rotationPitch means stock is pointing uphill
		double gradeForceFactor = -Math.sin(Math.toRadians(pos.rotationPitch)) * Config.ConfigBalance.slopeMultiplier;
		gradeForceNewtons += stock.getWeight() * gradeForceFactor * 9.8;
		
		if (stock instanceof Locomotive) {
			Locomotive loco = (Locomotive) stock;
			tractiveEffortNewtons += loco.getTractiveEffortNewtons(pos.speed) * (direction ? 1 : -1);

			// note, multiple locomotives in a train will conflict brake settings
			// maybe should be average of all of them?
			// previous implementation simply added all brake effects and capped at 1 (so 4 locos at 1/4 would make full braking power)
			airBrakeEffect = Math.max(loco.getAirBrake() * loco.getDefinition().getBrakePower() * loco.slipCoefficient(), airBrakeEffect);

			//wheelAdhesionNewtons += loco.getDefinition().getStartingTractionNewtons(stock.gauge); I have no idea where this came from
		}
		// mass * gravity * coefficient of friction
		wheelAdhesionNewtons += stock.getWeight() * 0.25 * 9.8;
		
		int slowdown = movable.getSpeedRetarderSlowdown(pos);
		rollingResistanceNewtons += slowdown * stockMassLb / 300;
	}

	//gets velocity in m/t for 1 tick (1/20 second) of recorded acceleration forces
	public Speed getVelocity() {
		double tickRatio = 1.0 / 400.0; // second^2 per tick^2

		// Note, brake strength (pad to wheel) is actually independent of wheel adhesion (wheel to rails)
		// It seems that we have just been assuming that brake strength is always strong enough to match wheel adhesion
		// Implementing over-braking is probably too immersive even for this mod
		// Note, this assumption means brakes are very powerful in game, roughly 2x better than real life
		double brakeForceNewtons = wheelAdhesionNewtons * airBrakeEffect * Config.ConfigBalance.brakeMultiplier;
		
		// a = f (to newtons) * m (to newtons)
		double deltaAccellTractiveMCVelocity = tractiveEffortNewtons / massToMoveKg * tickRatio;
		double deltaAccellRollingResistanceMCVelocity = rollingResistanceNewtons / massToMoveKg * tickRatio;
		double deltaAccellGradeMCVelocity = gradeForceNewtons / massToMoveKg * tickRatio; //implied * 1 for 1 tick per call
		double deltaAccellBrakeMCVelocity = brakeForceNewtons / massToMoveKg * tickRatio; //N / kg * s^2 / t^2 * t = m / t
		
		double currentMCVelocity = pos.speed.minecraft();

		// tractive acceleration and grade are always applied
		// technically tractive force and braking force should be summed and capped to wheel adhesion
		// since they are normally not applied at both the same time and same direction, just ignore for simplicity
		double newMCVelocity = currentMCVelocity + deltaAccellTractiveMCVelocity + deltaAccellGradeMCVelocity;

		if(debugTick > 38) {
			System.out.printf("New velocity before decell in meter/tick is %f\n", newMCVelocity);
		}

		// friction and brakes are limited to stopping
		// Apply in the reverse direction of current travel
		double deltaDecell = -1 * Math.copySign(Math.min(deltaAccellRollingResistanceMCVelocity +  deltaAccellBrakeMCVelocity, Math.abs(newMCVelocity)), newMCVelocity);
		
		newMCVelocity = newMCVelocity + deltaDecell;

		if(debugTick > 38) {
			debugTick = 0;
			System.out.printf("tractive force = %f\n", tractiveEffortNewtons);
			System.out.printf("adhesive force = %f\n", wheelAdhesionNewtons);
			System.out.printf("grade force = %f\n", gradeForceNewtons);
			System.out.printf("brake Acceleration = %f\n", deltaAccellBrakeMCVelocity);
			System.out.printf("Tractive Acceleration = %f\n", deltaAccellTractiveMCVelocity);
			System.out.printf("resistive Acceleration = %f\n", deltaAccellRollingResistanceMCVelocity);
			System.out.printf("grade Acceleration = %f\n", deltaAccellGradeMCVelocity);
			System.out.printf("New velocity in meter/tick is %f\n", newMCVelocity);
			System.out.println();
		}
		else {
			debugTick++;
		}

		if (Math.abs(newMCVelocity) < 0.0001) {
			newMCVelocity = 0;
		}
		
		return Speed.fromMinecraft(newMCVelocity);
	}
}