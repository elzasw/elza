package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Typ instituce.
 *
 * @author Martin Šlapa
 * @since 18.3.2016
 */
@Entity(name = "par_institution_type")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ParInstitutionType {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer institutionTypeId;

    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;

    /**
     * @return identifikátor
     */
    public Integer getInstitutionTypeId() {
        return institutionTypeId;
    }

    /**
     * @param institutionTypeId identifikátor
     */
    public void setInstitutionTypeId(final Integer institutionTypeId) {
        this.institutionTypeId = institutionTypeId;
    }

    /**
     * @return název typu instituce
     */
    public String getName() {
        return name;
    }

    /**
     * @param name název typu instituce
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return kód typu instituce
     */
    public String getCode() {
        return code;
    }

    /**
     * @param code kód typu instituce
     */
    public void setCode(final String code) {
        this.code = code;
    }
}
