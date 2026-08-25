package cz.tacr.elza.service.importcsv;

import java.time.Instant;
import java.util.UUID;

import cz.tacr.elza.controller.vo.ImportJobState;

public final class ImportJob {

	private final UUID jobId;
	private final Integer fundId;
	private final Integer initiatorId; // null pro admina bez id
	private final Instant startedAt;

	private volatile ImportJobState state;
	private volatile String error;
	private volatile Instant finishedAt;

	public ImportJob(UUID jobId, Integer fundId, Integer initiatorId) {
		this.jobId = jobId;
		this.fundId = fundId;
		this.initiatorId = initiatorId;
		this.startedAt = Instant.now();
		this.state = ImportJobState.RUNNING;
	}

	public UUID getJobId() {
		return jobId;
	}

	public Integer getFundId() {
		return fundId;
	}

	public Integer getInitiatorId() {
		return initiatorId;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public ImportJobState getState() {
		return state;
	}

	public String getError() {
		return error;
	}

	public Instant getFinishedAt() {
		return finishedAt;
	}

	void markCompleted() {
		this.state = ImportJobState.COMPLETED;
		this.finishedAt = Instant.now();
	}

	void markFailed(String error) {
		this.state = ImportJobState.FAILED;
		this.error = error;
		this.finishedAt = Instant.now();
	}
}
