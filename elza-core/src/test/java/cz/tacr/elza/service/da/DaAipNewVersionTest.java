package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import com.lightcomp.kads.mets.MetsReaderWriter;

import cz.tacr.elza.AbstractServiceTest;
import cz.tacr.elza.api.AipLinkState;
import cz.tacr.elza.api.AipProblemType;
import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDaLink;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaChange;
import cz.tacr.elza.domain.DaChangeType;
import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.repository.AipRepository;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.ArrDaLinkRepository;
import cz.tacr.elza.repository.DaChangeRepository;
import cz.tacr.elza.repository.DaDaoFileFolderRepository;
import cz.tacr.elza.repository.DaDaoFileRepository;
import cz.tacr.elza.repository.DaDaoRelationRepository;
import cz.tacr.elza.repository.DaDaoRepository;
import cz.tacr.elza.repository.DaLevelViewRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;
import cz.tacr.elza.repository.FundRepository;
import gov.loc.mets.v1_11.schema.MetsType;
import gov.loc.premis.v3.PremisComplexType;

/**
 * The digital archive delivers a new version of an AIP that is already attached to the archival
 * description. What the new version takes away from the AIP - a part, or the whole package by
 * moving it to another fund - is taken away from the description too, by a change of the unit it
 * hung on; everything else keeps its links.
 */
public class DaAipNewVersionTest extends AbstractServiceTest {

    private static final String CODE = "0ea6e9ad-424f-44dc-9413-373678fccf71";
    private static final String METS = "/zp2015/da-volne/a/METS.xml";

    /** The level "3.jpg" of the logical structure, with one file. */
    private static final String PART = "k_2";
    /** The level "33.jpg", a sibling of {@link #PART}. */
    private static final String OTHER_PART = "uuid-a4e47c74-3a76-498b-80ea-bee8de147a79";
    /** The level of the whole package; every file of the logical structure is under it. */
    private static final String PACKAGE_LEVEL = "ds_1";

    @Autowired
    private DaService daService;
    @Autowired
    private PackageInfoService packageInfoService;
    @Autowired
    private DaAipLinkStateResolver linkStateResolver;
    @Autowired
    private AipRepository aipRepository;
    @Autowired
    private AipStateRepository aipStateRepository;
    @Autowired
    private DaChangeRepository changeRepository;
    @Autowired
    private DaDaoRepository daoRepository;
    @Autowired
    private DaDaoRelationRepository daoRelationRepository;
    @Autowired
    private DaDaoFileRepository daoFileRepository;
    @Autowired
    private DaDaoFileFolderRepository daoFileFolderRepository;
    @Autowired
    private DaLevelViewRepository levelViewRepository;
    @Autowired
    private ArrDaLinkRepository daLinkRepository;
    @Autowired
    private DigitalRepositoryRepository digitalRepositoryRepository;
    @Autowired
    private FundRepository fundRepository;

    private TransactionTemplate tx() {
        return new TransactionTemplate(txManager);
    }

    @AfterEach
    public void deleteCreatedRows() {
        tx().executeWithoutResult(t -> {
            daLinkRepository.deleteAll();
            daoFileRepository.deleteAll();
            daoFileFolderRepository.deleteAll();
            daoRelationRepository.deleteAll();
            daoRepository.deleteAll();
            levelViewRepository.deleteAll();
            aipStateRepository.deleteAll();
            changeRepository.deleteAll();
            aipRepository.deleteAll();
            digitalRepositoryRepository.deleteAll();
        });
    }

    private Integer createRepository() {
        ArrDigitalRepository repository = new ArrDigitalRepository();
        repository.setCode("DA-NEW-VERSION");
        repository.setName("Testovaci digitalni archiv");
        repository.setDigitalRepositoryType(DigitalRepositoryType.DA);
        repository.setSendNotification(false);
        repository.setMultipleLinks(false);
        return digitalRepositoryRepository.save(repository).getExternalSystemId();
    }

    /** An AIP as a PACKAGE-INFO naming the fund by its internal code leaves it. */
    private Integer createAip(Integer repositoryId, FundInfo fund, String fundCode) {
        DaAip aip = new DaAip();
        aip.setCode(CODE);
        aip.setDigitalRepository(digitalRepositoryRepository.findById(repositoryId).orElseThrow());
        aipRepository.save(aip);

        DaChange change = new DaChange();
        change.setChangeDate(LocalDateTime.now());
        change.setDaAip(aip);
        change.setType(DaChangeType.AIP_CREATE);
        changeRepository.save(change);

        DaAipState state = new DaAipState();
        state.setDaAip(aip);
        state.setCreateChange(change);
        state.setAipVersion("1");
        state.setFundCode(fundCode);
        state.setFund(fund.getFund());
        aipStateRepository.save(state);
        return aip.getAipId();
    }

    /** Builds the digital entities from the METS of the test package, changed as the version needs. */
    private void receiveMets(Integer aipId, UnaryOperator<String> version) throws Exception {
        String metsXml;
        try (InputStream is = getClass().getResourceAsStream(METS)) {
            metsXml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        MetsType mets = MetsReaderWriter.unmarshal(
                new ByteArrayInputStream(version.apply(metsXml).getBytes(StandardCharsets.UTF_8)));
        Path tempDir = Files.createTempDirectory("da-new-version");
        tx().executeWithoutResult(t -> daService.createDaoStructure(aipRepository.findById(aipId).orElseThrow(),
                mets, new PremisComplexType(), tempDir));
    }

    /** Receives the PACKAGE-INFO of the given version, naming the given fund. */
    private void receivePackageInfo(Integer repositoryId, String version, String fundCode) throws Exception {
        receivePackageInfo(repositoryId, version, fundCode, null);
    }

    /** @param institutionCode null when the package names no institution */
    private void receivePackageInfo(Integer repositoryId, String version, String fundCode, String institutionCode)
            throws Exception {
        String institution = institutionCode == null ? "" : """
                        <premis:significantProperties>
                            <premis:significantPropertiesType>INSTITUTION_ID</premis:significantPropertiesType>
                            <premis:significantPropertiesValue>%s</premis:significantPropertiesValue>
                        </premis:significantProperties>
                """.formatted(institutionCode);
        String xml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <premis:premis xmlns:premis="http://www.loc.gov/premis/v3">
                    <premis:object xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="premis:intellectualEntity">
                        <premis:objectIdentifier>
                            <premis:objectIdentifierType>FONDS_ID</premis:objectIdentifierType>
                            <premis:objectIdentifierValue>%s</premis:objectIdentifierValue>
                        </premis:objectIdentifier>
%s                    </premis:object>
                    <premis:object xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="premis:intellectualEntity">
                        <premis:objectIdentifier>
                            <premis:objectIdentifierType>AIP_ID</premis:objectIdentifierType>
                            <premis:objectIdentifierValue>%s</premis:objectIdentifierValue>
                        </premis:objectIdentifier>
                        <premis:significantProperties>
                            <premis:significantPropertiesType>AIP_VERSION</premis:significantPropertiesType>
                            <premis:significantPropertiesValue>%s</premis:significantPropertiesValue>
                        </premis:significantProperties>
                    </premis:object>
                </premis:premis>
                """.formatted(fundCode, institution, CODE, version).strip();
        Path file = Files.createTempFile("PACKAGE-INFO", ".xml");
        Files.writeString(file, xml);
        try {
            packageInfoService.processPackageInfo(digitalRepositoryRepository.findById(repositoryId).orElseThrow(),
                    file.toFile());
        } finally {
            Files.delete(file);
        }
    }

    private DaDao liveDao(Integer aipId, String code) {
        DaAip aip = aipRepository.findById(aipId).orElseThrow();
        return daoRepository.findByAipAndDeleteChangeIsNull(aip).stream()
                .filter(d -> d.getCode().equals(code))
                .findFirst().orElse(null);
    }

    /** Attaches a part of the package to the root of the fund and returns the link. */
    private Integer attachPart(FundInfo fund, Integer aipId, String code) {
        return tx().execute(t -> {
            DaDao dao = liveDao(aipId, code);
            daService.connectPartToJP(nodeRepository.findById(fund.getRootNodeId()).orElseThrow(),
                    aipRepository.findById(aipId).orElseThrow(), dao);
            return daLinkRepository.findByDaDaoInAndDeleteChangeIsNull(List.of(dao)).get(0).getDaoLinkId();
        });
    }

    private DaAipState activeState(Integer aipId) {
        return aipStateRepository.findByDaAipAndDeleteChangeIsNull(aipRepository.findById(aipId).orElseThrow());
    }

    /** Renaming a level or a file in the archive keeps the entity, and so the link to it. */
    @Test
    public void renamedPartKeepsItsLink() throws Exception {
        FundInfo fund = tx().execute(t -> createFund("F-da-version-rename"));
        Integer repositoryId = tx().execute(t -> createRepository());
        Integer aipId = tx().execute(t -> createAip(repositoryId, fund, null));
        receiveMets(aipId, UnaryOperator.identity());
        Integer daoId = tx().execute(t -> liveDao(aipId, PART).getDaoId());
        Integer linkId = attachPart(fund, aipId, PART);

        receiveMets(aipId, xml -> xml.replace("LABEL=\"3.jpg\"", "LABEL=\"3-prejmenovano.jpg\""));

        tx().executeWithoutResult(t -> {
            DaDao dao = liveDao(aipId, PART);
            assertEquals(daoId, dao.getDaoId(), "the entity is the same one");
            assertEquals("komponenta:3-prejmenovano.jpg", dao.getLabel(), "under the new label");
            assertNull(daLinkRepository.findById(linkId).orElseThrow().getDeleteChange(), "the link stays");
        });
    }

    /**
     * A part the new version no longer has cannot stay attached. Its link is closed by a change of
     * the unit it hung on - before, the whole update failed and the AIP stayed at its old content.
     */
    @Test
    public void removedPartIsDetachedWithChangeOfItsUnit() throws Exception {
        FundInfo fund = tx().execute(t -> createFund("F-da-version-removed"));
        Integer repositoryId = tx().execute(t -> createRepository());
        Integer aipId = tx().execute(t -> createAip(repositoryId, fund, null));
        receiveMets(aipId, UnaryOperator.identity());
        Integer removedLinkId = attachPart(fund, aipId, PART);
        Integer keptLinkId = attachPart(fund, aipId, OTHER_PART);

        receiveMets(aipId, xml -> xml.replaceAll("(?s)<div ID=\"k_2\".*?</div>", ""));

        tx().executeWithoutResult(t -> {
            assertNull(liveDao(aipId, PART), "the level is gone from the package");

            ArrDaLink removed = daLinkRepository.findById(removedLinkId).orElseThrow();
            assertNotNull(removed.getDeleteChange(), "its link is closed");
            assertEquals(ArrChange.Type.DELETE_DAO_LINK, removed.getDeleteChange().getType());
            assertEquals(fund.getRootNodeId(), removed.getDeleteChange().getPrimaryNodeId(),
                         "by a change of the unit it hung on");

            assertNull(daLinkRepository.findById(keptLinkId).orElseThrow().getDeleteChange(),
                       "the link of a part still in the package stays");
        });
    }

    /**
     * Content added to the package does not touch the links; whether the whole package is attached
     * is worked out again - a link of the package level no longer reaches the new file.
     */
    @Test
    public void addedFileKeepsLinksAndLowersTheLinkState() throws Exception {
        FundInfo fund = tx().execute(t -> createFund("F-da-version-added"));
        Integer repositoryId = tx().execute(t -> createRepository());
        Integer aipId = tx().execute(t -> createAip(repositoryId, fund, null));
        receiveMets(aipId, UnaryOperator.identity());
        Integer linkId = attachPart(fund, aipId, PACKAGE_LEVEL);
        tx().executeWithoutResult(t -> assertEquals(AipLinkState.FULLY_LINKED,
                linkStateResolver.computeLinkState(aipRepository.findById(aipId).orElseThrow())));

        receiveMets(aipId, xml -> xml.replace(
                "<fileGrp ID=\"capture-LTP_COPY-f002\" USE=\"Representations/capture-LTP_COPY-f002\">",
                "<fileGrp ID=\"capture-LTP_COPY-f002\" USE=\"Representations/capture-LTP_COPY-f002\">"
                + "<file ID=\"novy_soubor\" MIMETYPE=\"image/jpeg\" SIZE=\"1\" CHECKSUM=\"00\" CHECKSUMTYPE=\"SHA-512\">"
                + "<FLocat LOCTYPE=\"URL\" xlink:type=\"simple\""
                + " xlink:href=\"representations/capture-LTP_COPY-f002/data/novy.jpg\"/></file>"));

        tx().executeWithoutResult(t -> {
            assertNotNull(liveDao(aipId, "novy_soubor"), "the new file is part of the package");
            assertNull(daLinkRepository.findById(linkId).orElseThrow().getDeleteChange(), "the link stays");
            assertEquals(AipLinkState.PARTIALLY_LINKED,
                         linkStateResolver.computeLinkState(aipRepository.findById(aipId).orElseThrow()),
                         "the new file is not under the attached level");
        });
    }

    /** A version naming another fund takes the package out of the description of the former one. */
    @Test
    public void movedToAnotherFundIsDetachedFromTheFormer() throws Exception {
        FundInfo former = tx().execute(t -> createFund("F-da-version-former", "FUND-FORMER"));
        FundInfo next = tx().execute(t -> createFund("F-da-version-next", "FUND-NEXT"));
        Integer repositoryId = tx().execute(t -> createRepository());
        Integer aipId = tx().execute(t -> createAip(repositoryId, former, "FUND-FORMER"));
        Integer linkId = tx().execute(t -> daService.connectToJP(former.getRootNodeId(), aipId).getDaoLinkId());

        tx().executeWithoutResult(t -> {
            try {
                receivePackageInfo(repositoryId, "2", "FUND-NEXT");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

        tx().executeWithoutResult(t -> {
            DaAipState state = activeState(aipId);
            assertEquals("2", state.getAipVersion());
            assertEquals(next.getFund().getFundId(), state.getFund().getFundId(), "the AIP belongs to the new fund");

            ArrDaLink link = daLinkRepository.findById(linkId).orElseThrow();
            assertNotNull(link.getDeleteChange(), "the link to the former fund is closed");
            assertEquals(ArrChange.Type.DELETE_DAO_LINK, link.getDeleteChange().getType());
            assertEquals(former.getRootNodeId(), link.getDeleteChange().getPrimaryNodeId());

            assertEquals(AipLinkState.NOT_LINKED, state.getLinkState());
        });
    }

    /** A version naming a fund ELZA does not know takes the package out of the former fund too. */
    @Test
    public void movedToUnknownFundIsDetached() throws Exception {
        FundInfo former = tx().execute(t -> createFund("F-da-version-unknown", "FUND-KNOWN"));
        Integer repositoryId = tx().execute(t -> createRepository());
        Integer aipId = tx().execute(t -> createAip(repositoryId, former, "FUND-KNOWN"));
        Integer linkId = tx().execute(t -> daService.connectToJP(former.getRootNodeId(), aipId).getDaoLinkId());

        tx().executeWithoutResult(t -> {
            try {
                receivePackageInfo(repositoryId, "2", "FUND-NOBODY-KNOWS");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

        tx().executeWithoutResult(t -> {
            DaAipState state = activeState(aipId);
            assertNull(state.getFund());
            assertEquals(AipProblemType.UNKNOWN_FUND, state.getProblemType());
            assertNotNull(daLinkRepository.findById(linkId).orElseThrow().getDeleteChange());
        });
    }

    private void receive(Integer repositoryId, String fundCode, String institutionCode) {
        tx().executeWithoutResult(t -> {
            try {
                receivePackageInfo(repositoryId, "2", fundCode, institutionCode);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    /** The same fund code under another institution is another fund: the package leaves the former one. */
    @Test
    public void sameFundCodeUnderAnotherInstitutionIsAnotherFund() throws Exception {
        FundInfo fund = tx().execute(t -> createFund("F-da-version-other-inst", "FUND-INST"));
        Integer repositoryId = tx().execute(t -> createRepository());
        Integer aipId = tx().execute(t -> createAip(repositoryId, fund, "FUND-INST"));
        Integer linkId = tx().execute(t -> daService.connectToJP(fund.getRootNodeId(), aipId).getDaoLinkId());

        receive(repositoryId, "FUND-INST", "INSTITUTION-ELSEWHERE");

        tx().executeWithoutResult(t -> {
            assertNull(activeState(aipId).getFund());
            assertNotNull(daLinkRepository.findById(linkId).orElseThrow().getDeleteChange());
        });
    }

    /** A version that names the institution of its fund for the first time stays in that fund. */
    @Test
    public void namingTheOwnInstitutionKeepsTheFund() throws Exception {
        FundInfo fund = tx().execute(t -> createFund("F-da-version-own-inst", "FUND-OWN"));
        Integer repositoryId = tx().execute(t -> createRepository());
        Integer aipId = tx().execute(t -> createAip(repositoryId, fund, "FUND-OWN"));
        Integer linkId = tx().execute(t -> daService.connectToJP(fund.getRootNodeId(), aipId).getDaoLinkId());

        receive(repositoryId, "FUND-OWN", firstInstitution.getInternalCode());

        tx().executeWithoutResult(t -> {
            assertEquals(fund.getFund().getFundId(), activeState(aipId).getFund().getFundId());
            assertNull(daLinkRepository.findById(linkId).orElseThrow().getDeleteChange());
        });
    }

    /**
     * A version naming the same fund stays where the AIP was, even when the fund would no longer be
     * found by its code - only the package moves an AIP. The new state still says it is attached.
     */
    @Test
    public void sameFundCodeKeepsTheFundAndTheLinks() throws Exception {
        FundInfo fund = tx().execute(t -> createFund("F-da-version-same", "FUND-SAME"));
        Integer repositoryId = tx().execute(t -> createRepository());
        Integer aipId = tx().execute(t -> createAip(repositoryId, fund, "FUND-SAME"));
        Integer linkId = tx().execute(t -> daService.connectToJP(fund.getRootNodeId(), aipId).getDaoLinkId());
        tx().executeWithoutResult(t -> {
            ArrFund stored = fundRepository.findById(fund.getFund().getFundId()).orElseThrow();
            stored.setInternalCode("FUND-RENAMED");
            fundRepository.save(stored);
        });

        tx().executeWithoutResult(t -> {
            try {
                receivePackageInfo(repositoryId, "2", "FUND-SAME");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

        tx().executeWithoutResult(t -> {
            DaAipState state = activeState(aipId);
            assertEquals("2", state.getAipVersion());
            assertEquals(fund.getFund().getFundId(), state.getFund().getFundId());
            assertNull(daLinkRepository.findById(linkId).orElseThrow().getDeleteChange());
            assertEquals(AipLinkState.FULLY_LINKED, state.getLinkState(),
                         "the new state does not start as not attached");
            assertTrue(aipStateRepository.findAll().stream().anyMatch(s -> s.getDeleteChange() != null),
                       "the previous state is closed");
        });
    }
}
