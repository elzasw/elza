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
@Entity(name = "arr_bulk_action_run")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrBulkActionRun implements cz.tacr.elza.api.ArrBulkActionRun<ArrChange, ArrFindingAidVersion> {

    @Id
    @GeneratedValue
    private Integer bulkActionRunId;

    @Column(nullable = false)
    private String bulkActionCode;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFindingAidVersion.class)
    @JoinColumn(name = "versionId", nullable = false)
    private ArrFindingAidVersion version;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "changeId", nullable = false)
    private ArrChange change;


    @Override
    public Integer getBulkActionRunId() {
        return bulkActionRunId;
    }

    @Override
    public void setBulkActionRunId(final Integer bulkActionId) {
        this.bulkActionRunId = bulkActionId;
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
        return version;
    }

    @Override
    public void setFindingAidVersion(final ArrFindingAidVersion findingAidVersion) {
        this.version = findingAidVersion;
    }

    @Override
    public ArrChange getChange() {
        return change;
    }

    @Override
    public void setChange(final ArrChange change) {
        this.change = change;
    }
}
