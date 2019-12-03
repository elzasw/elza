package cz.tacr.elza.print.format;

public class HtmlFormatter extends Formatter {

    @Override
    protected FormatContext createFormatCtx() {
        return new HtmlFormatContext();
    }

}
