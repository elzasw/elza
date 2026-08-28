package cz.tacr.elza.domain;

import cz.tacr.elza.api.DaDownloadMethod;
import cz.tacr.elza.api.DaOnReceivedAction;
import cz.tacr.elza.api.DigitalRepositoryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Uložiště digitalizátů.
 *
 * @author Martin Šlapa
 * @since 05. 12. 2016
 */
@Entity(name = "arr_digital_repository")
@Table
public class ArrDigitalRepository extends SysExternalSystem {

    /** Default interval of the synchronization with a DA repository, in seconds (5 minutes). */
    public static final int DEFAULT_SYNC_DELAY = 300;

    @Column(length = StringLength.LENGTH_1000)
    private String viewDaoUrl;

    @Column(length = StringLength.LENGTH_1000)
    private String viewFileUrl;

    @Column(length = StringLength.LENGTH_1000)
    private String viewThumbnailUrl;

    @Column(nullable = false)
    private Boolean sendNotification;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM, nullable = false)
    private DigitalRepositoryType digitalRepositoryType;

    @Column(nullable = false)
    private Boolean multipleLinks = Boolean.FALSE;

    /**
     * How AIP packages are downloaded from a DA repository.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM, nullable = false)
    private DaDownloadMethod downloadMethod = DaDownloadMethod.STANDARD;

    /**
     * Automatic action when a DA repository reports a newly received AIP.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM, nullable = false)
    private DaOnReceivedAction onReceived = DaOnReceivedAction.NONE;

    /**
     * Seconds between two synchronizations with a DA repository; 0 = no synchronization.
     */
    @Column(nullable = false)
    private Integer syncDelay = DEFAULT_SYNC_DELAY;

    public ArrDigitalRepository() {
    }

    public ArrDigitalRepository(ArrDigitalRepository ardr) {
		super(ardr);
		this.viewDaoUrl = ardr.getViewDaoUrl();
		this.viewFileUrl = ardr.getViewFileUrl();
		this.viewThumbnailUrl = ardr.getViewThumbnailUrl();
		this.sendNotification = ardr.getSendNotification();
		this.digitalRepositoryType = ardr.getDigitalRepositoryType();
		this.multipleLinks = ardr.getMultipleLinks();
		this.downloadMethod = ardr.getDownloadMethod();
		this.onReceived = ardr.getOnReceived();
		this.syncDelay = ardr.getSyncDelay();
    }

	/**
     * @return url k dao
     */
    public String getViewDaoUrl() {
        return viewDaoUrl;
    }

    /**
     * @param viewDaoUrl url k dao
     */
    public void setViewDaoUrl(final String viewDaoUrl) {
        this.viewDaoUrl = viewDaoUrl;
    }

    /**
     * @return url k souboru
     */
    public String getViewFileUrl() {
        return viewFileUrl;
    }

    /**
     * @param viewFileUrl url k souboru
     */
    public void setViewFileUrl(final String viewFileUrl) {
        this.viewFileUrl = viewFileUrl;
    }

    /**
     * @return url k náhledu
     */
    public String getViewThumbnailUrl() {
        return viewThumbnailUrl;
    }

    /**
     * @param viewThumbnailUrl k náhledu
     */
    public void setViewThumbnailUrl(String viewThumbnailUrl) {
        this.viewThumbnailUrl = viewThumbnailUrl;
    }

    /**
     * @return odeslat notifikaci?
     */
    public Boolean getSendNotification() {
        return sendNotification;
    }

    /**
     * @param sendNotification odeslat notifikaci?
     */
    public void setSendNotification(final Boolean sendNotification) {
        this.sendNotification = sendNotification;
    }

    public DigitalRepositoryType getDigitalRepositoryType() {
        return digitalRepositoryType;
    }

    public void setDigitalRepositoryType(DigitalRepositoryType digitalRepositoryType) {
        this.digitalRepositoryType = digitalRepositoryType;
    }

    public Boolean getMultipleLinks() {
		return multipleLinks;
	}

	public void setMultipleLinks(Boolean multipleLinks) {
		this.multipleLinks = multipleLinks;
	}

    public DaDownloadMethod getDownloadMethod() {
        return downloadMethod;
    }

    public void setDownloadMethod(DaDownloadMethod downloadMethod) {
        this.downloadMethod = downloadMethod;
    }

    public DaOnReceivedAction getOnReceived() {
        return onReceived;
    }

    public void setOnReceived(DaOnReceivedAction onReceived) {
        this.onReceived = onReceived;
    }

    public Integer getSyncDelay() {
        return syncDelay;
    }

    public void setSyncDelay(Integer syncDelay) {
        this.syncDelay = syncDelay;
    }

    @Override
    public String toString() {
        return "ArrDigitalRepository pk=" + getExternalSystemId();
    }
}
