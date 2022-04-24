package cam72cam.immersiverailroading.physics;

import cam72cam.mod.math.Vec3d;

public class Units {
    private Units(){
    }

    public static class Velocity {
        private Vec3d mt;
        private Velocity(Vec3d mt) {
            this.mt = mt;
        }

        public static Velocity fromMT(Vec3d mt) {
            return new Velocity(mt);
        }

        public static Velocity fromMS(Vec3d ms) {
            return new Velocity(ms.scale(1f/20));
        }

        public Velocity invert() {
            return new Velocity(mt.scale(-1));
        }

        public Vec3d applyTo(Vec3d position) {
            return position.add(mt);
        }

        public Vec3d asMT() {
            return mt;
        }

        public Velocity add(Velocity other) {
            return new Velocity(mt.add(other.mt));
        }
    }

    public static class Acceleration {
        public static final Acceleration ZERO = fromMTT(Vec3d.ZERO);
        static final Acceleration GRAVITY = Acceleration.fromMSS(new Vec3d(0, -9.8, 0));

        // meters per tick per tick
        private Vec3d mtt;
        private Acceleration(Vec3d mtt) {
            this.mtt = mtt;
        }

        public static Acceleration fromMTT(Vec3d mtt) {
            return new Acceleration(mtt);
        }

        public static Acceleration fromMSS(Vec3d mss) {
            return new Acceleration(mss.scale(1f/20/20));
        }

        public Acceleration scale(double scale) {
            return new Acceleration(mtt.scale(scale));
        }

        public Acceleration invert() {
            return scale(-1);
        }

        public Acceleration add(Acceleration other) {
            return new Acceleration(mtt.add(other.mtt));
        }

        public Vec3d toMTT() {
            return mtt;
        }

        public Velocity deltaVelocity() {
            return Velocity.fromMT(mtt);
        }
    }

    public static class Mass {
        private final double kg;

        private Mass(double kg) {
            this.kg = kg;
        }

        public static Mass fromKG(double kg) {
            return new Mass(kg);
        }

        public Mass scale(double scale) {
            return new Mass(kg * scale);
        }
    }

    public static class Force {
        public static final Force ZERO = new Force(Vec3d.ZERO);
        private final Vec3d newtons;

        private Force(Vec3d newtons) {
            this.newtons = newtons;
        }

        public Force(Acceleration acc, Mass m) {
            this(acc.mtt.scale(m.kg));
        }

        public static Force fromNewtons(Vec3d newtons) {
            return new Force(newtons);
        }

        public Acceleration toAcceleration(Mass mass) {
            return new Acceleration(newtons.scale(1/mass.kg));
        }

        public Force scale(double v) {
            return new Force(newtons.scale(v));
        }

        public Force invert() {
            return scale(-1);
        }

        public Force add(Force force) {
            return new Force(this.newtons.add(force.newtons));
        }

        public Force yComponent() {
            return new Force(new Vec3d(0, newtons.y, 0));
        }

        public Vec3d toNewtons() {
            return newtons;
        }
    }
}
