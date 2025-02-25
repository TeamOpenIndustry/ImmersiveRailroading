package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.immersiverailroading.registry.DefinitionManager;

import java.util.List;
import java.util.stream.Collectors;

public class MarkdownStockProvider {
    public static final String PREFIX = "[Stock]";

    public static List<MarkdownDocument.MarkdownLine> getLines(String str){
//        if(str.length() <= 7){
//            return Collections.emptyList();
//        } else {
            return DefinitionManager.getDefinitionNames().stream()
                    .map(string -> new MarkdownDocument.MarkdownLine(new MarkdownStyledText(string)))
                    .collect(Collectors.toList());
//        }
    }
}
