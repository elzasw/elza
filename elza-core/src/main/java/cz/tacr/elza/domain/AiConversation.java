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
 * One AI topic as the user sees it in the UI: a thread with a context (fund
 * version, fund list, access point, …), an owner and an ordered sequence of
 * {@link AiRequest} exchanges with one AI provider.
 */
@Entity(name = "ai_conversation")
@Table
public class AiConversation {

    public static final String TABLE_NAME = "ai_conversation";

    @Id
    @GeneratedValue
    @Column(name = "ai_conversation_id")
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer aiConversationId;

    /** AI provider external system the conversation runs against. */
    @Column(name = "external_system_id", nullable = false)
    private Integer externalSystemId;

    /** Owner of the conversation. */
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "title", length = StringLength.LENGTH_250, nullable = false)
    private String title;

    /** Context type code: {@code fund-version}, {@code fund-list}, {@code access-point}, … */
    @Column(name = "context_type", length = StringLength.LENGTH_50)
    private String contextType;

    /** Context detail (JSON): ids, filter, selection — shape depends on contextType. */
    @Lob
    @Column(name = "context")
    private String context;

    @Column(name = "create_date", nullable = false)
    private Date createDate;

    @Column(name = "last_change_date", nullable = false)
    private Date lastChangeDate;

    public Integer getAiConversationId() {
        return aiConversationId;
    }

    public void setAiConversationId(Integer aiConversationId) {
        this.aiConversationId = aiConversationId;
    }

    public Integer getExternalSystemId() {
        return externalSystemId;
    }

    public void setExternalSystemId(Integer externalSystemId) {
        this.externalSystemId = externalSystemId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContextType() {
        return contextType;
    }

    public void setContextType(String contextType) {
        this.contextType = contextType;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getLastChangeDate() {
        return lastChangeDate;
    }

    public void setLastChangeDate(Date lastChangeDate) {
        this.lastChangeDate = lastChangeDate;
    }
}
