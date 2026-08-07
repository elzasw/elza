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

/**
 * The user's decision on one proposed change of an AI node-update proposal (an
 * {@code elza.nodeUpdateProposals} result block stored in
 * {@link AiRequest#getOutput()}). {@code changeKey} addresses the change within
 * the stored output; an APPLIED decision carries the {@link ArrChange} the
 * application created. Drives the card states in the AI panel and the
 * evaluation metric (acceptance rate per {@code AiRequest.promptVersion}).
 */
@Entity(name = "ai_proposal_decision")
@Table
public class AiProposalDecision {

    public static final String TABLE_NAME = "ai_proposal_decision";

    /** The user applied the change; {@code changeId} holds the created change. */
    public static final String STATE_APPLIED = "APPLIED";

    /** The user rejected the change; the description was not touched. */
    public static final String STATE_REJECTED = "REJECTED";

    @Id
    @GeneratedValue
    @Column(name = "ai_proposal_decision_id")
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer aiProposalDecisionId;

    /** Request whose proposals block the decision belongs to. */
    @Column(name = "ai_request_id", nullable = false)
    private Integer aiRequestId;

    /** Addresses the change within the request's stored output, e.g. {@code "0/2"}. */
    @Column(name = "change_key", length = StringLength.LENGTH_50, nullable = false)
    private String changeKey;

    /** {@link #STATE_APPLIED} or {@link #STATE_REJECTED}. */
    @Column(name = "state", length = StringLength.LENGTH_50, nullable = false)
    private String state;

    /** The versioned change created by an APPLIED decision ({@code arr_change}). */
    @Column(name = "change_id")
    private Integer changeId;

    /** User who decided. */
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "decide_date", nullable = false)
    private Date decideDate;

    public Integer getAiProposalDecisionId() {
        return aiProposalDecisionId;
    }

    public void setAiProposalDecisionId(final Integer aiProposalDecisionId) {
        this.aiProposalDecisionId = aiProposalDecisionId;
    }

    public Integer getAiRequestId() {
        return aiRequestId;
    }

    public void setAiRequestId(final Integer aiRequestId) {
        this.aiRequestId = aiRequestId;
    }

    public String getChangeKey() {
        return changeKey;
    }

    public void setChangeKey(final String changeKey) {
        this.changeKey = changeKey;
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public Integer getChangeId() {
        return changeId;
    }

    public void setChangeId(final Integer changeId) {
        this.changeId = changeId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(final Integer userId) {
        this.userId = userId;
    }

    public Date getDecideDate() {
        return decideDate;
    }

    public void setDecideDate(final Date decideDate) {
        this.decideDate = decideDate;
    }
}
