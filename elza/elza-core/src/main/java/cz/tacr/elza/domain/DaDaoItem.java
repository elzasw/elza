package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity(name = "da_dao_item")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "preferredPart", "lastUpdate"})
public class DaDaoItem {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer daoItemId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaDao.class)
    @JoinColumn(name = "dao_id", nullable = false)
    private DaDao dao;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaChange.class)
    @JoinColumn(name = "create_change_id", nullable = false)
    private DaChange createChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaChange.class)
    @JoinColumn(name = "delete_change_id")
    private DaChange deleteChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemType.class)
    @JoinColumn(name = "itemTypeId", nullable = false)
    private RulItemType itemType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemSpec.class)
    @JoinColumn(name = "itemSpecId")
    private RulItemSpec itemSpec;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrData.class)
    @JoinColumn(name = "dataId")
    private ArrData data;

    public Integer getDaoItemId() {
        return daoItemId;
    }

    public void setDaoItemId(Integer daoItemId) {
        this.daoItemId = daoItemId;
    }

    public DaDao getDao() {
        return dao;
    }

    public void setDao(DaDao dao) {
        this.dao = dao;
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

    public RulItemType getItemType() {
        return itemType;
    }

    public void setItemType(RulItemType itemType) {
        this.itemType = itemType;
    }

    public RulItemSpec getItemSpec() {
        return itemSpec;
    }

    public void setItemSpec(RulItemSpec itemSpec) {
        this.itemSpec = itemSpec;
    }

    public ArrData getData() {
        return data;
    }

    public void setData(ArrData data) {
        this.data = data;
    }
}
