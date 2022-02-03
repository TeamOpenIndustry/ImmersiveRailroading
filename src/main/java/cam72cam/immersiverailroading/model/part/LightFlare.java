package cam72cam.immersiverailroading.model.part;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.library.ModelComponentType.ModelPosition;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition;
import cam72cam.immersiverailroading.registry.EntityRollingStockDefinition.LightDefinition;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.model.obj.ImageUtils;
import cam72cam.mod.render.Light;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.resource.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import util.Matrix4;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cam72cam.immersiverailroading.model.ComponentRenderer.lcgPattern;

public class LightFlare<T extends EntityMoveableRollingStock> {
    private final ModelComponent component;
    private final boolean forward;
    private static final Map<Identifier, Integer> textures = new HashMap<>();
    private final Map<UUID, List<Light>> castLights = new HashMap<>();
    private final Map<UUID, List<Vec3d>> castPositions = new HashMap<>();
    private final float red;
    private final float green;
    private final float blue;
    private final Identifier lightTex;
    private final int blinkIntervalTicks;
    private final int blinkOffsetTicks;
    private final boolean castsLights;
    private final boolean blinkFullBright;
    private final String controlGroup;
    private final boolean invert;
    private final Function<T, Matrix4> location;
    private float redReverse;
    private float greenReverse;
    private float blueReverse;

    private static final Pattern rgb = Pattern.compile(String.format(".*_0[xX](%s%<s)(%<s%<s)(%<s%<s).*", "[0-9A-Fa-f]"));

    public static <T extends EntityMoveableRollingStock> List<LightFlare<T>> get(EntityRollingStockDefinition def, ComponentProvider provider, ModelComponentType type) {
        return provider.parseAll(type).stream().map(c -> new LightFlare<T>(def, c, null)).collect(Collectors.toList());
    }

    public static <T extends EntityMoveableRollingStock> List<LightFlare<T>> get(EntityRollingStockDefinition def, ComponentProvider provider, ModelComponentType type, ModelPosition pos, Function<T, Matrix4> loc) {
        return provider.parseAll(type, pos).stream().map(c -> new LightFlare<>(def, c, loc)).collect(Collectors.toList());
    }

    private LightFlare(EntityRollingStockDefinition def, ModelComponent component, Function<T, Matrix4> loc) {
        this.component = component;
        this.location = loc;
        this.forward = component.center.x < 0;
        Matcher rgbValues = component.modelIDs.stream()
                .map(rgb::matcher)
                .filter(Matcher::matches)
                .findFirst().orElse(null);
        if (rgbValues != null) {
            this.red = Integer.parseInt(rgbValues.group(1), 16) / 255f;
            this.green = Integer.parseInt(rgbValues.group(2), 16) / 255f;
            this.blue = Integer.parseInt(rgbValues.group(3), 16) / 255f;
        } else {
            this.red = 1;
            this.green = 1;
            this.blue = 1;
        }
        this.redReverse = this.red;
        this.greenReverse = this.green;
        this.blueReverse = this.blue;
        this.controlGroup = component.modelIDs.stream()
                .map(lcgPattern::matcher).filter(Matcher::find).map(m -> m.group(1)).findFirst().orElse(null);

        this.invert = component.modelIDs.stream().anyMatch(g -> g.contains("_LINVERT_") || g.startsWith("LINVERT_") || g.endsWith("_LINVERT"));

        // This is bad...
        LightDefinition config = def.getLight(component.type.toString()
                .replace("_X", "_" + component.id)
                .replace("_POS_", "_" + component.pos + "_")
        );

        if (config != null) {
            this.lightTex = config.lightTex;
            this.blinkIntervalTicks = (int)(config.blinkIntervalSeconds * 20);
            this.blinkOffsetTicks = (int)(config.blinkOffsetSeconds * 20);
            this.blinkFullBright = config.blinkFullBright;
            this.castsLights = config.castsLight;
            if (config.reverseColor != null) {
                rgbValues = rgb.matcher("_" + config.reverseColor);
                if (rgbValues.matches()) {
                    this.redReverse = Integer.parseInt(rgbValues.group(1), 16) / 255f;
                    this.greenReverse = Integer.parseInt(rgbValues.group(2), 16) / 255f;
                    this.blueReverse = Integer.parseInt(rgbValues.group(3), 16) / 255f;
                }
            }
        } else {
            this.lightTex = LightDefinition.default_light_tex;
            this.blinkIntervalTicks = 0;
            this.blinkOffsetTicks = 0;
            this.blinkFullBright = true;
            this.castsLights = true;
        }
    }

    public void render(ComponentRenderer draw, T stock) {
        try (ComponentRenderer light = draw.withBrightGroups(blinkFullBright ? !isBlinkOff(stock) : !isLightOff(stock))) {
            if (location != null) {
                light.mult(location.apply(stock));
            }
            light.render(component);
        }
    }

    private boolean isLightOff(T stock) {
        return !stock.externalLightsEnabled() || (controlGroup != null && stock.getControlPosition(controlGroup) == (invert ? 1 : 0));
    }
    private boolean isBlinkOff(T stock) {
        return isLightOff(stock) || blinkIntervalTicks > 0 && (stock.getTickCount() + blinkOffsetTicks) % (blinkIntervalTicks*2) > blinkIntervalTicks;
    }

    public void postRender(T stock) {
        if (!textures.containsKey(lightTex)) {
            BufferedImage image;
            try {
                image = ImageIO.read(lightTex.getLastResourceStream());
            } catch (IOException e) {
                throw new RuntimeException(lightTex.toString(), e);
            }
            int[] texData = ImageUtils.toRGBA(image);
            int texId = OpenGL.allocateTexture();
            try (OpenGL.With tex = OpenGL.texture(texId)) {
                int width = image.getWidth();
                int height = image.getHeight();
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

                ByteBuffer buffer = ByteBuffer.allocateDirect(texData.length * Integer.BYTES);
                buffer.asIntBuffer().put(texData);

                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
            }
            textures.put(lightTex, texId);
        }
        boolean reverse = stock.getCurrentSpeed().minecraft() < 0;
        float red = reverse ? this.redReverse : this.red;
        float green = reverse ? this.greenReverse : this.green;
        float blue = reverse ? this.blueReverse : this.blue;

        if (red == 0 && green == 0 && blue == 0) {
            return;
        }

        if (isBlinkOff(stock)) {
            return;
        }

        Vec3d flareOffset = new Vec3d(forward ? component.min.x - 0.01 : component.max.x + 0.01, (component.min.y + component.max.y) / 2, (component.min.z + component.max.z) / 2).scale(stock.gauge.scale());
        if (location != null) {
            // TODO this does not actually work
            flareOffset = location.apply(stock).apply(flareOffset);
        }

        Vec3d playerOffset = VecUtil.rotateWrongYaw(stock.getPosition().subtract(MinecraftClient.getPlayer().getPosition()), 180 - (stock.getRotationYaw())).
                subtract(flareOffset).scale(forward ? 1 : -1);

        int viewAngle = 45;
        float intensity = 1 - Math.abs(Math.max(-viewAngle, Math.min(viewAngle, VecUtil.toWrongYaw(playerOffset) - 90))) / viewAngle;
        intensity *= Math.abs(playerOffset.x/(50 * stock.gauge.scale()));
        intensity = Math.min(intensity, 1.5f);

        try (
                OpenGL.With tex = OpenGL.texture(textures.get(lightTex));
                OpenGL.With light = OpenGL.shaderActive() ?
                        OpenGL.lightmap(1, 1) :
                        OpenGL.bool(GL11.GL_LIGHTING, false).and(OpenGL.lightmap(false));
                OpenGL.With depth = OpenGL.depth(false);
                OpenGL.With alpha = OpenGL.bool(GL11.GL_ALPHA_TEST, false);
                OpenGL.With blend = OpenGL.blend(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)) {

            if (intensity > 0.01) {
                try (OpenGL.With matrix = OpenGL.matrix()) {
                    GL11.glTranslated(flareOffset.x - (intensity / 2 * stock.gauge.scale())*(forward ? 3 : -3), flareOffset.y, flareOffset.z);
                    GL11.glRotated(90, 0, 1, 0);
                    double scale = Math.max((component.max.z - component.min.z) * 0.5, intensity * 2) * stock.gauge.scale();
                    GL11.glColor4f(red, green, blue, 1 - (intensity/3f));
                    GL11.glScaled(scale, scale, scale);
                    if (!forward) {
                        GL11.glRotated(180, 0, 1, 0);
                    }

                    GL11.glBegin(GL11.GL_QUADS);
                    GL11.glTexCoord2d(0, 0);
                    GL11.glVertex3d(-1, -1, 0);
                    GL11.glTexCoord2d(0, 1);
                    GL11.glVertex3d(-1, 1, 0);
                    GL11.glTexCoord2d(1, 1);
                    GL11.glVertex3d(1, 1, 0);
                    GL11.glTexCoord2d(1, 0);
                    GL11.glVertex3d(1, -1, 0);
                    GL11.glEnd();
                }
            }
            try (OpenGL.With matrix = OpenGL.matrix()) {
                GL11.glTranslated(flareOffset.x, flareOffset.y, flareOffset.z);
                GL11.glRotated(90, 0, 1, 0);
                if (location != null) {
                    // TODO: This is a shitty hack...
                    // I'm tired and don't want to fix the headlight code properly at this point
                    Matrix4 m = location.apply(stock).copy();
                    m.invert();
                    OpenGL.multMatrix(m);
                }
                GL11.glColor4d(Math.sqrt(red), Math.sqrt(green), Math.sqrt(blue), 1 - (intensity/3f));
                double scale = (component.max.z - component.min.z) / 1.5 * stock.gauge.scale();
                GL11.glScaled(scale, scale, scale);
                if (!forward) {
                    GL11.glRotated(180, 0, 1, 0);
                }

                GL11.glBegin(GL11.GL_QUADS);
                GL11.glTexCoord2d(0, 0);
                GL11.glVertex3d(-1, -1, 0);
                GL11.glTexCoord2d(0, 1);
                GL11.glVertex3d(-1, 1, 0);
                GL11.glTexCoord2d(1, 1);
                GL11.glVertex3d(1, 1, 0);
                GL11.glTexCoord2d(1, 0);
                GL11.glVertex3d(1, -1, 0);
                GL11.glEnd();
            }
        }
    }

    public void effects(T stock) {
        if (!castsLights) {
            return;
        }

        if (!Light.enabled() || isBlinkOff(stock)) {
            this.removed(stock);
            return;
        }

        int lightDistance = (int) (15 * stock.gauge.scale());
        if (!castLights.containsKey(stock.getUUID())) {
            Vec3d flareOffset = new Vec3d(-component.min.x, (component.min.y + component.max.y) / 2, (component.min.z + component.max.z) / 2).scale(stock.gauge.scale());

            castLights.put(stock.getUUID(), new ArrayList<>());
            castPositions.put(stock.getUUID(), new ArrayList<>());
            for (int i = 0; i < lightDistance; i++) {
                for (int j = 0; j < 5; j++) {
                    castLights.get(stock.getUUID()).add(new Light(stock.getWorld(), stock.getPosition(), 1 - i / (float)lightDistance));
                }
                double xOff = 4;
                double yOff = -(i / (float) lightDistance) * flareOffset.y;
                int sign = forward ? 1 : -1;
                castPositions.get(stock.getUUID()).add(flareOffset.add((i * 2 + xOff) * sign, 0+yOff, 0));
                castPositions.get(stock.getUUID()).add(flareOffset.add((i * 2 + xOff) * sign, i/2f+yOff, 0));
                castPositions.get(stock.getUUID()).add(flareOffset.add((i * 2 + xOff) * sign, -i/2f+yOff, 0));
                castPositions.get(stock.getUUID()).add(flareOffset.add((i * 2 + xOff) * sign, 0+yOff, i/2f));
                castPositions.get(stock.getUUID()).add(flareOffset.add((i * 2 + xOff) * sign, 0+yOff, -i/2f));
            }
        }
        Vec3d[] collided = new Vec3d[5];
        Vec3d nop = null;
        for (int i = 0; i < castLights.get(stock.getUUID()).size(); i++) {
            if (collided[i%5] != null) {
                castLights.get(stock.getUUID()).get(i).setPosition(collided[i%5]);
            } else {
                Vec3d cpos = castPositions.get(stock.getUUID()).get(i);
                if (location != null) {
                    cpos = location.apply(stock).apply(cpos);
                }
                Vec3d pos = stock.getPosition().add(VecUtil.rotateWrongYaw(cpos, stock.getRotationYaw()));
                if (nop == null) {
                    nop = pos;
                }
                if (!stock.getWorld().isReplaceable(new Vec3i(pos).up())) {
                    collided[i%5] = nop;
                    castLights.get(stock.getUUID()).get(i).setPosition(nop);
                } else {
                    castLights.get(stock.getUUID()).get(i).setPosition(pos);
                }
            }
        }
    }

    public <T extends EntityMoveableRollingStock> void removed(T stock) {
        if (castLights.containsKey(stock.getUUID())) {
            castLights.get(stock.getUUID()).forEach(Light::remove);
            castLights.remove(stock.getUUID());
            castPositions.remove(stock.getUUID());
        }
    }
}
