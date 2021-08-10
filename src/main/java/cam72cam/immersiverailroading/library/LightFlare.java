package cam72cam.immersiverailroading.library;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.model.ComponentRenderer;
import cam72cam.immersiverailroading.model.components.ComponentProvider;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.ImageUtils;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.resource.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LightFlare {
    private final ModelComponent component;
    private final boolean forward;
    private static final Map<Identifier, Integer> textures = new HashMap<>();
    private final float red;
    private final float green;
    private final float blue;

    private static final Pattern rgb = Pattern.compile(String.format(".*_0X(%s%<s)(%<s%<s)(%<s%<s).*", "[0-9A-Fa-f]"));

    public static List<LightFlare> get(ComponentProvider provider, ModelComponentType type) {
        return provider.parseAll(type).stream().map(LightFlare::new).collect(Collectors.toList());
    }

    public LightFlare(ModelComponent component) {
        this.component = component;
        this.forward = component.center.x > 0;  // Is this right?
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
    }

    public void render(ComponentRenderer draw) {
        draw.render(component);
    }

    public void postRender(EntityMoveableRollingStock stock) {
        if (!textures.containsKey(stock.getDefinition().light_tex)) {
            BufferedImage image = null;
            try {
                image = ImageIO.read(stock.getDefinition().light_tex.getLastResourceStream());
            } catch (IOException e) {
                throw new RuntimeException(stock.getDefinition().light_tex.toString(), e);
            }
            int[] texData = ImageUtils.toRGBA(image);
            int texId = GL11.glGenTextures();
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
            textures.put(stock.getDefinition().light_tex, texId);
        }
        Vec3d flareOffset = new Vec3d(component.min.x-0.01, (component.min.y + component.max.y) / 2, (component.min.z + component.max.z) / 2).scale(stock.gauge.scale());

        Vec3d playerOffset = VecUtil.rotateWrongYaw(stock.getPosition().subtract(MinecraftClient.getPlayer().getPosition()), 180-stock.getRotationYaw()).
                subtract(flareOffset);

        int viewAngle = 45;
        float intensity = 1 - Math.abs(Math.max(-viewAngle, Math.min(viewAngle, VecUtil.toWrongYaw(playerOffset) - 90))) / viewAngle;
        intensity *= Math.abs(playerOffset.x/(50 * stock.gauge.scale()));
        intensity = Math.min(intensity, 1.5f);

        try (
                OpenGL.With tex = OpenGL.texture(textures.get(stock.getDefinition().light_tex));
                OpenGL.With light = OpenGL.bool(GL11.GL_LIGHTING, false);
                OpenGL.With shader = OpenGL.shader(0);
                OpenGL.With lightMap = OpenGL.lightmap(false);
                OpenGL.With depth = OpenGL.depth(false);
                OpenGL.With alpha = OpenGL.bool(GL11.GL_ALPHA_TEST, false);
                OpenGL.With blend = OpenGL.blend(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)) {

            if (intensity > 0.1) {
                try (OpenGL.With matrix = OpenGL.matrix()) {
                    GL11.glTranslated(flareOffset.x - (intensity / 2 * stock.gauge.scale()), flareOffset.y, flareOffset.z);
                    GL11.glRotated(90, 0, 1, 0);
                    double scale = Math.max((component.max.z - component.min.z) * 0.5, intensity * 2) * stock.gauge.scale();
                    GL11.glColor4f(red, green, blue, 1 - (intensity/3f));
                    GL11.glScaled(scale, scale, scale);

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
                GL11.glColor4d(Math.sqrt(red), Math.sqrt(green), Math.sqrt(blue), 1 - (intensity/3f));
                double scale = (component.max.z - component.min.z) / 1.5 * stock.gauge.scale();
                GL11.glScaled(scale, scale, scale);

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
}