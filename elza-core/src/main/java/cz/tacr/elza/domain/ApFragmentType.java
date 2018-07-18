package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;

import javax.persistence.*;

/**
 * Pravidla popisu fragmentu.
 * Vlastní pravidla jsou uložena v externím souboru {@link RulComponent}.
 *
 * @since 18.07.2018
 */
@Entity
@Table(name = "ap_fragment_type")
public class ApFragmentType {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer fragmentTypeId;

    @Column(length = StringLength.LENGTH_50, nullable = false, unique = true)
    private String code;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    public Integer getFragmentTypeId() {
        return fragmentTypeId;
    }

    public void setFragmentTypeId(final Integer fragmentTypeId) {
        this.fragmentTypeId = fragmentTypeId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public RulPackage getRulPackage() {
        return rulPackage;
    }

    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }
}
