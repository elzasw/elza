package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Souhrn fyzických osob spojených příbuzenskou vazbou.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParDynasty extends Serializable {

    String getGenealogy();

    void setGenealogy(String genealogy);
}
