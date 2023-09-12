package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;

import java.util.List;

public interface ExtSyncsQueueItemRepositoryCustom {

    List<ExtSyncsQueueItem> findExtSyncsQueueItemsByExternalSystemAndScopesAndState(final String externalSystemCode,
                                                                                    final List<ExtAsyncQueueState> states,
                                                                                    final List<ApScope> scopes,
                                                                                    final Integer firstResult,
                                                                                    final Integer maxResults);

}
