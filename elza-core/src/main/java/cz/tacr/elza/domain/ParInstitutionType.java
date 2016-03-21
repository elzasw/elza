package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Implementace {@link cz.tacr.elza.api.ParInstitutionType}.
 *
 * @author Martin Šlapa
 * @since 18.3.2016
 */
@Entity(name = "par_institution_type")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ParInstitutionType implements cz.tacr.elza.api.ParInstitutionType {

    @Id
    @GeneratedValue
    private Integer institutionTypeId;

    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;

    @Override
    public Integer getInstitutionTypeId() {
        return institutionTypeId;
    }

    @Override
    public void setInstitutionTypeId(final Integer institutionTypeId) {
        this.institutionTypeId = institutionTypeId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(final String code) {
        this.code = code;
    }
}
