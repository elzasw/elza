package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.*;


/**
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 16.6.2016
 */
@Entity(name = "rul_output_type")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulOutputType implements cz.tacr.elza.api.RulOutputType<RulPackage> {

    @Id
    @GeneratedValue
    private Integer outputTypeId;

    @Column(length = 50, nullable = false)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;


    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Override
    public Integer getOutputTypeId() {
        return outputTypeId;
    }

    @Override
    public void setOutputTypeId(Integer outputTypeId) {
        this.outputTypeId = outputTypeId;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public RulPackage getPackage() {
        return rulPackage;
    }

    @Override
    public void setPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }

}
