package cz.tacr.elza.controller.vo.filter;

/**
 * Hledání v textu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 16. 1. 2017
 */
public class TextSearchParam extends SearchParam {

    public TextSearchParam() {
    }

    public TextSearchParam(final String value) {
        super(SearchParamType.TEXT, value);
    }
}
