package cam72cam.mod.entity.custom;

import cam72cam.mod.entity.DamageType;
import cam72cam.mod.entity.Entity;

public interface IKillable {
    void onDamage(DamageType type, Entity source, float amount);
    void onRemoved();

    static IKillable get(Object o) {
        if (o instanceof IKillable) {
            return (IKillable)o;
        }
        return NOP;
    }
    IKillable NOP = new IKillable() {
        @Override
        public void onDamage(DamageType type, Entity source, float amount) {

        }

        @Override
        public void onRemoved() {

        }
    };
}
