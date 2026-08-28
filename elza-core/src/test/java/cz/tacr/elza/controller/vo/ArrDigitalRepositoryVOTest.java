package cz.tacr.elza.controller.vo;

import org.junit.jupiter.api.Test;

import cz.tacr.elza.api.DaDownloadMethod;
import cz.tacr.elza.api.DaOnReceivedAction;
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.domain.ArrDigitalRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArrDigitalRepositoryVOTest {

    private static ArrDigitalRepositoryVO createVO(DigitalRepositoryType type) {
        ArrDigitalRepositoryVO vo = new ArrDigitalRepositoryVO();
        vo.setCode("REPO");
        vo.setName("Repository");
        vo.setUrl("/opt/repo");
        vo.setElzaCode("ELZA-REPO");
        vo.setUsername("user");
        vo.setPassword("secret");
        vo.setDigitalRepositoryType(type);
        vo.setViewDaoUrl("http://viewer/dao/{code}");
        vo.setViewFileUrl("http://viewer/file/{code}");
        vo.setViewThumbnailUrl("http://viewer/thumb/{code}");
        vo.setSendNotification(true);
        vo.setMultipleLinks(true);
        vo.setDownloadMethod(DaDownloadMethod.FILE_TRANSFER);
        vo.setOnReceived(DaOnReceivedAction.DOWNLOAD_METADATA);
        vo.setSyncDelay(3600);
        return vo;
    }

    @Test
    void createEntity_filesystem_clearsSettingsOfExternalRepository() {
        ArrDigitalRepository entity = (ArrDigitalRepository) createVO(DigitalRepositoryType.FILESYSTEM)
                .createEntity(null);

        assertNull(entity.getViewDaoUrl());
        assertNull(entity.getViewFileUrl());
        assertNull(entity.getViewThumbnailUrl());
        assertNull(entity.getUsername());
        assertNull(entity.getPassword());
        assertFalse(entity.getSendNotification());
        // settings a filesystem repository does use
        assertEquals("/opt/repo", entity.getUrl());
        // download and synchronization settings apply to DA repositories only
        assertEquals(DaDownloadMethod.STANDARD, entity.getDownloadMethod());
        assertEquals(DaOnReceivedAction.NONE, entity.getOnReceived());
        assertEquals(Integer.valueOf(ArrDigitalRepository.DEFAULT_SYNC_DELAY), entity.getSyncDelay());
        assertEquals("ELZA-REPO", entity.getElzaCode());
        assertTrue(entity.getMultipleLinks());
        assertEquals("REPO", entity.getCode());
        assertEquals("Repository", entity.getName());
    }

    @Test
    void createEntity_otherType_keepsAllSettings() {
        ArrDigitalRepository entity = (ArrDigitalRepository) createVO(DigitalRepositoryType.WSDL)
                .createEntity(null);

        assertEquals("http://viewer/dao/{code}", entity.getViewDaoUrl());
        assertEquals("http://viewer/file/{code}", entity.getViewFileUrl());
        assertEquals("http://viewer/thumb/{code}", entity.getViewThumbnailUrl());
        assertEquals("user", entity.getUsername());
        assertEquals("secret", entity.getPassword());
        assertTrue(entity.getSendNotification());
        assertEquals(DaDownloadMethod.STANDARD, entity.getDownloadMethod());
        assertEquals(DaOnReceivedAction.NONE, entity.getOnReceived());
    }

    @Test
    void createEntity_da_keepsDownloadSettings() {
        ArrDigitalRepository entity = (ArrDigitalRepository) createVO(DigitalRepositoryType.DA)
                .createEntity(null);

        assertEquals(DaDownloadMethod.FILE_TRANSFER, entity.getDownloadMethod());
        assertEquals(DaOnReceivedAction.DOWNLOAD_METADATA, entity.getOnReceived());
        assertEquals(Integer.valueOf(3600), entity.getSyncDelay());
    }

    @Test
    void createEntity_da_missingDownloadSettingsFallBackToDefaults() {
        ArrDigitalRepositoryVO vo = createVO(DigitalRepositoryType.DA);
        vo.setDownloadMethod(null);
        vo.setOnReceived(null);
        vo.setSyncDelay(null);

        ArrDigitalRepository entity = (ArrDigitalRepository) vo.createEntity(null);

        assertEquals(DaDownloadMethod.STANDARD, entity.getDownloadMethod());
        assertEquals(DaOnReceivedAction.NONE, entity.getOnReceived());
        assertEquals(Integer.valueOf(ArrDigitalRepository.DEFAULT_SYNC_DELAY), entity.getSyncDelay());
    }

    @Test
    void createEntity_da_zeroSyncDelayDisablesSynchronization() {
        ArrDigitalRepositoryVO vo = createVO(DigitalRepositoryType.DA);
        vo.setSyncDelay(0);

        ArrDigitalRepository entity = (ArrDigitalRepository) vo.createEntity(null);

        assertEquals(Integer.valueOf(0), entity.getSyncDelay());
    }

    @Test
    void newInstance_copiesDownloadSettings() {
        ArrDigitalRepository entity = (ArrDigitalRepository) createVO(DigitalRepositoryType.DA)
                .createEntity(null);

        ArrDigitalRepositoryVO vo = ArrDigitalRepositoryVO.newInstance(entity);

        assertEquals(DaDownloadMethod.FILE_TRANSFER, vo.getDownloadMethod());
        assertEquals(DaOnReceivedAction.DOWNLOAD_METADATA, vo.getOnReceived());
        assertEquals(Integer.valueOf(3600), vo.getSyncDelay());
    }

    @Test
    void createEntity_filesystem_stripsFileUriPrefixFromUrl() {
        ArrDigitalRepositoryVO vo = createVO(DigitalRepositoryType.FILESYSTEM);
        vo.setUrl("file:///opt/repo");

        ArrDigitalRepository entity = (ArrDigitalRepository) vo.createEntity(null);

        assertEquals("/opt/repo", entity.getUrl());
    }
}
