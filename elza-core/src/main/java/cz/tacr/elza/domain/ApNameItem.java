package cz.tacr.elza.domain;

import javax.persistence.*;

/**
 * Prvek popisu jména přístupového bodu.
 *
 * @since 17.07.2018
 */
@Entity
@Table(name = "ap_name_item")
public class ApNameItem extends ApItem {

    public static final String NAME_ID = "nameId";

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApName.class)
    @JoinColumn(name = NAME_ID, nullable = false)
    private ApName name;

    @Column(name = NAME_ID, nullable = false, updatable = false, insertable = false)
    private Integer nameId;

    public ApNameItem() {
    }

    @Override
    public ApNameItem copy() {
        return new ApNameItem(this);
    }

    public ApNameItem(final ApNameItem other) {
        super(other);
        this.name = other.name;
        this.nameId = other.nameId;
    }

    public ApName getName() {
        return name;
    }

    public void setName(final ApName name) {
        this.name = name;
        this.nameId = name == null ? null : name.getNameId();
    }

    public Integer getNameId() {
        return nameId;
    }
}
