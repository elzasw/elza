package cz.tacr.elza.service.report;

import jakarta.persistence.EntityManager;

public abstract class ReportBase implements ReportProcessor {

	protected final EntityManager em;

	protected final ReportService reportService;

	public ReportBase(EntityManager em, ReportService reportService) {
		this.em = em;
		this.reportService = reportService;
	}

}
