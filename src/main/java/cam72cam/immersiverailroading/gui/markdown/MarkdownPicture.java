package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.render.opengl.DirectDraw;
import cam72cam.mod.resource.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

//TODO fix me!
public class MarkdownPicture extends MarkdownElement{
    public Identifier picture;

    public double ratio;
    public BufferedImage image;
    public DirectDraw directDraw;

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
        this.directDraw = new DirectDraw();
        this.directDraw.vertex(0,0,0).uv(0,0);
        this.directDraw.vertex(0,ratio,0).uv(0,1);
        this.directDraw.vertex(1,ratio,0).uv(1,1);
        this.directDraw.vertex(1,0,0).uv(1,0);
    }

    @Override
    public String apply() {
        return "";
    }

    @Override
    public MarkdownElement[] split(int splitPos) {
        return null;
    }
}
