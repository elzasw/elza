package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.*;

/**
 * Hodnota atributu archivního popisu typu {@link ApFragment}.
 */
@Entity
@Table(name = "arr_data_apfrag_ref")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataApFragRef extends ArrData {

    public static final String FRAGMENT = "fragment";

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = "fragmentId", nullable = false)
    private ApFragment fragment;

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

    public ApFragment getFragment() {
        return fragment;
    }

    public void setFragment(final ApFragment fragment) {
        this.fragment = fragment;
        this.fragmentId = fragment == null ? null : fragment.getFragmentId();
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

}
