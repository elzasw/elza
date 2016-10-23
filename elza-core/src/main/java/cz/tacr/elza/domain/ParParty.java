package cz.tacr.elza.domain;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.tacr.elza.api.RegScope;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;


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
    public static final String CHARACTERISTICS = "characteristics";

    @Id
    @GeneratedValue
    private Integer partyId;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.EAGER, targetEntity = RegRecord.class)
    @JoinColumn(name = "recordId", nullable = false)
    @JsonIgnore
    private RegRecord record;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ParPartyType.class)
    @JoinColumn(name = "partyTypeId", nullable = false)
    @JsonIgnore
    private ParPartyType partyType;

    @RestResource(exported = false)
    @OneToOne(fetch = FetchType.EAGER, targetEntity = ParPartyName.class)
    @JoinColumn(name = "preferredNameId")
    @JsonIgnore
    private ParPartyName preferredName;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ParUnitdate.class)
    @JoinColumn(name = "fromUnitdateId")
    @JsonIgnore
    private ParUnitdate from;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ParUnitdate.class)
    @JoinColumn(name = "toUnitdateId")
    @JsonIgnore
    private ParUnitdate to;

    @RestResource(exported = false)
    @OneToMany(mappedBy = "party", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ParPartyName> partyNames;

    @OneToMany(mappedBy = "party", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ParCreator> partyCreators;

    @OneToMany(mappedBy = "party", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ParRelation> relations;

    @Column()
    @JsonIgnore
    private String history;

    @Column()
    @JsonIgnore
    private String sourceInformation;

    @Column(length = StringLength.LENGTH_1000, nullable = true)
    @JsonIgnore
    private String characteristics;

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

    public ParUnitdate getFrom() {
        return from;
    }

    public void setFrom(final ParUnitdate from) {
        this.from = from;
    }

    public ParUnitdate getTo() {
        return to;
    }

    public void setTo(final ParUnitdate to) {
        this.to = to;
    }

    public List<ParPartyName> getPartyNames() {
        return partyNames;
    }

    public void setPartyNames(final List<ParPartyName> partyNames) {
        this.partyNames = partyNames;
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

    public String getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics(final String characteristics) {
        this.characteristics = characteristics;
    }

    public List<ParCreator> getPartyCreators() {
        return partyCreators;
    }

    public void setPartyCreators(final List<ParCreator> partyCreators) {
        this.partyCreators = partyCreators;
    }

    public List<ParRelation> getRelations() {
        return relations;
    }

    public void setRelations(final List<ParRelation> relations) {
        this.relations = relations;
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

    @Override
    @JsonIgnore
    public RegScope getRegScope() {
        return record.getScope();
    }
}
