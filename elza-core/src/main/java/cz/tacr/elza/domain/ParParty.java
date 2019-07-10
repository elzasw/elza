
package cz.tacr.elza.domain;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Type;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.api.interfaces.IApAccessPoint;
import cz.tacr.elza.api.interfaces.IApScope;


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
public class ParParty extends AbstractVersionableEntity implements IApAccessPoint {

    /* Konstanty pro vazby a fieldy. */
    public static final String FIELD_PARTY_ID = "partyId";
    public static final String FIELD_RECORD = "accessPoint";
    // TODO: Is it correct? FIELD_RECORD_FK
    public static final String FIELD_RECORD_FK = FIELD_RECORD + ".accessPointId";
    public static final String FIELD_PARTY_TYPE = "partyType";
    public static final String FIELD_PARTY_TYPE_ID = "partyTypeId";
    public static final String FIELD_PARTY_PREFERRED_NAME = "preferredName";
    public static final String FIELD_HISTORY = "history";
    public static final String FIELD_SOURCE_INFORMATION = "sourceInformation";
    public static final String FIELD_CHARACTERISTICS = "characteristics";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer partyId;

    @RestResource(exported = false)
	@OneToOne(fetch=FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = "accessPointId", nullable = false)
    @JsonIgnore
    private ApAccessPoint accessPoint;

    @RestResource(exported = false)
    @JsonIgnore
    @Column(nullable = false, insertable = false, updatable = false)
    private Integer accessPointId;

    @RestResource(exported = false)
	@ManyToOne(fetch=FetchType.LAZY, targetEntity = ParPartyType.class)
    @JoinColumn(name = "partyTypeId", nullable = false)
    @JsonIgnore
    private ParPartyType partyType;

    @Column(nullable = false, insertable = false, updatable = false)
    @JsonIgnore
    private Integer partyTypeId;

    @RestResource(exported = false)
	@OneToOne(fetch = FetchType.LAZY, targetEntity = ParPartyName.class)
    @JoinColumn(name = "preferredNameId")
    @JsonIgnore
    private ParPartyName preferredName;

    @Column(insertable = false, updatable = false)
    @JsonIgnore
    private Integer preferredNameId;

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

    @Column
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @JsonIgnore
    private String characteristics;

    @Column(nullable = false)
    private boolean originator = true;

    /**
     * Primární ID.
     * @return      id objektu
     */
    public Integer getPartyId() {
        return partyId;
    }

    /**
     * Primární ID.
     * @param partyId   id objektu
     */
    public void setPartyId(final Integer partyId) {
        this.partyId = partyId;
    }

    /**
     * Rejstříkové heslo.
     * @return  objekt navázaného rejstříkového hesla
     */
    public ApAccessPoint getAccessPoint() {
        return accessPoint;
    }

    /**
     * Rejstříkové heslo.
     * @param accessPoint    objekt navázaného rejstříkového hesla
     */
    public void setAccessPoint(final ApAccessPoint accessPoint) {
        this.accessPoint = accessPoint;
        this.accessPointId = accessPoint != null ? accessPoint.getAccessPointId() : null;
    }

    @Override
    public Integer getAccessPointId() {
        return accessPointId;
    }

    /**
     * Typ osoby.
     * @return typ osoby.
     */
    public ParPartyType getPartyType() {
        return partyType;
    }

    /**
     * Typ osoby.
     * @param partyType
     */
    public void setPartyType(final ParPartyType partyType) {
        this.partyType = partyType;
        this.partyTypeId = partyType != null ? partyType.getPartyTypeId() : null;
    }

    public Integer getPartyTypeId() {
        return partyTypeId;
    }

    public ParPartyName getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(final ParPartyName preferredName) {
        this.preferredName = preferredName;
        this.preferredNameId = preferredName != null ? preferredName.getPartyNameId() : null;
    }

    public Integer getPreferredNameId() {
        return preferredNameId;
    }

    public List<ParPartyName> getPartyNames() {
        return partyNames;
    }

    public void setPartyNames(final List<ParPartyName> partyNames) {
        this.partyNames = partyNames;
    }

    /**
     * Dějiny osoby.
     * @return dějiny osoby
     */
    public String getHistory() {
        return history;
    }

    /**
     * Dějiny osoby.
     * @param history dějiny osoby
     */
    public void setHistory(final String history) {
        this.history = history;
    }

    /**
     * Zdroje informací.
     * @return  zdroje informací
     */
    public String getSourceInformation() {
        return sourceInformation;
    }

    /**
     * Zdroje informací.
     * @param sourceInformation zdroje informací
     */
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

    public boolean isOriginator() {
        return originator;
    }

    public void setOriginator(final boolean originator) {
        this.originator = originator;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ParParty)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ParParty other = (ParParty) obj;

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
