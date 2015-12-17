package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;


/**
 * Abstraktní osoby.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 * @since 21.8.2015
 */
@Entity(name = "par_party")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParParty extends AbstractVersionableEntity implements cz.tacr.elza.api.ParParty<RegRecord, ParPartyType, ParPartyName> {

    /* Konstanty pro vazby a fieldy. */
    public static final String ABSTRACT_PARTY_ID = "partyId";
    public static final String RECORD = "record";
    public static final String PARTY_TYPE = "partyType";
    public static final String PARTY_PREFERRED_NAME = "preferredName";
    public static final String HISTORY = "history";
    public static final String SOURCE_INFORMATION = "sourceInformation";

    @Id
    @GeneratedValue
    private Integer partyId;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.LAZY, targetEntity = RegRecord.class)
    @JoinColumn(name = "recordId", nullable = false)
    private RegRecord record;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParPartyType.class)
    @JoinColumn(name = "partyTypeId", nullable = false)
    private ParPartyType partyType;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParPartyName.class)
    @JoinColumn(name = "preferredNameId")
    private ParPartyName preferredName;

    @Column()
    private String history;

    @Column()
    private String sourceInformation;


    @Override
    public Integer getPartyId() {
        return partyId;
    }

    @Override
    public void setPartyId(final Integer partyId) {
        this.partyId = partyId;
    }

    @Override
    public RegRecord getRecord() {
        return record;
    }

    @Override
    public void setRecord(final RegRecord record) {
        this.record = record;
    }

    @Override
    public ParPartyType getPartyType() {
        return partyType;
    }

    @Override
    public void setPartyType(final ParPartyType partyType) {
        this.partyType = partyType;
    }

    @Override
    public ParPartyName getPreferredName() {
        return preferredName;
    }

    @Override
    public void setPreferredName(final ParPartyName preferredName) {
        this.preferredName = preferredName;
    }

    @Override
    public String getHistory() {
        return history;
    }

    @Override
    public void setHistory(final String history) {
        this.history = history;
    }

    @Override
    public String getSourceInformation() {
        return sourceInformation;
    }

    @Override
    public void setSourceInformation(final String sourceInformation) {
        this.sourceInformation = sourceInformation;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.api.ParParty)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        cz.tacr.elza.api.ParParty<RegRecord, ParPartyType, ParPartyName> other = (cz.tacr.elza.api.ParParty<RegRecord, ParPartyType, ParPartyName>) obj;

        return new EqualsBuilder().append(partyId, other.getPartyId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(partyId).toHashCode();
    }

    @Override
    public String toString() {
        return "ParParty pk=" + partyId;
    }
}
