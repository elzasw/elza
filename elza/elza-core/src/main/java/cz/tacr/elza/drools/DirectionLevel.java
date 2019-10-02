package cz.tacr.elza.drools;

/**
 * Směr založení levelu.
 *
 * @author Martin Šlapa
 * @since 9.12.2015
 */
public enum DirectionLevel {

    /**
     * Jako potomek
     */
    CHILD,

    /**
     * Před level
     */
    BEFORE,

    /**
     * Za level
     */
    AFTER,

    /**
     * Jako root level
     */
    ROOT

}
