package cz.tacr.elza.print;

import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.print.party.Institution;

/**
 * Representation of general fund properties. One instance per {@link OutputModel} is present.
 */
public class Fund {

    private final NodeId rootNodeId;

    private final NodeLoader nodeLoader;

    private String name;

    private String internalCode;

    private Date createDate;

    private String dateRange;

    private Institution institution;

    private Integer fundNumber;

    private String unitdate;

    private String mark;

    private Integer fundId;

    public Fund(NodeId rootNodeId, NodeLoader nodeLoader, ArrFund fund) {
        this.rootNodeId = Validate.notNull(rootNodeId);
        this.nodeLoader = Validate.notNull(nodeLoader);
        Validate.notNull(fund);

        initFundData(fund);
    }

    public Fund(ArrFund fund) {
        this.rootNodeId = null;
        this.nodeLoader = null;
        Validate.notNull(fund);
        initFundData(fund);
    }

    private void initFundData(ArrFund fund) {
        fundId = fund.getFundId();
        name = fund.getName();
        internalCode = fund.getInternalCode();
        createDate = Date.from(fund.getCreateDate().atZone(ZoneId.systemDefault()).toInstant());
        fundNumber = fund.getFundNumber();
        unitdate = fund.getUnitdate();
        mark = fund.getMark();
    }

    public Integer getFundId() {
        return fundId;
    }

    public NodeId getRootNodeId() {
        return rootNodeId;
    }

    public Node getRootNode() {
        List<Node> rootNode = nodeLoader.loadNodes(Collections.singletonList(rootNodeId));
        Validate.isTrue(rootNode.size() == 1);
        return rootNode.get(0);
    }

    public String getName() {
        return name;
    }

    public String getInternalCode() {
        return internalCode;
    }

    public Date getCreateDate() {
        return createDate;
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

	public Integer getFundNumber() {
		return fundNumber;
	}

	public String getUnitdate() {
		return unitdate;
	}

	public String getMark() {
		return mark;
	}
}
