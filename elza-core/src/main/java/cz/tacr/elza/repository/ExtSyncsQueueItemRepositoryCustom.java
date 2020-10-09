package cz.tacr.elza.repository;

import cz.tacr.elza.controller.vo.ExtAsyncQueueState;
import cz.tacr.elza.domain.ExtSyncsQueueItem;

import java.util.List;

public interface ExtSyncsQueueItemRepositoryCustom {

    List<ExtSyncsQueueItem> findExtSyncsQueueItemsByExternalSystemAndScopesAndState(final String externalSystemCode,
                                                                                    final List<ExtAsyncQueueState> states,
                                                                                    final List<String> scopes,
                                                                                    final Integer firstResult,
                                                                                    final Integer maxResults);

}
