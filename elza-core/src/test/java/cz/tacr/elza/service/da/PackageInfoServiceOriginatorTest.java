package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaChange;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.DaChangeRepository;

/**
 * The originator of a package is the agent linked to the creation event. It is kept by name; the
 * CAM identifier the package may give it is kept as received and resolved to an access point
 * with the other references - archives that do not describe their originators in CAM send the
 * agent without it.
 */
public class PackageInfoServiceOriginatorTest {

    private static final String CODE = "329740";

    @TempDir
    Path tempDir;

    private PackageInfoService service;
    private AipRepository aipRepository;
    private AipStateRepository aipStateRepository;
    private DaAipReferenceResolver referenceResolver;
    private ArrDigitalRepository repository;

    @BeforeEach
    void setUp() {
        aipRepository = mock(AipRepository.class);
        aipStateRepository = mock(AipStateRepository.class);
        DaChangeRepository changeRepository = mock(DaChangeRepository.class);
        referenceResolver = mock(DaAipReferenceResolver.class);
        when(aipRepository.save(any(DaAip.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aipStateRepository.save(any(DaAipState.class))).thenAnswer(inv -> inv.getArgument(0));
        when(changeRepository.save(any(DaChange.class))).thenAnswer(inv -> inv.getArgument(0));

        service = new PackageInfoService();
        setField(service, "aipRepository", aipRepository);
        setField(service, "aipStateRepository", aipStateRepository);
        setField(service, "changeRepository", changeRepository);
        setField(service, "referenceResolver", referenceResolver);
        setField(service, "linkStateResolver", mock(DaAipLinkStateResolver.class));

        repository = new ArrDigitalRepository();
        repository.setExternalSystemId(3);
        repository.setDigitalRepositoryType(DigitalRepositoryType.DA);
    }

    /** What the ESM of Charles University sends: the originator carries a local identifier only. */
    @Test
    void originatorWithoutCamIdentifierIsKeptByName() throws Exception {
        DaAipState state = service.processPackageInfo(repository, packageInfo(""));

        assertEquals("Univerzita Karlova. 1. lékařská fakulta", state.getOriginator());
        assertNull(state.getOriginatorCamId());
        assertNull(state.getOriginatorAccessPoint());
    }

    /** The identifier is handed to the reference resolution, which is where it is looked up. */
    @Test
    void camIdentifierIsKeptForTheReferenceResolution() throws Exception {
        DaAipState state = service.processPackageInfo(repository, packageInfo(
                "<agentIdentifier><agentIdentifierType>CAM_ID</agentIdentifierType>"
                        + "<agentIdentifierValue> 1234567 </agentIdentifierValue></agentIdentifier>"));

        assertEquals("1234567", state.getOriginatorCamId());
        verify(referenceResolver).resolveReferences(state, null);
    }

    /** A new version naming the same identifier keeps the originator resolved for the previous one. */
    @Test
    void newVersionWithTheSameCamIdentifierKeepsTheResolvedOriginator() throws Exception {
        ApAccessPoint accessPoint = new ApAccessPoint();
        accessPoint.setAccessPointId(42);
        DaAip aip = new DaAip();
        aip.setCode(CODE);
        aip.setDigitalRepository(repository);
        DaAipState previous = new DaAipState();
        previous.setDaAip(aip);
        previous.setOriginatorCamId("1234567");
        previous.setOriginatorAccessPoint(accessPoint);
        when(aipRepository.findByDigitalRepositoryAndCode(repository, CODE)).thenReturn(aip);
        when(aipStateRepository.findByDaAipAndDeleteChangeIsNull(aip)).thenReturn(previous);

        DaAipState state = service.processPackageInfo(repository, packageInfo(
                "<agentIdentifier><agentIdentifierType>CAM_ID</agentIdentifierType>"
                        + "<agentIdentifierValue>1234567</agentIdentifierValue></agentIdentifier>"));

        assertSame(accessPoint, state.getOriginatorAccessPoint());
    }

    @Test
    void newVersionWithAnotherCamIdentifierResolvesTheOriginatorAgain() throws Exception {
        ApAccessPoint accessPoint = new ApAccessPoint();
        accessPoint.setAccessPointId(42);
        DaAip aip = new DaAip();
        aip.setCode(CODE);
        aip.setDigitalRepository(repository);
        DaAipState previous = new DaAipState();
        previous.setDaAip(aip);
        previous.setOriginatorCamId("1");
        previous.setOriginatorAccessPoint(accessPoint);
        when(aipRepository.findByDigitalRepositoryAndCode(repository, CODE)).thenReturn(aip);
        when(aipStateRepository.findByDaAipAndDeleteChangeIsNull(aip)).thenReturn(previous);

        DaAipState state = service.processPackageInfo(repository, packageInfo(
                "<agentIdentifier><agentIdentifierType>CAM_ID</agentIdentifierType>"
                        + "<agentIdentifierValue>1234567</agentIdentifierValue></agentIdentifier>"));

        assertNull(state.getOriginatorAccessPoint());
        verify(referenceResolver).resolveReferences(state, null);
    }

    private File packageInfo(String originatorIdentifiers) throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <premis xmlns="http://www.loc.gov/premis/v3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="3.0">
                    <object xsi:type="intellectualEntity">
                        <objectIdentifier>
                            <objectIdentifierType>local</objectIdentifierType>
                            <objectIdentifierValue>_THIS</objectIdentifierValue>
                        </objectIdentifier>
                        <objectIdentifier>
                            <objectIdentifierType>AIP_ID</objectIdentifierType>
                            <objectIdentifierValue>329740</objectIdentifierValue>
                        </objectIdentifier>
                        <significantProperties>
                            <significantPropertiesType>AIP_VERSION</significantPropertiesType>
                            <significantPropertiesValue>a_00329740_aip_v00001_9</significantPropertiesValue>
                        </significantProperties>
                    </object>
                    <event>
                        <eventIdentifier>
                            <eventIdentifierType>local</eventIdentifierType>
                            <eventIdentifierValue>uuid-e1</eventIdentifierValue>
                        </eventIdentifier>
                        <eventType>cre</eventType>
                        <eventDateTime>1999-01-01/1999-12-31</eventDateTime>
                        <linkingAgentIdentifier>
                            <linkingAgentIdentifierType>local</linkingAgentIdentifierType>
                            <linkingAgentIdentifierValue>uuid-a1</linkingAgentIdentifierValue>
                            <linkingAgentRole>ORIGINATOR</linkingAgentRole>
                        </linkingAgentIdentifier>
                    </event>
                    <agent>
                        <agentIdentifier>
                            <agentIdentifierType>local</agentIdentifierType>
                            <agentIdentifierValue>uuid-a1</agentIdentifierValue>
                        </agentIdentifier>
                        %s
                        <agentName>Univerzita Karlova. 1. lékařská fakulta</agentName>
                        <agentType>org</agentType>
                    </agent>
                </premis>
                """.formatted(originatorIdentifiers);
        Path file = tempDir.resolve("PACKAGE-INFO.xml");
        Files.writeString(file, xml, StandardCharsets.UTF_8);
        return file.toFile();
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
