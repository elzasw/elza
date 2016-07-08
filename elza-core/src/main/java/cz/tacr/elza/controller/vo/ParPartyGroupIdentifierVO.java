package cz.tacr.elza.controller.vo;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.springframework.data.rest.core.annotation.RestResource;

import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParUnitdate;


/**
 * Identifikace o přiřazených kódech původce, například IČO.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 23.12.2015
 */
public class ParPartyGroupIdentifierVO {
    private Integer partyGroupIdentifierId;

    private ParUnitdateVO to;

    private ParUnitdateVO from;

    private Integer partyId;

    private String source;

    private String note;

    private String identifier;

    public Integer getPartyGroupIdentifierId() {
        return partyGroupIdentifierId;
    }

    public void setPartyGroupIdentifierId(final Integer partyGroupIdentifierId) {
        this.partyGroupIdentifierId = partyGroupIdentifierId;
    }

    public ParUnitdateVO getTo() {
        return to;
    }

    public void setTo(final ParUnitdateVO to) {
        this.to = to;
    }

    public ParUnitdateVO getFrom() {
        return from;
    }

    public void setFrom(final ParUnitdateVO from) {
        this.from = from;
    }

    public Integer getPartyId() {
        return partyId;
    }

    public void setPartyId(final Integer partyId) {
        this.partyId = partyId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(final String source) {
        this.source = source;
    }

    public String getNote() {
        return note;
    }

    public void setNote(final String note) {
        this.note = note;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(final String identifier) {
        this.identifier = identifier;
    }
}
