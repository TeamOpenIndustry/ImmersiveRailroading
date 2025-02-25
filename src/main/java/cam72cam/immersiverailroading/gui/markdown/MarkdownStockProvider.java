package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.immersiverailroading.registry.DefinitionManager;

import java.util.List;
import java.util.stream.Collectors;

public class MarkdownStockProvider extends MarkdownSyntaxProvider{
    public MarkdownStockProvider() {
        super("[list_stocks]");
    }

    @Override
    public List<MarkdownDocument.MarkdownLine> parse(String str){
            return DefinitionManager.getDefinitionNames().stream()
                    .map(string -> new MarkdownDocument.MarkdownLine(new MarkdownStyledText(string)))
                    .collect(Collectors.toList());
    }
}
