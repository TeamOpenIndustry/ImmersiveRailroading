package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.text.TextColor;
import util.Matrix4;

/*
  Will scale by level
  1 -> scale to 1.5x
  2 -> scale to 1.2x
  3+ -> just like regular text
  CANNOT CONTAIN URL
 */
public class MarkdownHeader extends MarkdownElement{
    //Starting from 1
    public int level;

    public static final double LEVEL1 = 1/1.35;
    public static final double LEVEL2 = 1/1.15;


    static {
        splittable = false;
    }

    public MarkdownHeader(String text) {
        label: {
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) != '#') {
                    this.level = i;
                    this.text = text.substring(i).trim();
                    break label;
                }
            }
            this.text = text;
            this.level = -1;
        }
    }

    public MarkdownHeader(String text, int level) {
        this.text = text;
        this.level = level;
    }

    public boolean render(Matrix4 transform){
        Vec3d offset = transform.apply(Vec3d.ZERO);
        if(this.level == 1){
            transform.translate(-offset.x, -offset.y, 0);
            transform.scale(1.35, 1.35, 1.35);
            transform.translate(offset.x * MarkdownHeader.LEVEL1, offset.y * MarkdownHeader.LEVEL1, 0);
            String str = this.apply();
            GUIHelpers.drawString(str, 0, 0, 0xFF000000, transform);

            transform.translate(-offset.x * MarkdownHeader.LEVEL1, -offset.y * MarkdownHeader.LEVEL1, 0);
            transform.scale(MarkdownHeader.LEVEL1, MarkdownHeader.LEVEL1, MarkdownHeader.LEVEL1);
            transform.translate(offset.x, offset.y, 0);
            transform.translate(0, 4, 0);
            return true;
        } else if(this.level == 2){
            transform.translate(-offset.x, -offset.y, 0);
            transform.scale(1.15, 1.15, 1.15);
            transform.translate(offset.x * MarkdownHeader.LEVEL2, offset.y * MarkdownHeader.LEVEL2, 0);
            String str = this.apply();
            GUIHelpers.drawString(str, 0, 0, 0xFF000000, transform);

            transform.translate(-offset.x * MarkdownHeader.LEVEL2, -offset.y * MarkdownHeader.LEVEL2, 0);
            transform.scale(MarkdownHeader.LEVEL2, MarkdownHeader.LEVEL2, MarkdownHeader.LEVEL2);
            transform.translate(offset.x, offset.y, 0);
            transform.translate(0, 2, 0);
            return true;
        }
        return false;
    }

    @Override
    public String apply() {
        if(level == -1){//Invalid
            return "";
        } else {
            return TextColor.ITALIC.wrap(text);
        }
    }

    @Override
    public MarkdownElement[] split(int splitPos) {
        int i = splitPos;
        while (this.text.charAt(i) == ' '){
            i++;
            if(i == this.text.length()){//rest are all space
                return new MarkdownElement[]{
                        new MarkdownHeader(this.text.substring(0, splitPos), this.level),
                        new MarkdownHeader("", this.level)};
            }
        }
        return new MarkdownElement[]{
                new MarkdownHeader(this.text.substring(0, splitPos), this.level),
                new MarkdownHeader(this.text.substring(0, splitPos), this.level)};
    }
}
