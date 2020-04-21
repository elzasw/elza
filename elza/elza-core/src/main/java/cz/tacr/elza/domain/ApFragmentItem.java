package cz.tacr.elza.domain;

import javax.persistence.*;

/**
 * @since 17.07.2018
 */
@Entity
@Table(name = "ap_fragment_item")
public class ApFragmentItem extends ApItem {

    public static final String FRAGMENT_ID = "fragmentId";

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApPart.class)
    @JoinColumn(name = FRAGMENT_ID, nullable = false)
    private ApPart fragment;

    @Column(name = FRAGMENT_ID, nullable = false, updatable = false, insertable = false)
    private Integer fragmentId;

    public ApFragmentItem() {
    }

    @Override
    public ApFragmentItem copy() {
        return new ApFragmentItem(this);
    }

    public ApFragmentItem(final ApFragmentItem other) {
        super(other);
        this.fragment = other.fragment;
        this.fragmentId = other.fragmentId;
    }

    public ApPart getFragment() {
        return fragment;
    }

    public void setFragment(final ApPart fragment) {
        this.fragment = fragment;
        this.fragmentId = fragment == null ? null : fragment.getPartId();
    }
}
