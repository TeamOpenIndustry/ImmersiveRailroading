package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.text.TextColor;

//Start with #
public class MarkdownHeader extends MarkdownElement{
    //Starting from 1
    public int level;

    //It may contain a url
    public MarkdownUrl url;

    static {
        splittable = false;
    }

    public MarkdownHeader(String text) {
        label: {
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) != '#') {
                    this.level = i;
                    this.text = text.substring(i);
                    break label;
                }
            }
            this.text = text;
            this.level = -1;
        }

        this.url = MarkdownUrl.compileSingle(this.text);
    }

    @Override
    public String apply() {
        if(this.url != null){
            return this.url.apply();
        }
        if(level == -1){//Invalid
            return "";
        }else if(level <= 2){
            return TextColor.BOLD.wrap(text);
        } else {
            return text;
        }
    }

    @Override
    public MarkdownElement[] split(int splitPos) {
        //Unsplitable
        return null;
    }
}
