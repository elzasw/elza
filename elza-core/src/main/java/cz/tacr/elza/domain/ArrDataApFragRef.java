package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.Validate;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Hodnota atributu archivního popisu typu {@link ApPart}.
 */
@Entity
@Table(name = "arr_data_apfrag_ref")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataApFragRef extends ArrData {

    public static final String FRAGMENT = "fragment";

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApPart.class)
    @JoinColumn(name = "fragmentId", nullable = false)
    private ApPart fragment;

    @Column(name = "fragmentId", updatable = false, insertable = false)
    private Integer fragmentId;

    public ArrDataApFragRef() {

    }

    protected ArrDataApFragRef(ArrDataApFragRef src) {
        super(src);
        copyValue(src);
    }

    private void copyValue(ArrDataApFragRef src) {
        this.fragment = src.fragment;
        this.fragmentId = src.fragmentId;
    }

    public ApPart getFragment() {
        return fragment;
    }

    public void setFragment(final ApPart fragment) {
        this.fragment = fragment;
        this.fragmentId = fragment == null ? null : fragment.getPartId();
    }

    public Integer getFragmentId() {
        return fragmentId;
    }

    @Override
    public String getFulltextValue() {
        return null; // TODO: není jasné jakým způsobem indexovat
    }

    @Override
    public ArrDataApFragRef makeCopy() {
        return new ArrDataApFragRef(this);
    }

    @Override
    protected boolean isEqualValueInternal(ArrData srcData) {
        ArrDataApFragRef src = (ArrDataApFragRef) srcData;
        return fragmentId.equals(src.fragmentId);
    }

    @Override
    public void mergeInternal(final ArrData srcData) {
        ArrDataApFragRef src = (ArrDataApFragRef) srcData;
        this.copyValue(src);
    }

    @Override
    protected void validateInternal() {
        Validate.notNull(fragment);
        Validate.notNull(fragmentId);
    }

}
