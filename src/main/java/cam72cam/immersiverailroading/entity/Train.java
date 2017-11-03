package cam72cam.immersiverailroading.entity;

import java.util.ArrayList;

import cam72cam.immersiverailroading.util.Speed;

public class Train extends ArrayList<EntityCoupleableRollingStock> {
	private static final long serialVersionUID = 1L;

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
