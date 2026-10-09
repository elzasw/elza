package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import cz.tacr.elza.api.AipType;
import cz.tacr.elza.api.DaAipActionItemState;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaLocalCache;
import cz.tacr.elza.domain.DaSyncQueueItem;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.DaLocalCacheRepository;

/**
 * The type of an AIP is read from the METS of its metadata package and stays known even when
 * the package cannot be processed further - it helps to tell which kind of packages fail.
 */
public class DaServicePackageTypeTest {

    private static final int AIP_ID = 21;

    private static final String METS = """
            <?xml version="1.0" encoding="UTF-8"?>
            <mets xmlns="http://www.loc.gov/METS/"
                  xmlns:xlink="http://www.w3.org/1999/xlink"
                  xmlns:csip="https://DILCIS.eu/XML/METS/CSIPExtensionMETS"
                  OBJID="8b58672e-7893-45c3-ab37-2b133389329d"
                  csip:CONTENTINFORMATIONTYPE="OTHER"
                  csip:OTHERCONTENTINFORMATIONTYPE="NSESSS"
                  PROFILE="https://stands.nacr.cz/da/2023/aip.xml">
              <amdSec>
                <digiprovMD ID="amd-1">
                  <mdRef LOCTYPE="URL" MDTYPE="PREMIS" xlink:type="simple" xlink:href="metadata/preservation/PREMIS.xml"/>
                </digiprovMD>
              </amdSec>
              <structMap TYPE="LOGICAL"><div/></structMap>
            </mets>
            """;

    @TempDir
    Path tempDir;

    @Test
    void typeIsKept_whenThePackageFailsAfterItsMetsWasRead() throws IOException {
        // METS only: the PREMIS file it refers to is missing, which fails after the METS was read
        Path zip = zip(Map.of("aip/METS.xml", METS));

        DaAip aip = new DaAip();
        aip.setAipId(AIP_ID);
        aip.setCode("aip-code");

        DaAipState aipState = new DaAipState();
        aipState.setAipStateId(7);
        aipState.setDaAip(aip);
        aipState.setAipVersion("1");
        aipState.setFund(new ArrFund());

        DaLocalCache localCache = new DaLocalCache();
        localCache.setLocalCacheId(3);
        localCache.setAipType(AipType.METADATA_BASE);
        localCache.setFilePath(zip.toString());

        AipRepository aipRepository = mock(AipRepository.class);
        AipStateRepository aipStateRepository = mock(AipStateRepository.class);
        DaLocalCacheRepository localCacheRepository = mock(DaLocalCacheRepository.class);
        when(aipRepository.findById(AIP_ID)).thenReturn(Optional.of(aip));
        when(aipStateRepository.findByDaAipAndDeleteChangeIsNull(aip)).thenReturn(aipState);
        when(aipStateRepository.findById(7)).thenReturn(Optional.of(aipState));
        when(aipStateRepository.save(any(DaAipState.class))).thenAnswer(inv -> inv.getArgument(0));
        when(localCacheRepository.findByAipStateAndAipTypeIn(eq(aipState), anyCollection(), anyCollection()))
                .thenReturn(localCache);

        DaService service = new DaService();
        setField(service, "aipRepository", aipRepository);
        setField(service, "aipStateRepository", aipStateRepository);
        setField(service, "daLocalCacheRepository", localCacheRepository);
        setField(service, "referenceResolver", mock(DaAipReferenceResolver.class));
        setField(service, "txManager", mock(PlatformTransactionManager.class));

        Map<Integer, String> failed = new ConcurrentHashMap<>();
        service.doCreateDaoStructure(List.of(AIP_ID), new AipOutcomeSink() {
            @Override
            public void record(Integer aipId, DaAipActionItemState state, @Nullable String message) {
                if (state == DaAipActionItemState.ERROR) {
                    failed.put(aipId, message);
                }
            }

            @Override
            public void enqueued(Integer aipId, DaSyncQueueItem queueItem) {
            }
        });

        assertTrue(failed.get(AIP_ID).contains("PREMIS.xml"), failed.toString());
        assertEquals("NSESSS", aipState.getContentType());
        assertEquals("https://stands.nacr.cz/da/2023/aip.xml", aipState.getProfile());
    }

    private Path zip(Map<String, String> entries) throws IOException {
        Path zip = tempDir.resolve("package.zip");
        try (OutputStream os = Files.newOutputStream(zip); ZipOutputStream zos = new ZipOutputStream(os)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return zip;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
