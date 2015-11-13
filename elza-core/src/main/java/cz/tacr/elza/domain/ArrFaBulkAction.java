package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Implementace záznamu o posledním úspěšném doběhnutím hromadné akce.
 *
 * @author Martin Šlapa
 * @since 10.11.2015
 */
@Entity(name = "arr_fa_bulk_action")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrFaBulkAction implements cz.tacr.elza.api.ArrFaBulkAction<ArrChange, ArrFindingAidVersion> {

    @Id
    @GeneratedValue
    private Integer arrFaBulkActionId;

    @Column(nullable = false)
    private String bulkActionCode;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFindingAidVersion.class)
    @JoinColumn(name = "faVersionId", nullable = false)
    private ArrFindingAidVersion faVersion;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "faChangeId", nullable = false)
    private ArrChange faChange;


    @Override
    public Integer getArrFaBulkActionId() {
        return arrFaBulkActionId;
    }

    @Override
    public void setArrFaBulkActionId(final Integer bulkActionId) {
        this.arrFaBulkActionId = bulkActionId;
    }

    @Override
    public String getBulkActionCode() {
        return bulkActionCode;
    }

    @Override
    public void setBulkActionCode(final String bulkActionCode) {
        this.bulkActionCode = bulkActionCode;
    }

    @Override
    public ArrFindingAidVersion getFindingAidVersion() {
        return faVersion;
    }

    @Override
    public void setFindingAidVersion(final ArrFindingAidVersion findingAidVersion) {
        this.faVersion = findingAidVersion;
    }

    @Override
    public ArrChange getChange() {
        return faChange;
    }

    @Override
    public void setChange(final ArrChange change) {
        this.faChange = change;
    }
}
