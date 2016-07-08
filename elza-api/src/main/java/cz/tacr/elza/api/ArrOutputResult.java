package cz.tacr.elza.api;

import java.util.List;

/**
 * Soubor v Output
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 13.3.2016
 */
public interface ArrOutputResult<D extends ArrOutputDefinition, T extends RulTemplate, C extends ArrChange, F extends ArrOutputFile> {

    Integer getOutputResultId();

    void setOutputResultId(Integer outputResultId);

    C getChange();

    void setChange(C change);

    T getTemplate();

    void setTemplate(T template);

    D getOutputDefinition();

    void setOutputDefinition(D outputDefinition);

    void setOutputFiles(List<F> outputResult);

    List<F> getOutputFiles();
}
