package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.service.cache.AccessPointCacheSerializable;

import jakarta.persistence.*;

@Entity(name = "ap_binding_item")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ApBindingItem implements AccessPointCacheSerializable {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer bindingItemId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApBinding.class)
    @JoinColumn(name = "bindingId", nullable = false)
    private ApBinding binding;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer bindingId;

    @Column(name = "item_value", length = StringLength.LENGTH_50, nullable = false)
    private String value;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApPart.class)
    @JoinColumn(name = "partId")
    private ApPart part;

    @Column(nullable = true, updatable = false, insertable = false)
    private Integer partId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApItem.class)
    @JoinColumn(name = "itemId")
    private ApItem item;

    @Column(nullable = true, updatable = false, insertable = false)
    private Integer itemId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private ApChange createChange;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer createChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "deleteChangeId")
    private ApChange deleteChange;

    @Column(nullable = true, updatable = false, insertable = false)
    private Integer deleteChangeId;

    public Integer getBindingItemId() {
        return bindingItemId;
    }

    public void setBindingItemId(Integer bindingItemId) {
        this.bindingItemId = bindingItemId;
    }

    public ApBinding getBinding() {
        return binding;
    }

    public Integer getBindingId() {
        return bindingId;
    }

    public void setBinding(ApBinding binding) {
        this.binding = binding;
        this.bindingId = binding != null ? binding.getBindingId() : null;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ApPart getPart() {
        return part;
    }

    public void setPart(ApPart part) {
        this.part = part;
        if (part == null) {
            this.partId = null;
        } else {
            this.partId = part.getPartId();
        }
    }

    public Integer getPartId() {
        return partId;
    }

    public ApItem getItem() {
        return item;
    }

    public void setItem(ApItem item) {
        this.item = item;
        if (item == null) {
            this.itemId = null;
        } else {
            this.itemId = item.getItemId();
        }
    }

    public Integer getItemId() {
        return itemId;
    }

    public ApChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(ApChange createChange) {
        this.createChange = createChange;
        if (createChange == null) {
            this.createChangeId = null;
        } else {
            this.createChangeId = createChange.getChangeId();
        }
    }

    public Integer getCreateChangeId() {
        return createChangeId;
    }

    public ApChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(ApChange deleteChange) {
        this.deleteChange = deleteChange;
        if (deleteChange == null) {
            this.deleteChangeId = null;
        } else {
            this.deleteChangeId = deleteChange.getChangeId();
        }
    }

    public Integer getDeleteChangeId() {
        return deleteChangeId;
    }

}
