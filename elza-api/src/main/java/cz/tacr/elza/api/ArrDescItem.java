package cz.tacr.elza.api;

import java.io.Serializable;



/**
 * Atribut archivního popisu evidovaný k jednotce archivního popisu. Odkaz na uzel stromu AP je
 * řešen pomocí node_id.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
public interface ArrDescItem<N extends ArrNode> extends Serializable {


    /**
     * @return nod.
     */
    N getNode();

    /**
     * Nastaví nod.
     *
     * @param node nod.
     */
    void setNode(N node);


}
