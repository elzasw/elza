package cz.tacr.elza.service.da;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.data.StructType;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDaLink;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaChange;
import cz.tacr.elza.domain.DaChangeType;
import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.domain.DaDaoFile;
import cz.tacr.elza.domain.DaDaoFileFolder;
import cz.tacr.elza.domain.DaDaoItem;
import cz.tacr.elza.domain.DaDaoRelation;
import cz.tacr.elza.domain.RulComponent;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.domain.converter.UnitDateConverter;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DaDaoFileFolderRepository;
import cz.tacr.elza.repository.DaDaoFileRepository;
import cz.tacr.elza.repository.DaDaoItemRepository;
import cz.tacr.elza.repository.DaDaoRelationRepository;
import cz.tacr.elza.repository.DaDaoRepository;
import cz.tacr.elza.repository.ArrDaLinkRepository;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.DaoLevelViewService;
import cz.tacr.elza.service.GroovyScriptService;
import gov.loc.mets.v1_11.schema.AmdSecType;
import gov.loc.mets.v1_11.schema.DivType;
import gov.loc.mets.v1_11.schema.FileGrpType;
import gov.loc.mets.v1_11.schema.FileType;
import gov.loc.mets.v1_11.schema.MdSecType;
import gov.loc.mets.v1_11.schema.MetsType;
import gov.loc.mets.v1_11.schema.StructMapType;
import gov.loc.premis.v3.File;
import gov.loc.premis.v3.ObjectComplexType;
import gov.loc.premis.v3.ObjectIdentifierComplexType;
import gov.loc.premis.v3.PremisComplexType;
import org.apache.commons.collections4.CollectionUtils;
import org.archivists.ead3.schema.Abstract;
import org.archivists.ead3.schema.Archdesc;
import org.archivists.ead3.schema.C;
import org.archivists.ead3.schema.Daterange;
import org.archivists.ead3.schema.Did;
import org.archivists.ead3.schema.Dsc;
import org.archivists.ead3.schema.Ead;
import org.archivists.ead3.schema.Unitdatestructured;
import org.archivists.ead3.schema.Unittitle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import java.time.format.DateTimeFormatter;

@Component
@Scope("prototype")
public class DaoProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DaoProcessor.class);
    private static final DateTimeFormatter FORMATTER_DATE = DateTimeFormatter.ofPattern(UnitDateConverter.FORMAT_DATE);

    @Autowired
    private DaService daService;
    @Autowired
    private DaDaoRepository daoRepository;
    @Autowired
    private DaDaoRelationRepository daoRelationRepository;
    @Autowired
    private DaDaoFileRepository daoFileRepository;
    @Autowired
    private DaDaoFileFolderRepository daoFileFolderRepository;
    @Autowired
    private DaDaoItemRepository daoItemRepository;
    @Autowired
    private ArrDaLinkRepository daLinkRepository;
    @Autowired
    private DaoLevelViewService levelViewService;
    @Autowired
    private StaticDataService staticDataService;
    @Autowired
    private DataRepository dataRepository;
    @Autowired
    private GroovyScriptService groovyScriptService;
    @Autowired
    private ResourcePathResolver resourcePathResolver;
    @Autowired
    private ArrangementInternalService arrangementInternalService;

    private static final String IMPORT_DA = "IMPORT_DA";

    private final DaAip aip;

    private final MetsType metsType;

    private final PremisComplexType premisComplexType;

    private final Path tempDir;

    private Ead ead;

    private Map<String, DaDao> daDaoMap;

    private Map<Integer, List<DaDaoRelation>> daDaoRelationMap;

    private Map<Integer, List<DaDaoFileFolder>> daDaoFileFolderMap;

    private Map<Integer, List<DaDaoFile>> daDaoFileMap;

    private Map<Integer, List<DaDaoItem>> daDaoItemMap;

    private final Map<String, DaDao> fileDaoMap = new HashMap<>();

    private final Map<String, DaDao> logicalDaoMap = new HashMap<>();

    private final Map<String, DaDao> representations = new HashMap<>();

    private final Map<Integer, List<DaDaoFileFolder>> newDaDaoFileFolderMap = new HashMap<>();

    private boolean forceUpdate;

    public DaoProcessor(DaAip aip, MetsType metsType, PremisComplexType premisComplexType, Path tempDir, boolean forceUpdate) {
        this.aip = aip;
        this.metsType = metsType;
        this.premisComplexType = premisComplexType;
        this.tempDir = tempDir;
        this.forceUpdate = forceUpdate;
    }


    public boolean process() {
        List<DaDao> daDaoList = daoRepository.findByAipAndDeleteChangeIsNull(aip);
        daDaoMap = daDaoList.stream()
                .collect(Collectors.toMap(DaDao::getCode, Function.identity()));

        daDaoRelationMap = daoRelationRepository.findByDaoInAndDeleteChangeIsNull(daDaoList).stream()
                .collect(Collectors.groupingBy(r -> r.getDao().getDaoId()));
        daDaoFileFolderMap = daoFileFolderRepository.findByRepresentationDaoInAndDeleteChangeIsNull(daDaoList).stream()
                .collect(Collectors.groupingBy(f -> f.getRepresentationDao().getDaoId()));
        daDaoFileMap = daoFileRepository.findByDaoInAndDeleteChangeIsNull(daDaoList).stream()
                .collect(Collectors.groupingBy(f -> f.getDao().getDaoId()));
        daDaoItemMap = daoItemRepository.findByDaoInAndDeleteChangeIsNull(daDaoList).stream()
                .collect(Collectors.groupingBy(i -> i.getDao().getDaoId()));

        DaChangeType changeType = daDaoMap.isEmpty() ? DaChangeType.AIP_UPDATE : DaChangeType.AIP_CREATE;
        DaChange change = daService.createDaChange(aip, changeType);

        //representation and files
        createRepresentationDaoFromStruct(metsType.getStructMap(), metsType.getFileSec(), change);
        if (metsType.getFileSec() != null) {
            createDaoFromFileSec(metsType.getFileSec(), change);
        }

        //metadata
        if (CollectionUtils.isNotEmpty(metsType.getAmdSec())) {
            createDaoFromAmdSec(metsType.getAmdSec(), change);
        }
        if (CollectionUtils.isNotEmpty(metsType.getDmdSec())) {
            createDaoFromDmdSec(metsType.getDmdSec(), change);
        }

        //logical
        createDaoFromStruct(metsType.getStructMap(), change);

        if (ead != null) {
            //ead
            createDaoItemsFromArchDesc(ead.getArchdesc(), change);
        }

        deleteOldComponents(change);
        levelViewService.processLevelViewForAip(aip, change);
        return true;
    }

    private void deleteOldComponents(DaChange change) {
        //smazání starých komponent
        Set<DaDao> daDaoSet = new HashSet<>(daDaoMap.values());

        List<ArrDaLink> daoLinkList = daLinkRepository.findByDaDaoInAndDeleteChangeIsNull(daDaoSet);
        if (CollectionUtils.isNotEmpty(daoLinkList) && !forceUpdate) {
            throw new IllegalStateException("Nelze smazat dao, které má vazbu na node");
        }
        ArrChange arrChange = arrangementInternalService.createChange(ArrChange.Type.DELETE_DAO_LINK, null);


        Set<DaDaoRelation> daDaoRelationSet = daDaoRelationMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());

        Set<DaDaoFileFolder> daDaoFileFolderSet = daDaoFileFolderMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());

        Set<DaDaoFile> daDaoFileSet = daDaoFileMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());

        Set<DaDaoItem> daDaoItemSet = daDaoItemMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());

        daDaoSet.forEach(d -> d.setDeleteChange(change));
        daDaoRelationSet.forEach(r -> r.setDeleteChange(change));
        daDaoFileFolderSet.forEach(f -> f.setDeleteChange(change));
        daDaoFileSet.forEach(f -> f.setDeleteChange(change));
        daDaoItemSet.forEach(i -> i.setDeleteChange(change));
        daoLinkList.forEach(l -> l.setDeleteChange(arrChange));

        daoRepository.saveAll(daDaoSet);
        daoRelationRepository.saveAll(daDaoRelationSet);
        daoFileFolderRepository.saveAll(daDaoFileFolderSet);
        daoFileRepository.saveAll(daDaoFileSet);
        daoItemRepository.saveAll(daDaoItemSet);
        daLinkRepository.saveAll(daoLinkList);
    }

    private void createRepresentationDaoFromStruct(List<StructMapType> structMap, MetsType.FileSec fileSec, DaChange change) {
        for (StructMapType structMapType : structMap) {
            if (structMapType.getTYPE().equals("PHYSICAL")) {
                createRepresentationDaoFromDiv(structMapType.getDiv(), fileSec, change);
            }
        }
    }

    private void createRepresentationDaoFromDiv(DivType divType, MetsType.FileSec fileSec, DaChange change) {
        if (divType.getLABEL() != null && divType.getLABEL().equals("Representations")) {
            if (CollectionUtils.isNotEmpty(divType.getFptr())) {
                for (DivType.Fptr fptr : divType.getFptr()) {
                    MetsType.FileSec.FileGrp fileGrp = (MetsType.FileSec.FileGrp) fptr.getFILEID();
                    String code = fileGrp.getID();
                    DaDao.DaoType type = DaDao.DaoType.REPRESENTATION;
                    String label = getRepresentationLabel(fileSec, code);
                    DaDao daDao = daDaoMap.getOrDefault(code, null);
                    if (daDao == null || isDaoChanged(daDao, code, label, type)) {
                        daDao = daService.createDaDao(aip, change, code, label, type);
                    } else {
                        daDaoMap.remove(code);
                    }

                    representations.put(code, daDao);
                }
            }
            return;
        }

        if (CollectionUtils.isNotEmpty(divType.getDiv())) {
            for (DivType div : divType.getDiv()) {
                createRepresentationDaoFromDiv(div, fileSec, change);
            }
        }
    }

    @Nullable
    private String getRepresentationLabel(MetsType.FileSec fileSec, String code) {
        if (CollectionUtils.isNotEmpty(fileSec.getFileGrp())) {
            for (FileGrpType fileGrpType : fileSec.getFileGrp()) {
                if (fileGrpType.getID().equals(code)) {
                    String use = fileGrpType.getUSE();
                    return use.substring(use.indexOf("/") + 1);
                }
            }
        }
        return null;
    }

    private void createDaoFromFileSec(MetsType.FileSec fileSec, DaChange change) {
        if (CollectionUtils.isNotEmpty(fileSec.getFileGrp())) {
            for (FileGrpType fileGrpType : fileSec.getFileGrp()) {
                DaDao representationDao = representations.getOrDefault(fileGrpType.getID(), null);
                if (representationDao != null) {
                    createDaoFromFileGrp(fileGrpType, representationDao, change);
                }
            }
        }
    }

    private void createDaoFromFileGrp(FileGrpType fileGrpType, DaDao representationDao, DaChange change) {
        if (CollectionUtils.isNotEmpty(fileGrpType.getFile())) {
            for (FileType fileType : fileGrpType.getFile()) {
                createDaoFromFile(fileType, representationDao, change);
            }
        }

        if (CollectionUtils.isNotEmpty(fileGrpType.getFileGrp())) {
            for (FileGrpType fileGrp : fileGrpType.getFileGrp()) {
                createDaoFromFileGrp(fileGrp, representationDao, change);
            }
        }
    }

    private void createDaoFromFile(FileType fileType, DaDao representationDao, DaChange change) {
        String code = fileType.getID();
        DaDao.DaoType type = DaDao.DaoType.FILE;
        String label = findOriginalNameInPremis(premisComplexType, code);
        if (label == null) {
            label = getDaoLabel(getFileHref(fileType));
        }

        DaDao daDao = daDaoMap.getOrDefault(code, null);
        if (daDao == null || isDaoChanged(daDao, code, label, type)) {
            daDao = daService.createDaDao(aip, change, code, label, type);
        } else {
            daDaoMap.remove(code);
        }

        findOrCreateDaoRelation(daDao, representationDao, change);

        createFileFromFile(fileType, daDao, representationDao, change);
        fileDaoMap.put(code, daDao);
    }

    private void createFileFromFile(FileType fileType, DaDao daDao, DaDao representationDao, DaChange change) {
        String href = getFolderPath(getFileHref(fileType));
        DaDaoFileFolder fileFolder = findOrCreateFileFolder(representationDao, change, href);
        String checksum = fileType.getCHECKSUM();
        String checksumType = fileType.getCHECKSUMTYPE();
        String mimeType = fileType.getMIMETYPE();
        BigInteger size = BigInteger.valueOf(fileType.getSIZE());
        Integer imageHeight = null;
        Integer imageWidth = null;
        String sourceXDimensionUnit = null;
        Integer sourceXDimensionValue = null;
        String sourceYDimensionUnit = null;
        Integer sourceYDimensionValue = null;
        String duration = null;
        String description = null;
        String fileName = findOriginalNameInPremis(premisComplexType, fileType.getID());
        if (fileName == null) {
            fileName = getFileHref(fileType);
        }

        findOrCreateFile(change, daDao, fileFolder, checksum, checksumType, mimeType, size, imageHeight, imageWidth, sourceXDimensionUnit,
                sourceXDimensionValue, sourceYDimensionUnit, sourceYDimensionValue, duration, description, fileName);
    }

    private void createDaoFromAmdSec(List<AmdSecType> amdSecList, DaChange change) {
        for (AmdSecType amdSecType : amdSecList) {
            if (CollectionUtils.isNotEmpty(amdSecType.getDigiprovMD())) {
                for (MdSecType mdSecType : amdSecType.getDigiprovMD()) {
                    String code = mdSecType.getID();
                    DaDao.DaoType type = DaDao.DaoType.METAAMD;
                    String label = findOriginalNameInPremis(premisComplexType, code);
                    if (label == null) {
                        label = getDaoLabel(mdSecType.getMdRef().getHref());
                    }

                    createDaoFromMdSecType(mdSecType, code, label, type, change);
                }
            }
        }
    }

    private void createDaoFromDmdSec(List<MdSecType> dmdSec, DaChange change) {
        for (MdSecType mdSecType : dmdSec) {
            String code = mdSecType.getID();
            DaDao.DaoType type = mdSecType.getGROUPID().equals("CONTEXTUAL") ? DaDao.DaoType.METADMDCONTEXTUAL : DaDao.DaoType.METADMDINHERENT;
            String href = mdSecType.getMdRef().getHref();

            if (type == DaDao.DaoType.METADMDINHERENT) {
                try {
                    String newHref = href.replace("/", java.io.File.separator);
                    ead = daService.loadEadFile(tempDir, newHref);
                } catch (Exception e) {
                    logger.error("Došlo k chybě při načtení EAD souboru {}", href, e);
                }
            }

            String label = findOriginalNameInPremis(premisComplexType, code);
            if (label == null) {
                label = getDaoLabel(href);
            }

            createDaoFromMdSecType(mdSecType, code, label, type, change);
        }
    }

    private String getFolderPath(String href) {
        return href.substring(href.lastIndexOf("/data/") + 6);
    }

    private String getDaoLabel(String href) {
        return href.substring(href.lastIndexOf("/") + 1);
    }

    private void createDaoFromMdSecType(MdSecType mdSecType, String code, String label, DaDao.DaoType type, DaChange change) {
        DaDao daDao = daDaoMap.getOrDefault(code, null);
        if (daDao == null || isDaoChanged(daDao, code, label, type)) {
            daDao = daService.createDaDao(aip, change, code, label, type);
        } else {
            daDaoMap.remove(code);
        }

        createFileFromMdSec(mdSecType, daDao, change);
        fileDaoMap.put(code, daDao);
    }

    private void createFileFromMdSec(MdSecType mdSecType, DaDao daDao, DaChange change) {
        String checksum = mdSecType.getMdRef().getCHECKSUM();
        String checksumType = mdSecType.getMdRef().getCHECKSUMTYPE();
        String mimeType = mdSecType.getMdRef().getMIMETYPE();
        BigInteger size = BigInteger.valueOf(mdSecType.getMdRef().getSIZE());
        Integer imageHeight = null;
        Integer imageWidth = null;
        String sourceXDimensionUnit = null;
        Integer sourceXDimensionValue = null;
        String sourceYDimensionUnit = null;
        Integer sourceYDimensionValue = null;
        String duration = null;
        String description = mdSecType.getMdRef().getMDTYPE();
        String fileName = findOriginalNameInPremis(premisComplexType, mdSecType.getID());
        if (fileName == null) {
            fileName = mdSecType.getMdRef().getHref();
        }

        findOrCreateFile(change, daDao, null, checksum, checksumType, mimeType, size, imageHeight, imageWidth, sourceXDimensionUnit,
                sourceXDimensionValue, sourceYDimensionUnit, sourceYDimensionValue, duration, description, fileName);
    }

    private void createDaoFromStruct(List<StructMapType> structMap, DaChange change) {
        for (StructMapType structMapType : structMap) {
            if (structMapType.getTYPE().equals("LOGICAL")) {
                createDaoFromDiv(structMapType.getDiv(), change, null);
            }
        }
    }

    private void createDaoFromDiv(DivType divType, DaChange change, @Nullable DaDao parentDao) {
        String label = divType.getTYPE() != null ? divType.getTYPE() + ":" + divType.getLABEL() : divType.getLABEL();
        String code = divType.getID();
        DaDao daDao = daDaoMap.getOrDefault(code, null);
        DaDao.DaoType type = DaDao.DaoType.LOGICAL;
        if (daDao == null || isDaoChanged(daDao, code, label, type)) {
            daDao = daService.createDaDao(aip, change, code, label, type);
            if (parentDao != null) {
                daService.createDaDaoRelation(daDao, parentDao, change);
            }
        } else {
            daDaoMap.remove(code);
            if (parentDao != null) {
                findOrCreateDaoRelation(daDao, parentDao, change);
            }
        }

        if (CollectionUtils.isNotEmpty(divType.getFptr())) {
            for (DivType.Fptr fptr : divType.getFptr()) {
                FileType fileType = (FileType) fptr.getFILEID();
                DaDao fileDao = fileDaoMap.get(fileType.getID());
                findOrCreateDaoRelation(fileDao, daDao, change);
            }
        }

        if (CollectionUtils.isNotEmpty(divType.getDiv())) {
            for (DivType div : divType.getDiv()) {
                createDaoFromDiv(div, change, daDao);
            }
        }
        logicalDaoMap.put(code, daDao);
    }

    private DaDaoFile findOrCreateFile(DaChange change, DaDao dao, DaDaoFileFolder daoFileFolder, String checksum, String checksumType,
                                       String mimeType, BigInteger size, Integer imageHeight, Integer imageWidth, String sourceXDimensionUnit,
                                       Integer sourceXDimensionValue, String sourceYDimensionUnit, Integer sourceYDimensionValue,
                                       String duration, String description, String fileName) {
        List<DaDaoFile> daDaoFiles = daDaoFileMap.getOrDefault(dao.getDaoId(), new ArrayList<>());
        if (CollectionUtils.isNotEmpty(daDaoFiles)) {
            for (DaDaoFile daDaoFile : daDaoFiles) {
                if (isFileSame(daDaoFile, daoFileFolder, checksum, checksumType, mimeType, size, imageHeight, imageWidth, sourceXDimensionUnit,
                        sourceXDimensionValue, sourceYDimensionUnit, sourceYDimensionValue, duration, description, fileName)) {
                    daDaoFiles.remove(daDaoFile);
                    daDaoFileMap.put(dao.getDaoId(), daDaoFiles);
                    return daDaoFile;
                }
            }
        }
        return daService.createDaDaoFile(change, dao, daoFileFolder, checksum, checksumType, mimeType, size, imageHeight, imageWidth, sourceXDimensionUnit,
                sourceXDimensionValue, sourceYDimensionUnit, sourceYDimensionValue, duration, description, fileName);
    }

    private boolean isFileSame(DaDaoFile daDaoFile, DaDaoFileFolder daoFileFolder, String checksum, String checksumType, String mimeType,
                               BigInteger size, Integer imageHeight, Integer imageWidth, String sourceXDimensionUnit, Integer sourceXDimensionValue,
                               String sourceYDimensionUnit, Integer sourceYDimensionValue, String duration, String description, String fileName) {
        return Objects.equals(daoFileFolder, daDaoFile.getDaoFileFolder())
                && Objects.equals(checksum, daDaoFile.getChecksum())
                && Objects.equals(checksumType, daDaoFile.getChecksumType())
                && Objects.equals(mimeType, daDaoFile.getMimeType())
                && Objects.equals(size, daDaoFile.getSize())
                && Objects.equals(imageHeight, daDaoFile.getImageHeight())
                && Objects.equals(imageWidth, daDaoFile.getImageWidth())
                && Objects.equals(sourceXDimensionUnit, daDaoFile.getSourceXDimensionUnit())
                && Objects.equals(sourceXDimensionValue, daDaoFile.getSourceXDimensionValue())
                && Objects.equals(sourceYDimensionUnit, daDaoFile.getSourceYDimensionUnit())
                && Objects.equals(sourceYDimensionValue, daDaoFile.getSourceYDimensionValue())
                && Objects.equals(duration, daDaoFile.getDuration())
                && Objects.equals(description, daDaoFile.getDescription())
                && Objects.equals(fileName, daDaoFile.getFileName());
    }

    private String getFileHref(FileType fileType) {
        return fileType.getFLocat().get(0).getHref();
    }

    private DaDaoFileFolder findOrCreateFileFolder(DaDao daDao, DaChange change, String href) {
        DaDaoFileFolder fileFolder = null;
        List<DaDaoFileFolder> foundFileFolders = new ArrayList<>();
        List<DaDaoFileFolder> createdFileFolders = new ArrayList<>();
        List<DaDaoFileFolder> fileFolderList = daDaoFileFolderMap.getOrDefault(daDao.getDaoId(), new ArrayList<>());
        List<DaDaoFileFolder> newFileFolderList = newDaDaoFileFolderMap.getOrDefault(daDao.getDaoId(), new ArrayList<>());

        DaDaoFileFolder parentFileFolder = null;
        String[] pathArray = href.split("/");
        for (String path : pathArray) {
            if (!path.contains(".")) {
                fileFolder = findFileFolder(fileFolderList, path, parentFileFolder);
                if (fileFolder == null) {
                    fileFolder = findFileFolder(newFileFolderList, path, parentFileFolder);
                    if (fileFolder == null) {
                        fileFolder = daService.createDaDaoFileFolder(daDao, change, path, parentFileFolder);
                        createdFileFolders.add(fileFolder);
                    }
                } else {
                    foundFileFolders.add(fileFolder);
                }
                parentFileFolder = fileFolder;
            }
        }

        fileFolderList.removeAll(foundFileFolders);
        daDaoFileFolderMap.put(daDao.getDaoId(), fileFolderList);

        newFileFolderList.addAll(createdFileFolders);
        newFileFolderList.addAll(foundFileFolders);
        newDaDaoFileFolderMap.put(daDao.getDaoId(), newFileFolderList);

        return fileFolder;
    }

    @Nullable
    private DaDaoFileFolder findFileFolder(List<DaDaoFileFolder> fileFolderList, String label, DaDaoFileFolder parentFileFolder) {
        if (CollectionUtils.isNotEmpty(fileFolderList)) {
            for (DaDaoFileFolder fileFolder : fileFolderList) {
                if (fileFolder.getLabel().equals(label) && Objects.equals(fileFolder.getParentFileFolder(), parentFileFolder)) {
                    return fileFolder;
                }
            }
        }
        return null;
    }

    private void findOrCreateDaoRelation(DaDao daDao, DaDao parentDao, DaChange change) {
        List<DaDaoRelation> daoRelations = daDaoRelationMap.getOrDefault(daDao.getDaoId(), null);
        DaDaoRelation relation = findDaoRelation(parentDao, daDao, daoRelations);
        if (relation == null) {
            daService.createDaDaoRelation(daDao, parentDao, change);
        } else {
            daoRelations.remove(relation);
            daDaoRelationMap.put(daDao.getDaoId(), daoRelations);
        }
    }

    private boolean isDaoChanged(DaDao daDao, String code, String label, DaDao.DaoType type) {
        return !(daDao.getCode().equals(code) && daDao.getLabel().equals(label) && daDao.getType().equals(type));
    }

    @Nullable
    private DaDaoRelation findDaoRelation(DaDao parentDao, DaDao daDao, List<DaDaoRelation> daoRelations) {
        if (CollectionUtils.isNotEmpty(daoRelations)) {
            for (DaDaoRelation daDaoRelation : daoRelations) {
                if (daDaoRelation.getParentDao().equals(parentDao) && daDaoRelation.getDao().equals(daDao)) {
                    return daDaoRelation;
                }
            }
        }
        return null;
    }

    @Nullable
    private String findOriginalNameInPremis(PremisComplexType premisComplexType, String code) {
        for (ObjectComplexType objectComplexType : premisComplexType.getObject()) {
            if (objectComplexType instanceof File file) {
                for (ObjectIdentifierComplexType objectIdentifierComplexType : file.getObjectIdentifier()) {
                    if (objectIdentifierComplexType.getObjectIdentifierValue().equals(code)) {
                        return file.getOriginalName().getValue();
                    }
                }
            }
        }
        return null;
    }

    private void createDaoItemsFromArchDesc(Archdesc archdesc, DaChange change) {
        createDaoItemsFromDid(archdesc.getDid(), archdesc.getId(), change);
        for (Object a : archdesc.getAccessrestrictOrAccrualsOrAcqinfo()) {
            if (a instanceof Dsc dsc) {
                for (C c : dsc.getC()) {
                    createDaoItemsFromC(c, change);
                }
            }
        }
    }

    private void createDaoItemsFromC(C c, DaChange change) {
        createDaoItemsFromDid(c.getDid(), c.getId(), change);

        for (Object t : c.getTheadAndC()) {
            if (t instanceof C newC) {
                createDaoItemsFromC(newC, change);
            }
        }
    }

    private void createDaoItemsFromDid(Did did, String id, DaChange change) {
        DaDao daDao = logicalDaoMap.getOrDefault(id, null);

        if (daDao != null) {
            for (Object o : did.getMDid()) {
                String itemTypeCode = groovyScriptService.process(o.getClass().getSimpleName(), getGroovyFilePath());
                if (itemTypeCode != null) {
                    RulItemType itemType = staticDataService.getData().getItemType(itemTypeCode);
                    ArrData data = null;
                    if (o instanceof Abstract abs) {
                        String stringValue = null;
                        for (Serializable s : abs.getContent()) {
                            if (s instanceof String sValue) {
                                stringValue = sValue;
                            }
                        }
                        data = new ArrDataString(stringValue);
                        data.setDataType(DataType.STRING.getEntity());
                    } else if (o instanceof Unittitle unittitle) {
                        String stringValue = null;
                        for (Serializable s : unittitle.getContent()) {
                            if (s instanceof String sValue) {
                                stringValue = sValue;
                            }
                        }
                        data = new ArrDataString(stringValue);
                        data.setDataType(DataType.STRING.getEntity());
                    } else if (o instanceof Unitdatestructured uds) {
                        Daterange daterange = uds.getDaterange();

                        String date = "";
                        if (daterange.getFromdate() != null) {
                            LocalDate fromDate = LocalDate.parse(daterange.getFromdate().getStandarddate());
                            date += fromDate.format(FORMATTER_DATE);
                        }
                        date += "-";
                        if (daterange.getTodate() != null) {
                            LocalDate toDate = LocalDate.parse(daterange.getTodate().getStandarddate());
                            date += toDate.format(FORMATTER_DATE);
                        }

                        data = UnitDateConverter.convertToUnitDate(date, new ArrDataUnitdate());
                        data.setDataType(DataType.UNITDATE.getEntity());
                    }

                    if (data != null) {
                        dataRepository.save(data);
                        daService.createDaDaoItem(daDao, change, itemType, null, data);
                    }
                }
            }
        }
    }

    public String getGroovyFilePath() {
        StaticDataProvider sdp = staticDataService.getData();

        RulComponent component;
        RulPackage rulPackage;

        StructType structType = sdp.getStructuredTypeByCode(IMPORT_DA);

        List<RulStructureDefinition> structureDefinitions = structType
                .getDefsByType(RulStructureDefinition.DefType.SERIALIZED_VALUE);
        if (!structureDefinitions.isEmpty()) {
            RulStructureDefinition structureDefinition = structureDefinitions.get(structureDefinitions.size() - 1);
            component = structureDefinition.getComponent();
            rulPackage = structureDefinition.getRulPackage();
        } else {
            throw new SystemException("Strukturovaný typ '" + structType.getCode()
                    + "' nemá žádný script pro výpočet hodnoty", BaseCode.INVALID_STATE);
        }

        return resourcePathResolver.getGroovyDir(rulPackage)
                .resolve(component.getFilename())
                .toString();
    }

}
