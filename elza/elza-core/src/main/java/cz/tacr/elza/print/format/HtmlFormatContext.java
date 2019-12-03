package cz.tacr.elza.print.format;

/*
 * import org.commonmark.node.Node;
 * import org.commonmark.parser.Parser;
 * import org.commonmark.renderer.html.HtmlRenderer;
 */

// Not yet finished
// Bude vhodnejsi implementovat vlastni zpusob parsovani
public class HtmlFormatContext extends FormatContext {
    /*
    @Override
    public String getResult() {
        String textResult = super.getResult();
    
        // convert to HTML
        Parser parser = Parser.builder().build();
        Node document = parser.parse(textResult);
        HtmlRenderer renderer = HtmlRenderer.builder().escapeHtml(true).softbreak("<br>").build();
        return renderer.render(document);  // "<p>This is <em>Sparta</em></p>\n"
    
    }
    */
}
