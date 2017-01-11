package cz.tacr.elza.domain;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.bulkaction.generator.result.Result;


/**
 * Implementace záznamu o posledním úspěšném doběhnutím hromadné akce.
 *
 * @author Martin Šlapa
 * @since 10.11.2015
 */
@Entity(name = "arr_bulk_action_run")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrBulkActionRun implements Serializable {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Id
    @GeneratedValue
    private Integer bulkActionRunId;

    @Column(nullable = false)
    private String bulkActionCode;

    public List<ArrBulkActionNode> getArrBulkActionNodes() {
        return arrBulkActionNodes;
    }

    public void setArrBulkActionNodes(final List<ArrBulkActionNode> arrBulkActionNodes) {
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

    public Integer getBulkActionRunId() {
        return bulkActionRunId;
    }

    public void setBulkActionRunId(final Integer bulkActionId) {
        this.bulkActionRunId = bulkActionId;
    }

    public String getBulkActionCode() {
        return bulkActionCode;
    }

    public void setBulkActionCode(final String bulkActionCode) {
        this.bulkActionCode = bulkActionCode;
    }

    public ArrFundVersion getFundVersion() {
        return fundVersion;
    }

    public void setFundVersion(final ArrFundVersion fundVersion) {
        this.fundVersion = fundVersion;
        this.fundVersionId = fundVersion.getFundVersionId();
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(final Integer userId) {
        this.userId = userId;
    }

    public ArrChange getChange() {
        return change;
    }

    public void setChange(final ArrChange change) {
        this.change = change;
    }

    public State getState() {
        return state;
    }

    public void setState(final State state) {
        this.state = state;
    }

    public Date getDateFinished() {
        return dateFinished;
    }

    public void setDateFinished(final Date dateFinished) {
        this.dateFinished = dateFinished;
    }

    public Date getDateStarted() {
        return dateStarted;
    }

    public void setDateStarted(final Date dateStarted) {
        this.dateStarted = dateStarted;
    }

    public Date getDatePlanned() {
        return datePlanned;
    }

    public void setDatePlanned(final Date datePlaned) {
        this.datePlanned = datePlaned;
    }

    public String getError() {
        return error;
    }

    public void setError(final String error) {
        this.error = error;
    }

    public Integer getFundVersionId() {
        return fundVersionId;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public void setInterrupted(final boolean interrupted) {
        this.interrupted = interrupted;
    }

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

    public void setResult(final Result result) {
        try {
            this.result = objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Problém při parsování JSON", e);
        }
    }

    public ArrOutputDefinition getOutputDefinition() {
        return outputDefinition;
    }

    public void setOutputDefinition(final ArrOutputDefinition outputDefinition) {
        this.outputDefinition = outputDefinition;
    }

    /**
     * Stav hromadné akce
     */
    public enum State {
        /**
         * Čekající
         */
        WAITING,
        /**
         * Naplánovaný
         */
        PLANNED,
        /**
         * Běžící
         */
        RUNNING,
        /**
         * Dokončená
         */
        FINISHED,
        /**
         * Chyba při běhu
         */
        ERROR,
        /**
         * Zrušená
         */
        INTERRUPTED,
        /**
         * Neplatný
         */
        OUTDATED;
    }
}