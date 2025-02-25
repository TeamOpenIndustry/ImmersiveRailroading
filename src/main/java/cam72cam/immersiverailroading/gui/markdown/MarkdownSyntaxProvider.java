package cam72cam.immersiverailroading.gui.markdown;

import java.util.List;

public abstract class MarkdownSyntaxProvider {
    public MarkdownSyntaxProvider(String str){
        MarkdownBuilder.register(str, this::parse);
    }

    public abstract List<MarkdownDocument.MarkdownLine> parse(String lines);
}
