package cz.tacr.elza.domain;

import java.io.IOException;
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
public class ArrBulkActionRun {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Id
    @GeneratedValue
    private Integer bulkActionRunId;

    @Column(nullable = false)
    private String bulkActionCode;

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

    /**
     * Vrací identifikátor záznamu.
     *
     * @return identifikátor záznamu
     */
    public Integer getBulkActionRunId() {
        return bulkActionRunId;
    }

    /**
     * Nastaví identifikátor záznamu.
     *
     * @param bulkActionId identifikátor záznamu
     */
    public void setBulkActionRunId(final Integer bulkActionId) {
        this.bulkActionRunId = bulkActionId;
    }

    /**
     * Vrací kód hromadné akce.
     *
     * @return kód hromadné akce
     */
    public String getBulkActionCode() {
        return bulkActionCode;
    }

    /**
     * Nastaví kód hromadné akce.
     *
     * @param bulkActionCode kód hromadné akce
     */
    public void setBulkActionCode(final String bulkActionCode) {
        this.bulkActionCode = bulkActionCode;
    }

    /**
     * Vrací verzi archivní pomůcky.
     *
     * @return verze archivní pomůcky
     */
    public ArrFundVersion getFundVersion() {
        return fundVersion;
    }

    /**
     * Nastavuje verzi archivní pomůcky.
     *
     * @param fundVersion verze archivní pomůcky
     */
    public void setFundVersion(final ArrFundVersion fundVersion) {
        this.fundVersion = fundVersion;
        this.fundVersionId = fundVersion.getFundVersionId();
    }

    /**
     * Vrátí user id.
     *
     * @return user id
     */
    public Integer getUserId() {
        return userId;
    }

    /**
     * Nastaví user id.
     *
     * @param userId user id
     */
    public void setUserId(final Integer userId) {
        this.userId = userId;
    }

    /**
     * Vrací změnu se kterou běžela hromadná akce.
     *
     * @return změna change
     */
    public ArrChange getChange() {
        return change;
    }

    /**
     * Nastavuje změnu se kterou běžela hromadná akce.
     *
     * @param change změna
     */
    public void setChange(final ArrChange change) {
        this.change = change;
    }

    /**
     * Vrátí stav hromadné akce
     *
     * @return stav state
     */
    public State getState() {
        return state;
    }

    /**
     * Nastaví stav hromadné akce
     *
     * @param state stav
     */
    public void setState(final State state) {
        this.state = state;
    }

    /**
     * Vrátí datum dokončení hromadné akce
     *
     * @return datum date finished
     */
    public Date getDateFinished() {
        return dateFinished;
    }

    /**
     * Nastavuje datum kdy byla dokončena hromadná akce
     *
     * @param dateFinished datum
     */
    public void setDateFinished(final Date dateFinished) {
        this.dateFinished = dateFinished;
    }

    /**
     * Vrátí datum startu hromadné akce
     *
     * @return datum date started
     */
    public Date getDateStarted() {
        return dateStarted;
    }

    /**
     * Nastavuje datum kdy byla spuštěna hromadná akce
     *
     * @param dateStarted datum
     */
    public void setDateStarted(final Date dateStarted) {
        this.dateStarted = dateStarted;
    }

    /**
     * Vrátí datum naplánování hromadné akce
     *
     * @return datum date planed
     */
    public Date getDatePlanned() {
        return datePlanned;
    }

    /**
     * Nastavuje datum kdy byla naplánována hromadná akce
     *
     * @param datePlaned datum
     */
    public void setDatePlanned(final Date datePlaned) {
        this.datePlanned = datePlaned;
    }

    /**
     * Vrátí chybu které nastala při běhu hromadné
     *
     * @return String log chyby
     */
    public String getError() {
        return error;
    }

    /**
     * Nastaví chybu které nastala při běhu hromadné akce
     *
     * @param error string log chyba
     */
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

    /**
     * Vazba na bulk action nodes
     *
     * @return množina, může být prázdná.
     */
    public List<ArrBulkActionNode> getArrBulkActionNodes() {
        return arrBulkActionNodes;
    }

    /**
     * Vazba na bulk action nodes
     *
     * @param arrBulkActionNodes množina záznamů.
     */
    public void setArrBulkActionNodes(final List<ArrBulkActionNode> arrBulkActionNodes) {
        this.arrBulkActionNodes = arrBulkActionNodes;
    }

    /**
     * @return výsledek hromadné akce
     */
    public Result getResult() {
        if (this.result == null) {
            return null;
        }
        try {
            return objectMapper.readValue(this.result, new TypeReference<Result>(){});
        } catch (IOException e) {
            throw new IllegalArgumentException("Error while reading JSON, value: " + result, e);
        }
    }

    /**
     * @param result výsledek hromadné akce
     */
    public void setResult(final Result result) {
        try {
            this.result = objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error while writing JSON", e);
        }
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
