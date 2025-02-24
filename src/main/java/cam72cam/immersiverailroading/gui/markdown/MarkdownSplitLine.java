package cam72cam.immersiverailroading.gui.markdown;

public class MarkdownSplitLine extends MarkdownElement{

    private static final char[] allowed = new char[]{'*', '-', '_'};

    public static boolean validate(String str){
        if(str.length() < 3){
            return false;
        }
        for(char c : allowed){
            label: {
                for (int i = 0; i < str.length(); i++) {
                    if (str.charAt(i) != c) {
                        break label;
                    }
                }
                return true;
            }
        }
        return false;
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
