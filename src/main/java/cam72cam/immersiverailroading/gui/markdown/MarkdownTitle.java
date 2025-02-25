package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.text.TextColor;
import util.Matrix4;

/*
  Will scale by level
  1 -> scale to 1.35x and italicize
  2 -> scale to 1.15x and italicize
  3+ -> To regular text, don't handle
  CANNOT CONTAIN URL
 */
public class MarkdownTitle extends MarkdownElement {
    //Starting from 1
    public int level;

    public static final double LEVEL1 = 1/1.35;
    public static final double LEVEL2 = 1/1.15;

    public MarkdownTitle(String text) {
        label: {
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) != '#') {
                    this.level = i;
                    this.text = text.substring(i).trim();
                    break label;
                }
            }
            //All the chars are '#'
            this.text = text;
            this.level = -1;
        }
    }

    public MarkdownTitle(String text, int level) {
        this.text = text;
        this.level = level;
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
                        new MarkdownTitle(this.text.substring(0, splitPos), this.level),
                        new MarkdownTitle("", this.level)};
            }
        }
        return new MarkdownElement[]{
                new MarkdownTitle(this.text.substring(0, splitPos), this.level),
                new MarkdownTitle(this.text.substring(0, splitPos), this.level)};
    }

    @Override
    public int render(Matrix4 transform, int pageWidth){
        Vec3d offset = transform.apply(Vec3d.ZERO);
        String str = this.apply();
        if(this.level == 1){
            //Scale matrix
            transform.translate(-offset.x, -offset.y, 0);
            transform.scale(1.35, 1.35, 1.35);
            transform.translate(offset.x * MarkdownTitle.LEVEL1, offset.y * MarkdownTitle.LEVEL1, 0);
            GUIHelpers.drawString(str, 0, 0, 0xFF000000, transform);

            //Revert matrix
            transform.translate(-offset.x * MarkdownTitle.LEVEL1, -offset.y * MarkdownTitle.LEVEL1, 0);
            transform.scale(MarkdownTitle.LEVEL1, MarkdownTitle.LEVEL1, MarkdownTitle.LEVEL1);
            transform.translate(offset.x, offset.y, 0);

            //Move down(4 == 14 - 10)
            transform.translate(0, 4, 0);
            return 4;
        } else if(this.level == 2){
            //Scale matrix
            transform.translate(-offset.x, -offset.y, 0);
            transform.scale(1.15, 1.15, 1.15);
            transform.translate(offset.x * MarkdownTitle.LEVEL2, offset.y * MarkdownTitle.LEVEL2, 0);
            GUIHelpers.drawString(str, 0, 0, 0xFF000000, transform);

            //Revert matrix
            transform.translate(-offset.x * MarkdownTitle.LEVEL2, -offset.y * MarkdownTitle.LEVEL2, 0);
            transform.scale(MarkdownTitle.LEVEL2, MarkdownTitle.LEVEL2, MarkdownTitle.LEVEL2);
            transform.translate(offset.x, offset.y, 0);
            //Move down(2 == 12 - 10)
            transform.translate(0, 2, 0);
            return 2;
        } else {
            GUIHelpers.drawString(str, 0, 0, 0xFF000000, transform);
            return 0;
        }
    }
}
