package cam72cam.immersiverailroading.gui.markdown;

import java.io.BufferedReader;
import java.io.IOException;

//TODO Proxy rendering part

/**
 * A proxy class to simplify main builder class
 */
public class MarkdownCodeBlock {
    private static final int CODE_COLOR = 0xFFDDDDDD;
    private static final int BLACK = 0xFF000000;

    public static void parse(BufferedReader reader, MarkdownDocument document, String firstLine) throws IOException {
        if(firstLine.length() > 3){
            //Meaning it has language mark
            document.addLine(new MarkdownDocument.MarkdownLine(new MarkdownStyledText(firstLine.substring(3)))
                    .isCodeBlockStart(true));
        } else {
            //Language is empty
            document.addLine(new MarkdownDocument.MarkdownLine(new MarkdownStyledText(""))
                    .isCodeBlockStart(true));
        }
        String str;
        while ((str = reader.readLine()) != null){
            if(str.startsWith("```")){
                document.addLine(new MarkdownDocument.MarkdownLine(new MarkdownStyledText("")).isCodeBlockEnd(true));
                return;
            }
            document.addLine(MarkdownBuilder.parse(str));
        }
        document.addLine(new MarkdownDocument.MarkdownLine(new MarkdownStyledText("")).isCodeBlockEnd(true));
    }
}
