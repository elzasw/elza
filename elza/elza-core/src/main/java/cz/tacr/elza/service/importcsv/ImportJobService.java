package cz.tacr.elza.service.importcsv;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import cz.tacr.elza.controller.vo.ImportJobStatus;

@Service
public class ImportJobService {

    private static final Logger logger = LoggerFactory.getLogger(ImportJobService.class);

    private static final Duration TTL = Duration.ofHours(24);

    private final Map<UUID, ImportJob> jobs = new ConcurrentHashMap<>();

    public UUID create(Integer fundId, Integer initiatorId) {
        UUID id = UUID.randomUUID();
        jobs.put(id, new ImportJob(id, fundId, initiatorId));
        return id;
    }

    public void markCompleted(UUID jobId) {
        ImportJob job = jobs.get(jobId);
        if (job != null) {
            job.markCompleted();
        }
    }

    public void markFailed(UUID jobId, String error) {
        ImportJob job = jobs.get(jobId);
        if (job != null) {
            job.markFailed(error);
        }
    }

    public Optional<ImportJob> get(UUID jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    /** Odstranění dokončených úloh starších než {@value #TTL_HOURS} hodin. */
    @Scheduled(fixedDelay = 30 * 60 * 1000L)   // každých 30 min
    void purgeExpired() {
        Instant cutoff = Instant.now().minus(TTL);
        int before = jobs.size();
        jobs.entrySet().removeIf(e -> {
            Instant fin = e.getValue().getFinishedAt();
            return fin != null && fin.isBefore(cutoff);
        });
        int removed = before - jobs.size();
        if (removed > 0) {
            logger.debug("Purged {} expired import jobs (retained {})", removed, jobs.size());
        }
    }
    
    public ImportJobStatus toVo(ImportJob job) {
        ImportJobStatus vo = new ImportJobStatus();
        vo.setJobId(job.getJobId().toString());
        vo.setFundId(job.getFundId());
        vo.setState(job.getState());
        vo.setError(job.getError());
        vo.setStartedAt(job.getStartedAt().atOffset(java.time.ZoneOffset.UTC));
        if (job.getFinishedAt() != null) {
            vo.setFinishedAt(job.getFinishedAt().atOffset(java.time.ZoneOffset.UTC));
        }
        return vo;
    }    
}