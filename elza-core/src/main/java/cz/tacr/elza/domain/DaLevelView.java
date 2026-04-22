package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.*;

import java.util.List;

@Entity(name = "da_level_view")
public class DaLevelView {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer levelViewId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaChange.class)
    @JoinColumn(name = "create_change_id", nullable = false)
    private DaChange createChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaChange.class)
    @JoinColumn(name = "delete_change_id")
    private DaChange deleteChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fund_id", nullable = false)
    private ArrFund fund;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaLevelView.class)
    @JoinColumn(name = "parent_level_view_id")
    private DaLevelView parentLevelView;

    @Column(name = "label", length = StringLength.LENGTH_250)
    private String label;

    @OneToMany(mappedBy = "parentLevelView", fetch = FetchType.LAZY)
    private List<DaLevelView> children;

    public List<DaLevelView> getChildren() {
        return children;
    }

    public void setChildren(List<DaLevelView> children) {
        this.children = children;
    }

    public Integer getLevelViewId() {
        return levelViewId;
    }

    public void setLevelViewId(Integer levelViewId) {
        this.levelViewId = levelViewId;
    }

    public DaChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(DaChange createChange) {
        this.createChange = createChange;
    }

    public DaChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(DaChange deleteChange) {
        this.deleteChange = deleteChange;
    }

    public ArrFund getFund() {
        return fund;
    }

    public void setFund(ArrFund fund) {
        this.fund = fund;
    }

    public DaLevelView getParentLevelView() {
        return parentLevelView;
    }

    public void setParentLevelView(DaLevelView parentLevelView) {
        this.parentLevelView = parentLevelView;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
