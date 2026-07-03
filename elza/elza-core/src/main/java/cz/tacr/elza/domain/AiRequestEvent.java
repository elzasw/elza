package cz.tacr.elza.domain;

import java.util.Date;

import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * One step of an {@link AiRequest} lifecycle with its payload — the exchange
 * log of the request. Serves debugging and user-facing transparency (what
 * exactly was sent to the provider / what came back); a future confirmation
 * workflow can extend it additively.
 */
@Entity(name = "ai_request_event")
@Table
public class AiRequestEvent {

    public static final String TABLE_NAME = "ai_request_event";

    /** Event types; an open set — new steps are added as new codes. */
    public static final String TYPE_SUBMIT = "SUBMIT";
    public static final String TYPE_TOOL_CALLS = "TOOL_CALLS";
    public static final String TYPE_TOOL_RESULTS = "TOOL_RESULTS";
    public static final String TYPE_OUTPUT = "OUTPUT";
    public static final String TYPE_ERROR = "ERROR";
    public static final String TYPE_CANCEL = "CANCEL";

    @Id
    @GeneratedValue
    @Column(name = "ai_request_event_id")
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer aiRequestEventId;

    /** Request this event belongs to. */
    @Column(name = "ai_request_id", nullable = false)
    private Integer aiRequestId;

    /** Event type, e.g. {@link #TYPE_SUBMIT}. */
    @Column(name = "event_type", length = StringLength.LENGTH_50, nullable = false)
    private String eventType;

    /** Event payload (JSON); shape depends on the event type. */
    @Lob
    @Column(name = "data")
    private String data;

    @Column(name = "create_date", nullable = false)
    private Date createDate;

    public Integer getAiRequestEventId() {
        return aiRequestEventId;
    }

    public void setAiRequestEventId(Integer aiRequestEventId) {
        this.aiRequestEventId = aiRequestEventId;
    }

    public Integer getAiRequestId() {
        return aiRequestId;
    }

    public void setAiRequestId(Integer aiRequestId) {
        this.aiRequestId = aiRequestId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
}
