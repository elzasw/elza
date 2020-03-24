package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;
import org.hibernate.annotations.Type;

import javax.persistence.*;

/**
 * Fragment je popisem části přístupového bodu nebo jména. Fragment je tvořen prvky popisu. Fragment má svůj vnitřní stav,
 * který je uložen v atributu state. Fragment je možné reprezentovat textovou hodnotou. Ta je uložena v hodnotě value.
 *
 * @since 17.07.2018
 */
@Entity
@Table(name = "ap_fragment")
public class ApFragment {

    public static final String FRAGMENT_ID = "fragmentId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer fragmentId;

    @Column
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String value;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM)
    private ApStateEnum state;

    @Column
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String errorDescription;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulStructuredType.class)
    @JoinColumn(name = "fragmentTypeId", nullable = false)
    private RulStructuredType fragmentType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApFragment.class)
    @JoinColumn(name = "ap_fragment_parent_id")
    private ApFragment parentFragment;

    public Integer getFragmentId() {
        return fragmentId;
    }

    public void setFragmentId(final Integer fragmentId) {
        this.fragmentId = fragmentId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public ApStateEnum getState() {
        return state;
    }

    public void setState(final ApStateEnum state) {
        this.state = state;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(final String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public RulStructuredType getFragmentType() {
        return fragmentType;
    }

    public void setFragmentType(final RulStructuredType fragmentType) {
        this.fragmentType = fragmentType;
    }

    public ApFragment getParentFragment() {
        return parentFragment;
    }

    public void setParentFragment(ApFragment parentFragment) {
        this.parentFragment = parentFragment;
    }
}
