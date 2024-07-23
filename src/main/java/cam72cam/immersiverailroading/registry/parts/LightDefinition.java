package cam72cam.immersiverailroading.registry.parts;

import cam72cam.immersiverailroading.ImmersiveRailroading;
import cam72cam.immersiverailroading.util.DataBlock;
import cam72cam.mod.resource.Identifier;

public class LightDefinition {
    public static final Identifier default_light_tex = new Identifier(ImmersiveRailroading.MODID, "textures/light.png");

    public final float blinkIntervalSeconds;
    public final float blinkOffsetSeconds;
    public final boolean blinkFullBright;
    public final String reverseColor;
    public final Identifier lightTex;
    public final boolean castsLight;

    public LightDefinition(DataBlock data) {
        blinkIntervalSeconds = data.getValue("blinkIntervalSeconds").asFloat(0f);
        blinkOffsetSeconds = data.getValue("blinkOffsetSeconds").asFloat(0f);
        blinkFullBright = data.getValue("blinkFullBright").asBoolean(true);
        reverseColor = data.getValue("reverseColor").asString();
        lightTex = data.getValue("texture").asIdentifier(default_light_tex);
        castsLight = data.getValue("castsLight").asBoolean(true);
    }
}
