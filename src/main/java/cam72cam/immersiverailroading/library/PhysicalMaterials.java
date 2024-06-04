package cam72cam.immersiverailroading.library;

public enum PhysicalMaterials {
    STEEL,
    CAST_IRON,
    WOOD,
    ;

    private boolean match(PhysicalMaterials materialA, PhysicalMaterials materialB, PhysicalMaterials matchA, PhysicalMaterials matchB) {
        return  materialA == matchA && materialB == matchB ||
                materialA == matchB && materialB == matchA;
    }

    private float friction(PhysicalMaterials other, boolean kinetic) {
        // unless otherwise specified: https://structx.com/Material_Properties_005a.html

        float dirty = 0.5f;

        if (match(STEEL, STEEL, this, other)) {
            // assume slightly dirty / non-ideal surfaces
            return (kinetic ? 0.5f : 0.8f) * dirty;
        }
        if (match(STEEL, CAST_IRON, this, other)) {
            return (kinetic ? 0.25f : 0.4f) * dirty;
        }
        if (match(STEEL, WOOD, this, other)) {
            return (kinetic ? 0.2f : 0.6f) * dirty;
        }

        return 0;
    }

    public float staticFriction(PhysicalMaterials other) {
        return friction(other, false);
    }
    public float kineticFriction(PhysicalMaterials other) {
        return friction(other, true);
    }
}
