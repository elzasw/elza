package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.*;
import java.net.URI;

@Entity
@Table(name="arr_data_uri_ref")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataUriRef extends ArrData {

    public static final String DESCRIPTION = "description";

    @Basic
    @Column(name="schema", nullable = false, length = 50)
    private String schema;

    @Basic
    @Column(name="value", nullable = false, length = 2000)
    private String value;

    @Basic
    @Column(name="description", length = 2000)
    private String description;

    @Column(name = "nodeId", updatable = false, insertable = false)
    private Integer nodeId;

    @RestResource(exported = false)
    @ManyToOne(fetch=FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name="nodeId")
    private ArrNode arrNode;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrRefTemplate.class)
    @JoinColumn(name = "refTemplateId")
    private ArrRefTemplate refTemplate;

    @Column(name = "refTemplateId", updatable = false, insertable = false)
    private Integer refTemplateId;

    @Transient
    private boolean deletingProcess = false;

    public ArrDataUriRef() {

    }

    public ArrDataUriRef(final String schema, final String value, final String description, final ArrNode arrNode) {
        this.schema = schema;
        this.value = value;
        this.description = description;
        this.arrNode = arrNode;
    }

    public ArrDataUriRef(ArrDataUriRef src) {
            super(src);
            copyValue(src);
    }

    private void copyValue(ArrDataUriRef src) {
        this.schema = src.schema;
        this.value = src.value;
        this.description = src.description;
        this.arrNode = src.arrNode;
        this.nodeId = src.nodeId;
        this.refTemplate = src.refTemplate;
        this.refTemplateId = src.refTemplateId;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public ArrNode getArrNode() {
        return arrNode;
    }

    public void setArrNode(ArrNode arrNode) {
        this.arrNode = arrNode;
        this.nodeId = arrNode == null ? null : arrNode.getNodeId();
    }

    public ArrRefTemplate getRefTemplate() {
        return refTemplate;
    }

    public void setRefTemplate(ArrRefTemplate refTemplate) {
        this.refTemplate = refTemplate;
        this.refTemplateId = refTemplate == null ? null : refTemplate.getRefTemplateId();
    }

    public Integer getRefTemplateId() {
        return refTemplateId;
    }

    @Override
    public String getFulltextValue() {
        return value + ";" + description;
    }

    @Override
    protected boolean isEqualValueInternal(ArrData srcData) {
        ArrDataUriRef src = (ArrDataUriRef) srcData;
        return value.equals(src.value);
    }

    public boolean isDeletingProcess() {
        return deletingProcess;
    }

    public void setDeletingProcess(boolean deletingProcess) {
        this.deletingProcess = deletingProcess;
    }

    @Override
    public ArrDataUriRef makeCopy() {
        return new ArrDataUriRef(this);
    }

    @Override
    protected void mergeInternal(ArrData srcData) {
        ArrDataUriRef src = (ArrDataUriRef) srcData;
        copyValue(src);
    }

    @Override
    protected void validateInternal() {
        Validate.notNull(value);
        Validate.notNull(schema);

    }

    public static String createSchema(String value) {
        if(StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException("Nebyl zadán odkaz, nebo je odkaz prázdný");
        }
        URI tempUri = URI.create(value).normalize();
        return tempUri.getScheme();
    }
}
