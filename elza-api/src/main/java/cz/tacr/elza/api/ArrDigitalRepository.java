package cz.tacr.elza.api;

/**
 * Uložiště digitalizátů.
 *
 * @author Martin Šlapa
 * @since 05. 12. 2016
 */
public interface ArrDigitalRepository extends SysExternalSystem {

    /**
     * @return url k dao
     */
    String getViewDaoUrl();

    /**
     * @param viewDaoUrl url k dao
     */
    void setViewDaoUrl(String viewDaoUrl);

    /**
     * @return url k souboru
     */
    String getViewFileUrl();

    /**
     * @param viewFileUrl url k souboru
     */
    void setViewFileUrl(String viewFileUrl);

    /**
     * @return odeslat notifikaci?
     */
    Boolean getSendNotification();

    /**
     * @param sendNotification odeslat notifikaci?
     */
    void setSendNotification(Boolean sendNotification);
}
