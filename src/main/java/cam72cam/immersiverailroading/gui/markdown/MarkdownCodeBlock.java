package cam72cam.immersiverailroading.gui.markdown;

import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.math.Vec3d;
import util.Matrix4;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;

import static cam72cam.immersiverailroading.gui.markdown.Colors.CODE_BACKGROUND_COLOR;
import static cam72cam.immersiverailroading.gui.markdown.Colors.DEFAULT_TEXT_COLOR;

/**
 * Proxy class to simplify MarkdownBuilder's build logic and MarkdownDocument's render logic
 * @see MarkdownBuilder
 * @see MarkdownDocument
 */
public class MarkdownCodeBlock {
    /**
     * Parse code block into given MarkdownDocument
     * @param reader Markdown file's reader
     * @param document Given document
     * @param firstLine The line contains beginning syntax
     * @throws IOException If internal I/O error occurs
     */
    public static void parse(BufferedReader reader, MarkdownDocument document, String firstLine) throws IOException {
        if(firstLine.length() > 3){
            //Meaning it has language mark
            document.addLine(MarkdownDocument.MarkdownLine.create(new MarkdownStyledText(firstLine.substring(3)))
                    .isCodeBlockStart(true));
        } else {
            //Language is empty
            document.addLine(MarkdownDocument.MarkdownLine.create(new MarkdownStyledText(""))
                    .isCodeBlockStart(true));
        }
        String str;
        while ((str = reader.readLine()) != null){
            if(str.startsWith("```")){
                document.addLine(MarkdownDocument.MarkdownLine.create(new MarkdownStyledText("")).isCodeBlockEnd(true));
                return;
            }
            document.addLine(MarkdownBuilder.parse(str));
        }
        document.addLine(MarkdownDocument.MarkdownLine.create(new MarkdownStyledText("")).isCodeBlockEnd(true));
    }

    /**
     * Render given document's code block element
     * @param matrix4 Transform matrix
     * @param iterator Lines iterator
     * @param document Source document
     * @param currentLine Current iterated line which can't be gotten by iterator.next()
     * @return The code block's height fo further use
     */
    public static int render(Matrix4 matrix4, Iterator<MarkdownDocument.MarkdownLine> iterator,
                             MarkdownDocument document, MarkdownDocument.MarkdownLine currentLine){
        Vec3d offset = matrix4.apply(Vec3d.ZERO);
        int height = 0;
        //Draw header line
        //Code blocks have a gray background and start with a language specification mark
        GUIHelpers.drawRect((int) offset.x, (int) offset.y,
                document.getPageWidth(), 10, CODE_BACKGROUND_COLOR);
        int delta = document.getPageWidth() - GUIHelpers.getTextWidth(currentLine.getElements().get(0).apply());
        matrix4.translate(delta, 0, 0);
        GUIHelpers.drawString(currentLine.getElements().get(0).apply(), 0, 0, DEFAULT_TEXT_COLOR, matrix4);
        matrix4.translate(-delta, 10, 0);
        height += 10;

        while (iterator.hasNext()){
            MarkdownDocument.MarkdownLine line = iterator.next();
            if(line.codeBlockEnd){
                //Draw footer line and language specification mark
                GUIHelpers.drawRect((int) offset.x, (int) offset.y,
                        document.getPageWidth(), 5, CODE_BACKGROUND_COLOR);
                matrix4.translate(0, 5, 0);
                height += 5;
                return height;
            }
            //Otherwise draw content
            GUIHelpers.drawRect((int) offset.x , (int) offset.y ,
                    document.getPageWidth(), 10, CODE_BACKGROUND_COLOR);
            GUIHelpers.drawString(line.getElements().get(0).apply(), 0, 0, DEFAULT_TEXT_COLOR, matrix4);
            matrix4.translate(0, 10, 0);
            height += 10;
        }

        //Why does a file ends and leaves a code block open? This should never happen!
        throw new IllegalStateException();
    }
}
