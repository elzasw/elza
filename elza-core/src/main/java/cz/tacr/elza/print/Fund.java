package cz.tacr.elza.print;

import java.util.Date;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.print.party.Institution;

/**
 * Při tisku se vytvoří 1 instance
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class Fund {

    private final ArrFundVersion arrFundVersion; // vazba na DB

    private String name;
    private String internalCode;
    private Date createDate;
    private String dateRange;
    private Institution institution;
    private NodeId rootNodeId;

    public Fund(final NodeId rootNodeId, final ArrFundVersion arrFundVersion) {
        this.rootNodeId = rootNodeId;
        this.arrFundVersion = arrFundVersion;
    }

    /**
     * @return kořenový node fondu - je vždy ve výstupu, pokud má výstup alespoň jeden přiřazený node
     */
    public NodeId getRootNodeId() {
        return rootNodeId;
    }

    public ArrFundVersion getArrFundVersion() {
        return arrFundVersion;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(final Institution institution) {
        this.institution = institution;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(final Date createDate) {
        this.createDate = createDate;
    }

    public String getDateRange() {
        return dateRange;
    }

    public void setDateRange(final String dateRange) {
        this.dateRange = dateRange;
    }

    public String getInternalCode() {
        return internalCode;
    }

    public void setInternalCode(final String internalCode) {
        this.internalCode = internalCode;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(o, this);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
    }

    public void setRootNodeId(final NodeId rootNodeId) {
        this.rootNodeId = rootNodeId;
    }
}
