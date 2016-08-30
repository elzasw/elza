package cz.tacr.elza.print;

/**
 * Interface pro získání iterátorů pro průchod do šířky a do hloubky.
 *
 * @author Martin Šlapa
 * @since 29.08.2016
 */
public interface NodesOrder {

    /**
     * @return instance iterátoru, který prochází jednotky popisu do hloubky
     */
    IteratorNodes getNodesDFS();

    /**
     * @return instance iterátoru, který prochází jednotky popisu do šířky
     */
    IteratorNodes getNodesBFS();

}
