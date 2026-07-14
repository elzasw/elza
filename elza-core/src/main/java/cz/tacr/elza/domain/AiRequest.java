package cz.tacr.elza.domain;

import java.util.Date;

import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One exchange with an AI provider (one submitted task) within an
 * {@link AiConversation}: the user's instructions in, one output back, one
 * usage record. Tool-loop turns inside a task stay provider-side (Elza only
 * relays them). Kept independently of the provider — it drives the user-facing
 * history, the audit trail and the local usage/cost ledger (the provider
 * deletes conversation payloads after its retention window). Stores the output
 * (what the user saw) and usage, not the full input payload (only its
 * parameters).
 */
@Entity(name = "ai_request")
@Table
public class AiRequest {

    public static final String TABLE_NAME = "ai_request";

    @Id
    @GeneratedValue
    @Column(name = "ai_request_id")
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer aiRequestId;

    /** Conversation this exchange belongs to. */
    @Column(name = "ai_conversation_id", nullable = false)
    private Integer aiConversationId;

    /** Provider-assigned task id (opaque). */
    @Column(name = "task_uid", length = StringLength.LENGTH_50)
    private String taskUid;

    /** Client idempotency key sent with the submission. */
    @Column(name = "request_id", length = StringLength.LENGTH_250, nullable = false)
    private String requestId;

    /** Task type, e.g. {@code elza.echo}, {@code elza.revision}. */
    @Column(name = "task_type", length = StringLength.LENGTH_250, nullable = false)
    private String taskType;

    /** Last known task state (wire value, e.g. {@code done}, {@code error}). */
    @Column(name = "state", length = StringLength.LENGTH_50, nullable = false)
    private String state;

    /** Version of the provider-side prompt used (for reproducibility/audit). */
    @Column(name = "prompt_version", length = StringLength.LENGTH_250)
    private String promptVersion;

    /** Profile/model requested for this exchange (an {@code AiProfile.code}); null = provider default. */
    @Column(name = "profile", length = StringLength.LENGTH_250)
    private String profile;

    /** The user's message of this exchange (rendered in the thread). */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "user_instructions")
    private String userInstructions;

    /** Structured task parameters (JSON) — checks, node ids; not the full payload. */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "parameters")
    private String parameters;

    /** Task output (JSON) as returned by the provider; what the user saw. */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "output")
    private String output;

    @Column(name = "error_code", length = StringLength.LENGTH_50)
    private String errorCode;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "error_message")
    private String errorMessage;

    /**
     * Short human-readable phase of a running task, mirrored from the
     * provider's advisory {@code Task.progress}; null once terminal.
     */
    @Column(name = "progress_message", length = StringLength.LENGTH_1000)
    private String progressMessage;

    /** Rough completion estimate 0–100 (advisory); null once terminal. */
    @Column(name = "progress_percent")
    private Double progressPercent;

    /** Model input tokens consumed (informative). */
    @Column(name = "input_tokens", nullable = false)
    private long inputTokens;

    /** Model output tokens produced (informative). */
    @Column(name = "output_tokens", nullable = false)
    private long outputTokens;

    /** Billable cost units per the provider's price list. */
    @Column(name = "cost_units", nullable = false)
    private double costUnits;

    /**
     * Credits charged to the account the signing key selects
     * ({@code costUnits × multiplier} — a shared organizational account may
     * burn credits faster). What the user sees as the price of the exchange.
     */
    @Column(name = "charged_credits", nullable = false)
    private double chargedCredits;

    @Column(name = "create_date", nullable = false)
    private Date createDate;

    @Column(name = "finish_date")
    private Date finishDate;

    public Integer getAiRequestId() {
        return aiRequestId;
    }

    public void setAiRequestId(Integer aiRequestId) {
        this.aiRequestId = aiRequestId;
    }

    public Integer getAiConversationId() {
        return aiConversationId;
    }

    public void setAiConversationId(Integer aiConversationId) {
        this.aiConversationId = aiConversationId;
    }

    public String getTaskUid() {
        return taskUid;
    }

    public void setTaskUid(String taskUid) {
        this.taskUid = taskUid;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getUserInstructions() {
        return userInstructions;
    }

    public void setUserInstructions(String userInstructions) {
        this.userInstructions = userInstructions;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getProgressMessage() {
        return progressMessage;
    }

    public void setProgressMessage(String progressMessage) {
        this.progressMessage = progressMessage;
    }

    public Double getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Double progressPercent) {
        this.progressPercent = progressPercent;
    }

    public long getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(long inputTokens) {
        this.inputTokens = inputTokens;
    }

    public long getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(long outputTokens) {
        this.outputTokens = outputTokens;
    }

    public double getCostUnits() {
        return costUnits;
    }

    public void setCostUnits(double costUnits) {
        this.costUnits = costUnits;
    }

    public double getChargedCredits() {
        return chargedCredits;
    }

    public void setChargedCredits(double chargedCredits) {
        this.chargedCredits = chargedCredits;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(Date finishDate) {
        this.finishDate = finishDate;
    }
}
