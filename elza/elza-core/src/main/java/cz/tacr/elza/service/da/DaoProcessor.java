package cz.tacr.elza.service.da;

import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaAipState;
import cz.tacr.elza.domain.DaChange;
import cz.tacr.elza.domain.DaChangeType;
import cz.tacr.elza.domain.DaDao;
import cz.tacr.elza.domain.DaDaoFile;
import cz.tacr.elza.domain.DaDaoFileFolder;
import cz.tacr.elza.domain.DaDaoRelation;
import cz.tacr.elza.repository.AipStateRepository;
import cz.tacr.elza.repository.DaDaoFileFolderRepository;
import cz.tacr.elza.repository.DaDaoFileRepository;
import cz.tacr.elza.repository.DaDaoRelationRepository;
import cz.tacr.elza.repository.DaDaoRepository;
import cz.tacr.elza.service.DaoLevelViewService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class DaoProcessor {

    @Autowired
    private DaService daService;
    @Autowired
    private DaDaoRepository daoRepository;
    @Autowired
    private AipStateRepository aipStateRepository;
    @Autowired
    private DaDaoRelationRepository daoRelationRepository;
    @Autowired
    private DaDaoFileRepository daoFileRepository;
    @Autowired
    private DaDaoFileFolderRepository daoFileFolderRepository;
    @Autowired
    private DaoLevelViewService levelViewService;

    private final DaAip aip;

    private final MetsType metsType;

    private final PremisComplexType premisComplexType;

    private Map<String, DaDao> daDaoMap;

    private Map<String, List<DaDaoRelation>> daDaoRelationMap;

    private Map<String, List<DaDaoFileFolder>> daDaoFileFolderMap;

    private Map<String, List<DaDaoFile>> daDaoFileMap;

    private final Map<String, DaDao> fileDaoMap = new HashMap<>();

    private final List<String> representations = new ArrayList<>();

    private final Map<String, List<DaDaoFileFolder>> newDaDaoFileFolderMap = new HashMap<>();

    public DaoProcessor(DaAip aip, MetsType metsType, PremisComplexType premisComplexType) {
        this.aip = aip;
        this.metsType = metsType;
        this.premisComplexType = premisComplexType;
    }


    public boolean process() {
        List<DaDao> daDaoList = daoRepository.findByAipAndDeleteChangeIsNull(aip);
        daDaoMap = daDaoList.stream()
                .collect(Collectors.toMap(DaDao::getCode, Function.identity()));

        daDaoRelationMap = daoRelationRepository.findByDaoInAndDeleteChangeIsNull(daDaoList).stream()
                .collect(Collectors.groupingBy(r -> r.getDao().getCode()));
        daDaoFileFolderMap = daoFileFolderRepository.findByRepresentationDaoInAndDeleteChangeIsNull(daDaoList).stream()
                .collect(Collectors.groupingBy(f -> f.getRepresentationDao().getCode()));
        daDaoFileMap = daoFileRepository.findByDaoInAndDeleteChangeIsNull(daDaoList).stream()
                .collect(Collectors.groupingBy(f -> f.getDao().getCode()));

        DaAipState aipState = aipStateRepository.findByDaAipAndDeleteChangeIsNull(aip);
        DaChangeType changeType = daDaoMap.isEmpty() ? DaChangeType.AIP_UPDATE : DaChangeType.AIP_CREATE;
        DaChange change = daService.createDaChange(aip, changeType);

        //representation and files
        DaDao representationDao = createRepresentationDaoFromStruct(metsType.getStructMap(), change);
        if (metsType.getFileSec() != null) {
            createDaoFromFileSec(metsType.getFileSec(), representationDao, change);
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

        deleteOldComponents(change);
        levelViewService.processLevelViewForAip(aip);
        aipState.setCreateDaoStructure(true);
        aipStateRepository.save(aipState);
        return true;
    }

    private void deleteOldComponents(DaChange change) {
        //smazání starých komponent
        Set<DaDao> daDaoSet = new HashSet<>(daDaoMap.values());

        Set<DaDaoRelation> daDaoRelationSet = daDaoRelationMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());

        Set<DaDaoFileFolder> daDaoFileFolderSet = daDaoFileFolderMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());

        Set<DaDaoFile> daDaoFileSet = daDaoFileMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());

        daDaoSet.forEach(d -> d.setDeleteChange(change));
        daDaoRelationSet.forEach(r -> r.setDeleteChange(change));
        daDaoFileFolderSet.forEach(f -> f.setDeleteChange(change));
        daDaoFileSet.forEach(f -> f.setDeleteChange(change));

        daoRepository.saveAll(daDaoSet);
        daoRelationRepository.saveAll(daDaoRelationSet);
        daoFileFolderRepository.saveAll(daDaoFileFolderSet);
        daoFileRepository.saveAll(daDaoFileSet);
    }

    @Nullable
    private DaDao createRepresentationDaoFromStruct(List<StructMapType> structMap, DaChange change) {
        DaDao representationDaDao;
        for (StructMapType structMapType : structMap) {
            if (structMapType.getTYPE().equals("PHYSICAL")) {
                representationDaDao = createRepresentationDaoFromDiv(structMapType.getDiv(), change);
                if (representationDaDao != null) {
                    return representationDaDao;
                }
            }
        }
        return null;
    }

    @Nullable
    private DaDao createRepresentationDaoFromDiv(DivType divType, DaChange change) {
        DaDao representationDaDao;
        if (divType.getLABEL() != null && divType.getLABEL().equals("Representations")) {
            String code = divType.getID();
            DaDao.DaoType type = DaDao.DaoType.REPRESENTATION;
            String label = divType.getLABEL();
            DaDao daDao = daDaoMap.getOrDefault(code, null);
            if (daDao == null || isDaoChanged(daDao, code, label, type)) {
                daDao = daService.createDaDao(aip, change, code, label, type);
            } else {
                daDaoMap.remove(code);
            }

            if (CollectionUtils.isNotEmpty(divType.getFptr())) {
                for (DivType.Fptr fptr : divType.getFptr()) {
                    MetsType.FileSec.FileGrp fileGrp = (MetsType.FileSec.FileGrp) fptr.getFILEID();
                    representations.add(fileGrp.getID());
                }
            }
            return daDao;
        }

        if (CollectionUtils.isNotEmpty(divType.getDiv())) {
            for (DivType div : divType.getDiv()) {
                representationDaDao = createRepresentationDaoFromDiv(div, change);
                if (representationDaDao != null) {
                    return representationDaDao;
                }
            }
        }
        return null;
    }

    private void createDaoFromFileSec(MetsType.FileSec fileSec, DaDao representationDao, DaChange change) {
        if (CollectionUtils.isNotEmpty(fileSec.getFileGrp())) {
            for (FileGrpType fileGrpType : fileSec.getFileGrp()) {
                createDaoFromFileGrp(fileGrpType, representationDao, change);
            }
        }
    }

    private void createDaoFromFileGrp(FileGrpType fileGrpType, DaDao representationDao, DaChange change) {
        if (CollectionUtils.isNotEmpty(fileGrpType.getFile())) {
            boolean representation = representations.contains(fileGrpType.getID());
            for (FileType fileType : fileGrpType.getFile()) {
                createDaoFromFile(fileType, representationDao, change, representation);
            }
        }

        if (CollectionUtils.isNotEmpty(fileGrpType.getFileGrp())) {
            for (FileGrpType fileGrp : fileGrpType.getFileGrp()) {
                createDaoFromFileGrp(fileGrp, representationDao, change);
            }
        }
    }

    private void createDaoFromFile(FileType fileType, DaDao representationDao, DaChange change, boolean representation) {
        String code = fileType.getID();
        DaDao.DaoType type = DaDao.DaoType.FILE;
        String label = findOriginalNameInPremis(premisComplexType, code);
        if (label == null) {
            label = getFileHref(fileType);
        }

        DaDao daDao = daDaoMap.getOrDefault(code, null);
        if (daDao == null || isDaoChanged(daDao, code, label, type)) {
            daDao = daService.createDaDao(aip, change, code, label, type);
        } else {
            daDaoMap.remove(code);
        }

        if (representation) {
            findOrCreateDaoRelation(daDao, representationDao, change);
        }

        createFileFromFile(fileType, daDao, representationDao, change);
        fileDaoMap.put(code, daDao);
    }

    private void createFileFromFile(FileType fileType, DaDao daDao, DaDao representationDao, DaChange change) {
        String href = getFileHref(fileType);
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
                        label = mdSecType.getMdRef().getMDTYPE() + ":" + mdSecType.getMdRef().getHref();
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
            String label = findOriginalNameInPremis(premisComplexType, code);
            if (label == null) {
                label = mdSecType.getMdRef().getHref();
            }

            createDaoFromMdSecType(mdSecType, code, label, type, change);
        }
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
    }

    private DaDaoFile findOrCreateFile(DaChange change, DaDao dao, DaDaoFileFolder daoFileFolder, String checksum, String checksumType,
                                       String mimeType, BigInteger size, Integer imageHeight, Integer imageWidth, String sourceXDimensionUnit,
                                       Integer sourceXDimensionValue, String sourceYDimensionUnit, Integer sourceYDimensionValue,
                                       String duration, String description, String fileName) {
        List<DaDaoFile> daDaoFiles = daDaoFileMap.getOrDefault(dao.getCode(), new ArrayList<>());
        if (CollectionUtils.isNotEmpty(daDaoFiles)) {
            for (DaDaoFile daDaoFile : daDaoFiles) {
                if (isFileSame(daDaoFile, daoFileFolder, checksum, checksumType, mimeType, size, imageHeight, imageWidth, sourceXDimensionUnit,
                        sourceXDimensionValue, sourceYDimensionUnit, sourceYDimensionValue, duration, description, fileName)) {
                    daDaoFiles.remove(daDaoFile);
                    daDaoFileMap.put(dao.getCode(), daDaoFiles);
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
        List<DaDaoFileFolder> fileFolderList = daDaoFileFolderMap.getOrDefault(daDao.getCode(), new ArrayList<>());
        List<DaDaoFileFolder> newFileFolderList = newDaDaoFileFolderMap.getOrDefault(daDao.getCode(), new ArrayList<>());

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
        daDaoFileFolderMap.put(daDao.getCode(), fileFolderList);

        newFileFolderList.addAll(createdFileFolders);
        newFileFolderList.addAll(foundFileFolders);
        newDaDaoFileFolderMap.put(daDao.getCode(), newFileFolderList);

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
        List<DaDaoRelation> daoRelations = daDaoRelationMap.getOrDefault(daDao.getCode(), null);
        DaDaoRelation relation = findDaoRelation(parentDao, daDao, daoRelations);
        if (relation == null) {
            daService.createDaDaoRelation(daDao, parentDao, change);
        } else {
            daoRelations.remove(relation);
            daDaoRelationMap.put(daDao.getCode(), daoRelations);
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

}
