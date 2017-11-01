package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;

import cam72cam.immersiverailroading.util.Speed;

public class Train extends ArrayList<EntityCoupleableRollingStock> {
	private static final long serialVersionUID = 1L;

	public Speed getMovement(Speed speed, boolean isReverse) {
		//http://evilgeniustech.com/idiotsGuideToRailroadPhysics/HorsepowerAndTractiveEffort/
		//http://www.republiclocomotive.com/locomotive-power-calculations.html
		//http://www.wplives.org/forms_and_documents/Air_Brake_Principles.pdf
		
		// ABS
		speed = Speed.fromMinecraft(Math.abs(speed.minecraft()));
		
		double tractiveEffortNewtons = 0;
		double airBrake = 0;
		
		//lbs
		double rollingResistanceNewtons = 0;
		double gradeForceNewtons = 0;
		//TODO starting effort
		double massToMoveKg = 0;
		for (EntityCoupleableRollingStock e : this) {
			massToMoveKg += e.getWeight();
			
			double stockMassLb = 2.20462 * e.getWeight();
			rollingResistanceNewtons += 0.0015 * stockMassLb * 4.44822f;
			
			//Grade forces
			// TODO force while not moving
			
			double grade = -Math.tan(Math.toRadians(e.rotationPitch % 90));
			
			// lbs * 1%gradeResistance * grade multiplier
			gradeForceNewtons += (stockMassLb / 100) * (grade * 100)  * 4.44822f;
		}
		
		for (EntityCoupleableRollingStock e : this) {
			if (e instanceof Locomotive) {
				Locomotive loco = (Locomotive) e;
				tractiveEffortNewtons += loco.getTractiveEffortNewtons(speed) * (loco.isReverse ? -1 : 1);
				airBrake += loco.getAirBrake();
			}
		}

		// 0.25 = steel wheel on steel rail
		double brakeAdhesion =  massToMoveKg * 0.25;
		double airBrakeNewtons = brakeAdhesion * Math.min(airBrake, 1) * 4.44822f;
		
		double reverseMultiplier = (isReverse ? -1 : 1);
		
		// a = f (to newtons) * m (to newtons)
		double tractiveAccell = tractiveEffortNewtons / massToMoveKg;
		double resistanceAccell = rollingResistanceNewtons / massToMoveKg;
		double gradeAccell = gradeForceNewtons / massToMoveKg;
		double brakeAccell = airBrakeNewtons / massToMoveKg;
		
		
		
		double currentMCVelocity = speed.minecraft() * reverseMultiplier;
		double deltaAccellTractiveMCVelocity = Speed.fromMetric(tractiveAccell).minecraft() * reverseMultiplier;
		
		// Limit decell to current speed to trains stop
		// Apply in the reverse direction of current travel
		double deltaAccellRollingResistanceMCVelocity = Math.min(Speed.fromMetric(resistanceAccell).minecraft(), speed.minecraft()) * -reverseMultiplier;
		
		double deltaAccellGradeMCVelocity = Speed.fromMetric(gradeAccell).minecraft();
		
		double deltaAccellBrakeMCVelocity = Math.min(Speed.fromMetric(brakeAccell).minecraft(), speed.minecraft()) * -reverseMultiplier;
		
		// Limit decell to current speed to trains stop
		// Apply in the reverse direction of current travel
		double newMCVelocity = currentMCVelocity + deltaAccellTractiveMCVelocity + deltaAccellRollingResistanceMCVelocity + deltaAccellGradeMCVelocity + deltaAccellBrakeMCVelocity;

		if (Math.abs(newMCVelocity) < 0.001) {
			newMCVelocity = 0;
		}
		
		//TODO NOW if (Math.abs(newMCVelocity) > this.getDefinition().getMaxSpeed().minecraft()) {
			//newMCVelocity = Math.copySign(this.getDefinition().getMaxSpeed().minecraft(), newMCVelocity);
		//}
		
		return Speed.fromMinecraft(newMCVelocity);
	}

	/*
	 * THIS IS WRONG AND NEEDS TO BE FIXED!
	 */
	public void smoothSpeeds() {
		//double massToMoveKg = 0;
		double eSpeed = 0;
		for (EntityCoupleableRollingStock e : this) {
			eSpeed += e.getCurrentSpeed().metric();
			/*
			double delta = eSpeed - speed.metric();
			if (Math.abs(delta) > 1) {
				if (eSpeed > speed.metric()) {
					double hvWeight = e.getWeight();
					double lvWeight = massToMoveKg;
					double ratio = hvWeight / (lvWeight + hvWeight);
					double newSpeed = delta * ratio;
					speed = Speed.fromMetric(newSpeed);
				} else {
					double lvWeight = e.getWeight();
					double hvWeight = massToMoveKg;
					double ratio = hvWeight / (lvWeight + hvWeight);
					double newSpeed = delta * ratio;
					speed = Speed.fromMetric(newSpeed);
				}
			}*/
			
			//massToMoveKg += e.getWeight();
		}
		
		// Take the avg speed.  Not that accurate
		Speed speed = Speed.fromMetric(eSpeed / this.size());
		
		for (EntityCoupleableRollingStock e : this) {
			e.setCurrentSpeed(speed);
			// Do I need to set isReverse in here?
			// Or perhaps a math.copySign for speed?
			if (e.positions.size() != 0) {
				e.positions.get(0).speed = speed;
				if (e.positions.size() > 1) {
					e.positions.get(1).speed = speed;
				}
			}
		}
	}
}
