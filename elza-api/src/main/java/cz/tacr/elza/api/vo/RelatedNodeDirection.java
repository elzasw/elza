package cz.tacr.elza.api.vo;

/**
 * Směry, kterými lze prohledávat strom.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.11.2015
 */
public enum RelatedNodeDirection {
    /**
     * Samotný uzel.
     */
    NODE,
    /**
     * Všechny přímé rodiče.
     */
    PARENTS,
    /**
     * Všechny předky.
     */
    ASCENDATNS,
    /**
     * Všechny příme potomky.
     */
    CHILDREN,
    /**
     * Všechny potomky.
     */
    DESCENDANTS,
    /**
     * Všechny sourozence.
     */
    SIBLINGS,
    /**
     * Všechny uzly ve verzi.
     */
    ALL;
}
