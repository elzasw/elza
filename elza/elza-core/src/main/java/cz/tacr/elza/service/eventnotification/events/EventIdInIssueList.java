package cz.tacr.elza.service.eventnotification.events;

import java.util.Set;


/**
 * Událost nesoucí seznam ovlivněných id.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.01.2016
 */
public class EventIdInIssueList extends EventId {

    private Integer issueListId;

    public EventIdInIssueList(final EventType eventType, final Integer issueListId, final Integer... ids) {
        super(eventType, ids);
        this.issueListId = issueListId;
    }

    public EventIdInIssueList(final EventType eventType, final Integer issueListId, final Set<Integer> ids) {
        super(eventType, ids);
        this.issueListId = issueListId;
    }

    public Integer getIssueListId() {
        return issueListId;
    }

    public void setIssueListId(final Integer issueListId) {
        this.issueListId = issueListId;
    }
}
