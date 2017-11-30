package cz.tacr.elza.print;

import java.util.Date;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.print.party.Institution;

/**
 * Representation of general fund properties. One instance per {@link OutputModel} is present.
 */
public class Fund {

    private final NodeId rootNodeId;

    private String name;

    private String internalCode;

    private Date createDate;

    private String dateRange;

    private Institution institution;

    public Fund(NodeId rootNodeId) {
        this.rootNodeId = Validate.notNull(rootNodeId);
    }

    public NodeId getRootNodeId() {
        return rootNodeId;
    }

    public String getName() {
        return name;
    }

    void setName(final String name) {
        this.name = name;
    }

    public String getInternalCode() {
        return internalCode;
    }

    void setInternalCode(final String internalCode) {
        this.internalCode = internalCode;
    }

    public Date getCreateDate() {
        return createDate;
    }

    void setCreateDate(final Date createDate) {
        this.createDate = createDate;
    }

    public String getDateRange() {
        return dateRange;
    }

    void setDateRange(final String dateRange) {
        this.dateRange = dateRange;
    }

    public Institution getInstitution() {
        return institution;
    }

    void setInstitution(final Institution institution) {
        this.institution = institution;
    }
}
