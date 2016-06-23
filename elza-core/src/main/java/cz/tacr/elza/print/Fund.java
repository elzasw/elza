package cz.tacr.elza.print;

// TODO - JavaDoc - Lebeda

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import cz.tacr.elza.print.party.Institution;

import java.util.Date;

/**
 * Při tisku se vytvoří 1 instance
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class Fund {
    private final Output output; // vazba na nadřazený output
    private final ArrFund arrFund; // vazba na DB
    private final ArrFundVersion arrFundVersion; // vazba na DB

    private String name;
    private String internalCode;
    private Date createDate;
    private String dateRange;
    private Institution institution;

    public Fund(Output output, ArrFund arrFund, ArrFundVersion arrFundVersion) {
        this.output = output;
        this.arrFund = arrFund;
        this.arrFundVersion = arrFundVersion;
    }

    /**
     * @return kořenový node fondu - je vždy ve výstupu, pokud má výstup alespoň jeden přiřazený node
     */
    public Node getRootNode() {
        final ArrNode arrNode = arrFundVersion.getRootNode();
        return output.getNodes().stream()
                .filter(node -> node.getArrNode().equals(arrNode)) // node z output oddkazující na odpovídající node ve fundVersion
                .findFirst().orElse(null); // vrátit první (a jediný) nalezený jinak null
    }

    public ArrFund getArrFund() {
        return arrFund;
    }

    public ArrFundVersion getArrFundVersion() {
        return arrFundVersion;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getDateRange() {
        return dateRange;
    }

    public void setDateRange(String dateRange) {
        this.dateRange = dateRange;
    }

    public String getInternalCode() {
        return internalCode;
    }

    public void setInternalCode(String internalCode) {
        this.internalCode = internalCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
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
}
