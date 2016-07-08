package cz.tacr.elza.bulkaction.generator.result;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementace {@link cz.tacr.elza.api.vo.result.Result}
 *
 * @author Martin Šlapa
 * @since 30.06.2016
 */
public class Result implements cz.tacr.elza.api.vo.result.Result<ActionResult> {

    private List<ActionResult> results = new ArrayList<>();

    @Override
    public List<ActionResult> getResults() {
        return results;
    }

    @Override
    public void setResults(final List<ActionResult> results) {
        this.results = results;
    }
}
