package cz.tacr.elza.api;

/**
 * Atribut pro výstupy.
 *
 * @author Martin Šlapa
 * @since 20.06.2016
 */
public interface ArrOutputItem<OD extends ArrOutputDefinition> {
    void setOutputDefinition(OD output);

    OD getOutputDefinition();
}
