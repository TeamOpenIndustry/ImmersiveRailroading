package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.resource.Identifier;
import util.Matrix4;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class MarkdownPicture extends MarkdownElement {
    public Identifier picture;

    public double ratio;
    public BufferedImage image;

    public MarkdownPicture(Identifier picture) {
        this.picture = picture;
        try {
            this.image = ImageIO.read(picture.getResourceStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.ratio = (double) this.image.getHeight() / this.image.getWidth();
    }

    @Override
    public String apply() {
        return "";
    }

    @Override
    public MarkdownElement[] split(int splitPos) {
        return new MarkdownElement[0];
    }

    @Override
    public int render(Matrix4 transform, int pageWidth) {
        Vec3d offset = transform.apply(Vec3d.ZERO);
        int picHeight = (int) (pageWidth * this.ratio);
        GUIHelpers.texturedRect(this.picture, (int) offset.x, (int) offset.y, pageWidth, picHeight);
        transform.translate(0, picHeight, 0);
        return picHeight - 10;
    }
}
