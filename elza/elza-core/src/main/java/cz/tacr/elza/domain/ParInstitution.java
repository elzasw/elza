package cz.tacr.elza.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Instituce.
 *
 * @author Martin Šlapa
 * @since 18.3.2016
 */
@Entity(name = "par_institution")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ParInstitution {

    public static final String FIELD_INTERNAL_CODE = "internalCode";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer institutionId;

    @Column(length = 250, nullable = false)
    private String internalCode;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = ParInstitutionType.class)
    @JoinColumn(name = "institutionTypeId", nullable = true)
    private ParInstitutionType institutionType;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = "accessPointId", nullable = false)
    private ApAccessPoint accessPoint;

    @Column(updatable = false, insertable = false, nullable = false)
    private Integer accessPointId;

    @Column(length = 250, nullable = true)
    private String name;

    @Column(length = 250, nullable = true)
    private String shortName;

    /**
     * @return identifikátor
     */
    public Integer getInstitutionId() {
        return institutionId;
    }

    /**
     * @param institutionId identifikátor
     */
    public void setInstitutionId(final Integer institutionId) {
        this.institutionId = institutionId;
    }

    /**
     * @return kód instituce
     */
    public String getInternalCode() {
        return internalCode;
    }

    /**
     * @param internalCode kód instituce
     */
    public void setInternalCode(final String internalCode) {
        this.internalCode = internalCode;
    }

    /**
     * @return typ instituce
     */
    public ParInstitutionType getInstitutionType() {
        return institutionType;
    }

    /**
     * @return id typu instituce
     */
    public Integer getInstitutionTypeId() {
    	if (institutionType == null) {
    		return null;
    	}
        return institutionType.getInstitutionTypeId();
    }

    /**
     * @param institutionType typ instituce
     */
    public void setInstitutionType(final ParInstitutionType institutionType) {
        this.institutionType = institutionType;
    }

    /**
     * @return přístupový bod
     */
    public ApAccessPoint getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(ApAccessPoint accessPoint) {
        this.accessPoint = accessPoint;
        this.accessPointId = accessPoint == null ? null : accessPoint.getAccessPointId();
    }

    public Integer getAccessPointId() {
        return accessPointId;
    }

    public void setAccessPointId(Integer accessPointId) {
        this.accessPointId = accessPointId;
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}
}
