package cz.tacr.elza.api;

import java.io.Serializable;
import java.util.List;

/**
 * //TODO marik missing comment
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParRelation<PP extends ParParty, PRT extends ParRelationType, PU extends ParUnitdate, PRE extends ParRelationEntity>
        extends Versionable, Serializable {

    /**
     * Primární ID.
     * @return      id objektu
     */
    Integer getRelationId();

    void setRelationId(Integer relationId);

    PP getParty();

    void setParty(PP party);

    PRT getRelationType();

    void setRelationType(PRT relationType);

    PU getFrom();

    void setFrom(PU from);

    PU getTo();

    void setTo(PU to);

    String getNote();

    void setNote(String note);

    String getSource();

    void setSource(String source);

    List<PRE> getRelationEntities();

    void setRelationEntities(List<PRE> relations);
}
