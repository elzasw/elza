package cz.tacr.elza.drools.model;

/**
 * Určuje u nově vytvářeného levelu kdo ho vytváří.
 *
 * @author Martin Šlapa
 * @since 23.12.2015
 */
public enum EventSource {

    PARENT,
    SIBLING_BEFORE,
    SIBLING_AFTER

}
