package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.resource.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class MarkdownPicture extends MarkdownElement{
    public Identifier picture;

    public double ratio;
    public BufferedImage image;

    static {
        splittable = false;
    }

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
}
