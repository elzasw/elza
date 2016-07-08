package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * //TODO marik missing comment
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParRelation<PP extends ParParty, PRT extends ParRelationType, PU extends ParUnitdate>
        extends Versionable, Serializable {

    /**
     * Primární ID.
     * @return      id objektu
     */
    Integer getRelationId();

    void setRelationId(Integer relationId);

    PP getParty();

    void setParty(PP party);

    PRT getComplementType();

    void setComplementType(PRT complementType);

    PU getFrom();

    void setFrom(PU from);

    PU getTo();

    void setTo(PU to);

    String getDateNote();

    void setDateNote(String dateNote);

    String getNote();

    void setNote(String note);

    String getSource();

    void setSource(String source);
}
