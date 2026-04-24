package cz.tacr.elza.cam.v2;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.cam.v2.client.ApiException;
import cz.tacr.cam.v2.client.controller.vo.BatchUpdateStatus;
import cz.tacr.cam.v2.schema.cam.BatchChangeFailureXml;
import cz.tacr.cam.v2.schema.cam.BatchChangeSuccessXml;
import cz.tacr.cam.v2.schema.cam.BatchUpdateResultXml;
import cz.tacr.cam.v2.schema.cam.EntityIssuesXml;
import cz.tacr.cam.v2.schema.cam.ExistingIssueXml;
import cz.tacr.cam.v2.schema.cam.IssueSeverityXml;
import cz.tacr.elza.cam.ItemSyncProcessor;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;
import cz.tacr.elza.service.AccessPointConnectorService;

@Component
@Scope("prototype")
public class ItemSyncExportConfirmProcessor implements ItemSyncProcessor {

    static private final Logger log = LoggerFactory.getLogger(ItemSyncExportConfirmProcessor.class);

    @Autowired
    private CamService camService;

    @Autowired
    private AccessPointConnectorService apConnectService;

    @Autowired
    private ObjectMapper objectMapper;

    private ExtSyncsQueueItem queueItem;

    public ItemSyncExportConfirmProcessor(ExtSyncsQueueItem queueItem) {
        this.queueItem = queueItem;
    }

    @Override
    public boolean process() {
		synchronized (camService) {
			log.debug("Get upload status of queue item, id: {}, accessPointId: {}", queueItem.getExtSyncsQueueItemId(), queueItem.getAccessPointId());

			try {
				BatchUpdateResultXml batchUpdateResult;
				BatchUpdateStatus status = camService.getBatchStatus(queueItem);
				log.debug("Get status from CAM: {}", status.getState());
				switch (status.getState()) {
				case FINISHED:
					batchUpdateResult = camService.getBatchUpdateResult(queueItem);
					if (batchUpdateResult instanceof BatchChangeSuccessXml) {
						// Persist bindings using the uuid map captured at upload time —
						// these are the UUIDs that actually went on the wire to CAM, so the
						// stored bindings stay consistent with CAM's state.
						camService.confirmBatchSuccess(queueItem, (BatchChangeSuccessXml) batchUpdateResult);
						apConnectService.setQueueItemStateTA(queueItem, ExtAsyncQueueState.EXPORT_OK);
					} else {
						processBatchChangeFailureInCaseFinished((BatchChangeFailureXml) batchUpdateResult);
					}
					break;
				case ERROR:
					batchUpdateResult = camService.getBatchUpdateResult(queueItem);
					BatchChangeFailureXml failure = (BatchChangeFailureXml) batchUpdateResult;
					IssueRefResolver errorResolver = camService.createIssueRefResolver(queueItem, failure);
					List<ApIssue> issueList = ApIssue.createList(failure, errorResolver);
					String errorStateMessage = objectMapper.writeValueAsString(issueList);
					log.error("Failed to send item, accessPointId: {}, message: {}", queueItem.getAccessPointId(), issueList);
					apConnectService.setQueueItemStateTA(queueItem, ExtAsyncQueueState.ERROR, errorStateMessage, queueItem.getBatchId(), null, null);
					break;
				case PROCESSING:
					// TODO
				case PENDING:
					// TODO
				}
			} catch (ApiException e) {
				// if ApiException -> it means we connected server and it is logical failure
				log.error("Failed to synchronize items, code: {}, body: {}", e.getCode(), e.getResponseBody(), e);
				apConnectService.setQueueItemStateTA(queueItem, ExtAsyncQueueState.ERROR, CamException.getApiExceptionInfo(e));
			} catch (JsonProcessingException e) {
				log.error("Failed to serialize items: {}", e.getMessage(), e);
				apConnectService.setQueueItemStateTA(queueItem, ExtAsyncQueueState.ERROR, e.getMessage());
			}
		}

		return true;
	}

    private void processBatchChangeFailureInCaseFinished(BatchChangeFailureXml failure) throws JsonProcessingException {
		IssueRefResolver resolver = camService.createIssueRefResolver(queueItem, failure);
		List<ApIssue> issueList = ApIssue.createList(failure, resolver);
		String stateMessage = objectMapper.writeValueAsString(issueList);
		ExtAsyncQueueState queueState = getQueueState(failure);
		String forceKey = failure.getForceKey() != null ? failure.getForceKey().getValue() : null;

		log.error("Failed to send item, accessPointId: {}, message: {}", queueItem.getAccessPointId(), issueList);
		apConnectService.setQueueItemStateTA(queueItem, queueState, stateMessage, queueItem.getBatchId(), queueItem.getData(), forceKey);
    }

    private ExtAsyncQueueState getQueueState(BatchChangeFailureXml failure) {
		for (EntityIssuesXml entityIssue : failure.getIssues()) {
			for (ExistingIssueXml issue : entityIssue.getIssue()) {
				if (issue.getSeverity().equals(IssueSeverityXml.ERROR)) {
					return ExtAsyncQueueState.ERROR;
				}
			}
		}
		return ExtAsyncQueueState.EXPORT_NEED_CONFIRM;
    }
}
