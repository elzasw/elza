package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;

import javax.persistence.*;

@Entity(name = "ap_binding_item")
public class ApBindingItem {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer bindingItemId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApBinding.class)
    @JoinColumn(name = "bindingId", nullable = false)
    private ApBinding binding;

    @Column(length = StringLength.LENGTH_50, nullable = false)
    private String value;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApPart.class)
    @JoinColumn(name = "partId")
    private ApPart part;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApItem.class)
    @JoinColumn(name = "itemId")
    private ApItem item;

    @Column(name = "camIdentifier")
    private Boolean camIdentifier;

    public Integer getBindingItemId() {
        return bindingItemId;
    }

    public void setBindingItemId(Integer bindingItemId) {
        this.bindingItemId = bindingItemId;
    }

    public ApBinding getBinding() {
        return binding;
    }

    public void setBinding(ApBinding binding) {
        this.binding = binding;
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
    }

    public ApItem getItem() {
        return item;
    }

    public void setItem(ApItem item) {
        this.item = item;
    }

    public Boolean getCamIdentifier() {
        return camIdentifier;
    }

    public void setCamIdentifier(Boolean camIdentifier) {
        this.camIdentifier = camIdentifier;
    }
}
