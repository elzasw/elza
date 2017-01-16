package cz.tacr.elza.domain.vo;

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
    PARENT,
    /**
     * Všechny předky.
     */
    ASCENDANTS,
    /**
     * Všechny příme potomky.
     */
    CHILDREN,
    /**
     * Všechny potomky.
     */
    DESCENDANTS,
    /**
     * Sourozence - před a za.
     */
    SIBLINGS,
    /**
     * Všechny sourozence.
     */
    ALL_SIBLINGS,
    /**
     * Všechny uzly ve verzi.
     */
    ALL;
}
