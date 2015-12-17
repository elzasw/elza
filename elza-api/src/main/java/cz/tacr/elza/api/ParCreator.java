package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * //TODO marik missing comment
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParCreator<PP extends ParParty> extends Serializable {

    /**
     * Vlastní ID.
     * @return id
     */
    Integer getCreatorId();

    void setCreatorId(Integer creatorId);

    /**
     * Vazba na osobu.
     * @return osoba
     */
    PP getParty();

    /**
     * Vazba na osobu.
     * @param party osoba
     */
    void setParty(PP party);

    PP getCreatorParty();

    void setCreatorParty(PP creatorParty);
}
