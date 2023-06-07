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

        if (match(STEEL, STEEL, this, other)) {
            //https://www.engineeringtoolbox.com/friction-coefficients-d_778.html
            return kinetic ? 0.42f : 0.8f;
        }
        if (match(STEEL, CAST_IRON, this, other)) {
            return kinetic ? 0.25f : 0.4f;
        }
        if (match(STEEL, WOOD, this, other)) {
            return kinetic ? 0.2f : 0.6f;
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
