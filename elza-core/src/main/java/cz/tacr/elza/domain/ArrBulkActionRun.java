package cz.tacr.elza.domain;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.tacr.elza.bulkaction.generator.result.Result;
import cz.tacr.elza.domain.table.ElzaColumn;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.rest.core.annotation.RestResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Implementace záznamu o posledním úspěšném doběhnutím hromadné akce.
 *
 * @author Martin Šlapa
 * @since 10.11.2015
 */
@Entity(name = "arr_bulk_action_run")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrBulkActionRun implements cz.tacr.elza.api.ArrBulkActionRun<ArrChange, ArrFundVersion, ArrBulkActionNode, ArrOutputDefinition, Result> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Id
    @GeneratedValue
    private Integer bulkActionRunId;

    @Column(nullable = false)
    private String bulkActionCode;

    @Override
    public List<ArrBulkActionNode> getArrBulkActionNodes() {
        return arrBulkActionNodes;
    }

    @Override
    public void setArrBulkActionNodes(List<ArrBulkActionNode> arrBulkActionNodes) {
        this.arrBulkActionNodes = arrBulkActionNodes;
    }

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFundVersion.class)
    @JoinColumn(name = "fundVersionId", nullable = false)
    private ArrFundVersion fundVersion;

    @Column(insertable = false, updatable = false)
    @ReadOnlyProperty
    private Integer fundVersionId;

    @Column
    private Integer userId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "changeId", nullable = false)
    private ArrChange change;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private State state = State.WAITING;

    @Column
    private Date datePlanned;

    @Column
    private Date dateStarted;

    @Column
    private Date dateFinished;

    @Column(length = 10000)
    private String error;

    @Transient
    private boolean interrupted = false;

    @RestResource(exported = false)
    @OneToMany(mappedBy = "bulkActionRun", fetch = FetchType.LAZY, targetEntity = ArrBulkActionNode.class)
    private List<ArrBulkActionNode> arrBulkActionNodes = new ArrayList<>(0);

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column
    private String result;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrOutputDefinition.class)
    @JoinColumn(name = "outputDefinitionId")
    private ArrOutputDefinition outputDefinition;

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
    public ArrFundVersion getFundVersion() {
        return fundVersion;
    }

    @Override
    public void setFundVersion(final ArrFundVersion fundVersion) {
        this.fundVersion = fundVersion;
        this.fundVersionId = fundVersion.getFundVersionId();
    }

    @Override
    public Integer getUserId() {
        return userId;
    }

    @Override
    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    @Override
    public ArrChange getChange() {
        return change;
    }

    @Override
    public void setChange(final ArrChange change) {
        this.change = change;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public Date getDateFinished() {
        return dateFinished;
    }

    @Override
    public void setDateFinished(Date dateFinished) {
        this.dateFinished = dateFinished;
    }

    @Override
    public Date getDateStarted() {
        return dateStarted;
    }

    @Override
    public void setDateStarted(Date dateStarted) {
        this.dateStarted = dateStarted;
    }

    public Date getDatePlanned() {
        return datePlanned;
    }

    public void setDatePlanned(Date datePlaned) {
        this.datePlanned = datePlaned;
    }

    @Override
    public String getError() {
        return error;
    }

    @Override
    public void setError(String error) {
        this.error = error;
    }

    public Integer getFundVersionId() {
        return fundVersionId;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }

    @Override
    public Result getResult() {
        if (this.result == null) {
            return null;
        }
        try {
            return objectMapper.readValue(this.result, new TypeReference<Result>(){});
        } catch (IOException e) {
            throw new IllegalArgumentException("Problém při generování JSON", e);
        }
    }

    @Override
    public void setResult(final Result result) {
        try {
            this.result = objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Problém při parsování JSON", e);
        }
    }

    @Override
    public ArrOutputDefinition getOutputDefinition() {
        return outputDefinition;
    }

    @Override
    public void setOutputDefinition(final ArrOutputDefinition outputDefinition) {
        this.outputDefinition = outputDefinition;
    }
}