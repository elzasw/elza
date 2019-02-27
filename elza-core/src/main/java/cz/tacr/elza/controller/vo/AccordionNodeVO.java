package cz.tacr.elza.controller.vo;

import java.util.List;
import java.util.Objects;

/**
 * Struktura pro položky v accordionu.
 */
public class AccordionNodeVO {

    /**
     * Nodeid uzlu.
     */
    private Integer id;

    /**
     * Popis levé části accordionu.
     */
    private String accordionLeft;

    /**
     * Popis pravé části accordionu.
     */
    private String accordionRight;

    /**
     * Označení JP.
     */
    private String[] referenceMark;

    /**
     * Informace o stavu JP.
     */
    private NodeConformityVO nodeConformity;

    /**
     * Seznam otevřených požadavků na digitalizaci.
     */
    private List<ArrDigitizationRequestVO> digitizationRequests;

    /**
     * DB verze JP.
     */
    private Integer version;

    /**
     * Seznam otevřených připomínek.
     */
    private List<WfSimpleIssueVO> issues;

    public AccordionNodeVO() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public NodeConformityVO getNodeConformity() {
        return nodeConformity;
    }

    public void setNodeConformity(final NodeConformityVO nodeConformity) {
        this.nodeConformity = nodeConformity;
    }

    public List<ArrDigitizationRequestVO> getDigitizationRequests() {
        return digitizationRequests;
    }

    public void setDigitizationRequests(final List<ArrDigitizationRequestVO> digitizationRequests) {
        this.digitizationRequests = digitizationRequests;
    }

    public String getAccordionLeft() {
        return accordionLeft;
    }

    public void setAccordionLeft(final String accordionLeft) {
        this.accordionLeft = accordionLeft;
    }

    public String getAccordionRight() {
        return accordionRight;
    }

    public void setAccordionRight(final String accordionRight) {
        this.accordionRight = accordionRight;
    }

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(final Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        AccordionNodeVO that = (AccordionNodeVO) o;
        return Objects.equals(id, that.id);
	}

    public void setReferenceMark(final String[] referenceMark) {
        this.referenceMark = referenceMark;
    }

    public String[] getReferenceMark() {
        return referenceMark;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public Integer getVersion() {
        return version;
    }

    public List<WfSimpleIssueVO> getIssues() {
        return issues;
    }

    public void setIssues(List<WfSimpleIssueVO> issues) {
        this.issues = issues;
    }
}
