package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;

/**
 * Implementace třídy {@link cz.tacr.elza.api.ArrNamedOutput}
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
@Entity(name = "arr_named_output")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrNamedOutput implements cz.tacr.elza.api.ArrNamedOutput<ArrFund> {

    @Id
    @GeneratedValue
    private Integer namedOutputId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fundId", nullable = false)
    private ArrFund fund;

    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;

    @Column(nullable = false)
    private Boolean temporary;

    @Column(nullable = false)
    private Boolean deleted;

    @Override
    public Integer getNamedOutputId() {
        return namedOutputId;
    }

    @Override
    public void setNamedOutputId(final Integer namedOutputId) {
        this.namedOutputId = namedOutputId;
    }

    @Override
    public ArrFund getFund() {
        return fund;
    }

    @Override
    public void setFund(final ArrFund fund) {
        this.fund = fund;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(final String code) {
        this.code = code;
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
    public Boolean getTemporary() {
        return temporary;
    }

    @Override
    public void setTemporary(final Boolean temporary) {
        this.temporary = temporary;
    }

    @Override
    public Boolean getDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(final Boolean deleted) {
        this.deleted = deleted;
    }
}
