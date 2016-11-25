package cz.tacr.elza.controller.vo;

import java.util.LinkedList;
import java.util.List;


/**
 * Vztah osoby.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 23.12.2015
 */
public class ParRelationVO {

    private Integer id;

    private Integer relationTypeId;

    private ParUnitdateVO from;

    private ParUnitdateVO to;

    private String note;

    private Integer partyId;

    private Integer version;

    private String displayName;

    private String source;


    private List<ParRelationEntityVO> relationEntities;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getRelationTypeId() {
        return relationTypeId;
    }

    public void setRelationTypeId(final Integer relationTypeId) {
        this.relationTypeId = relationTypeId;
    }

    public ParUnitdateVO getFrom() {
        return from;
    }

    public void setFrom(final ParUnitdateVO from) {
        this.from = from;
    }

    public ParUnitdateVO getTo() {
        return to;
    }

    public void setTo(final ParUnitdateVO to) {
        this.to = to;
    }

    public String getNote() {
        return note;
    }

    public void setNote(final String note) {
        this.note = note;
    }


    public Integer getPartyId() {
        return partyId;
    }

    public void setPartyId(final Integer partyId) {
        this.partyId = partyId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(final String source) {
        this.source = source;
    }

    public List<ParRelationEntityVO> getRelationEntities() {
        return relationEntities;
    }

    public void setRelationEntities(final List<ParRelationEntityVO> relationEntities) {
        this.relationEntities = relationEntities;
    }

    public void addRelationEntity(final ParRelationEntityVO relationEntity) {
        if (relationEntities == null) {
            relationEntities = new LinkedList<>();
        }
        relationEntities.add(relationEntity);
    }


}
