
/**
 * Please modify this class to meet your needs
 * This class is not complete
 */

package cz.tacr.elza.ws.core.v1;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoBatchInfo;
import cz.tacr.elza.domain.ArrDaoFile;
import cz.tacr.elza.domain.ArrDaoFileGroup;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.DigitizationCode;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.repository.DaoBatchInfoRepository;
import cz.tacr.elza.repository.DaoFileGroupRepository;
import cz.tacr.elza.repository.DaoFileRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.DaoPackageRepository;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.repository.DigitalRepositoryRepository;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.GroovyScriptService;
import cz.tacr.elza.ws.types.v1.ChecksumType;
import cz.tacr.elza.ws.types.v1.Dao;
import cz.tacr.elza.ws.types.v1.DaoBatchInfo;
import cz.tacr.elza.ws.types.v1.DaoImport;
import cz.tacr.elza.ws.types.v1.DaoLink;
import cz.tacr.elza.ws.types.v1.DaoPackage;
import cz.tacr.elza.ws.types.v1.Did;
import cz.tacr.elza.ws.types.v1.File;
import cz.tacr.elza.ws.types.v1.RelatedFileGroup;
import cz.tacr.elza.ws.types.v1.UnitOfMeasure;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * This class was generated by Apache CXF 3.1.8
 * 2016-12-09T13:06:28.035+01:00
 * Generated source version: 3.1.8
 *
 */

@Component
@javax.jws.WebService(
                      serviceName = "CoreService",
                      portName = "DaoCoreService",
                      targetNamespace = "http://elza.tacr.cz/ws/core/v1",
//                      wsdlLocation = "file:elza-core-v1.wsdl",
                      endpointInterface = "cz.tacr.elza.ws.core.v1.DaoService")

public class DaoCoreServiceImpl implements DaoService {

    private Log logger = LogFactory.getLog(this.getClass());

    @Autowired
    private DaoPackageRepository daoPackageRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private DaoRepository daoRepository;

    @Autowired
    private DaoFileGroupRepository daoFileGroupRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private DaoFileRepository daoFileRepository;

    @Autowired
    private DaoBatchInfoRepository daoBatchInfoRepository;

    @Autowired
    private GroovyScriptService groovyScriptService;

    @Autowired
    private DaoLinkRepository daoLinkRepository;

    @Autowired
    private DigitalRepositoryRepository digitalRepositoryRepository;

    @Autowired
    private cz.tacr.elza.service.DaoService daoService;

    @Autowired
    private ArrangementService arrangementService;

    /*
     * @see cz.tacr.elza.ws.core.v1.DaoService#_import(cz.tacr.elza.ws.types.v1.DaoImport daoImport)
     */
    @Transactional
    public void _import(DaoImport daoImport) throws CoreServiceException   {
        logger.info("Executing operation _import");
        Assert.notNull(daoImport);
        Assert.notNull(daoImport.getDaoPackages());
        Assert.notNull(daoImport.getDaoLinks());

        final List<DaoPackage> daoPackageList = daoImport.getDaoPackages().getDaoPackage();
        if (CollectionUtils.isNotEmpty(daoPackageList)) {
            for (DaoPackage daoPackage : daoPackageList) {
                final ArrDaoPackage arrDaoPackage = createArrDaoPackage(daoPackage);
            }
        }

        // založí se DaoLink bez notifikace, pokud již link existuje, tak se zruší a založí se nový (arr_change).
        final List<DaoLink> daoLinkList = daoImport.getDaoLinks().getDaoLink();
        if (CollectionUtils.isNotEmpty(daoLinkList)) {
            for (DaoLink daoLink : daoLinkList) {
                final ArrDaoLink arrDaoLink = createArrDaoLink(daoLink);
            }
        }

        logger.info("Finished operation _import");
    }

    /**
     * Založí se DaoLink bez notifikace, pokud již link existuje, tak se zruší a založí se nový (arr_change).
     *
     * @param daoLink @see cz.tacr.elza.ws.core.v1.DaoService#_import(cz.tacr.elza.ws.types.v1.DaoImport daoImport)
     * @return nově založený link
     */
    private ArrDaoLink createArrDaoLink(DaoLink daoLink) {
        Assert.notNull(daoLink);
        Assert.hasText(daoLink.getDaoIdentifier());
        Assert.hasText(daoLink.getDidIdentifier());

        final String daoIdentifier = daoLink.getDaoIdentifier();
        final String didIdentifier = daoLink.getDidIdentifier();
        final ArrDao arrDao = daoRepository.findOneByCode(daoIdentifier);
        final ArrNode arrNode = nodeRepository.findOneByUuid(didIdentifier);

        // daolink.daoIdentifier a didIdentifier existují a ukazují na shodný AS
        if (arrDao == null) {
            throw new ObjectNotFoundException(DigitizationCode.DAO_NOT_FOUND).set("code", daoLink.getDaoIdentifier());
        }
        if (arrNode == null) {
            throw new ObjectNotFoundException(ArrangementCode.NODE_NOT_FOUND).set("Uuid", daoLink.getDidIdentifier());
        }
        if (!arrNode.getFund().equals(arrDao.getDaoPackage().getFund())) {
            throw new BusinessException(DigitizationCode.DAO_AND_NODE_HAS_DIFFERENT_PACKAGE);
        }

        // kontrola existence linku, zrušení
        final List<ArrDaoLink> daoLinkList = daoLinkRepository.findByDaoAndNodeAndDeleteChangeIsNull(arrDao, arrNode);
        for (ArrDaoLink arrDaoLink : daoLinkList) {
            // vytvořit změnu
            final ArrChange deleteChange = arrangementService.createChange(ArrChange.Type.DELETE_DAO_LINK, arrNode);

            // nastavit připojení na neplatné
            arrDaoLink.setDeleteChange(deleteChange);
            logger.debug("Propojení arrDaoLink(ID=" + arrDaoLink.getDaoLinkId() + ") bylo automaticky zneplatněno novou změnou.");
            final ArrDaoLink resultDaoLink = daoLinkRepository.save(arrDaoLink);
        }

        // vytvořit změnu
        final ArrChange createChange = arrangementService.createChange(ArrChange.Type.CREATE_DAO_LINK, arrNode);

        // vytvořit připojení
        ArrDaoLink arrDaoLink = new ArrDaoLink();
        arrDaoLink.setCreateChange(createChange);
        arrDaoLink.setDao(arrDao);
        arrDaoLink.setNode(arrNode);
        logger.debug("Automaticky založeno nové propojení mezi DAO(ID=" + arrDao.getDaoId() + ") a node(ID=" + arrNode.getNodeId() + ").");
        return daoLinkRepository.save(arrDaoLink);
    }

    private ArrDaoPackage createArrDaoPackage(DaoPackage daoPackage) {
        Assert.notNull(daoPackage);

        ArrFund fund = fundRepository.findOneByUuid(daoPackage.getFundIdentifier());
        ArrDigitalRepository repository = digitalRepositoryRepository.findOneByCode(daoPackage.getRepositoryIdentifier());

        if (fund == null) {
            throw new ObjectNotFoundException(ArrangementCode.FUND_NOT_FOUND).set("uuid", daoPackage.getFundIdentifier());
        }
        if (repository == null) {
            throw new ObjectNotFoundException(DigitizationCode.REPOSITORY_NOT_FOUND).set("code", daoPackage.getRepositoryIdentifier());
        }

        ArrDaoPackage arrDaoPackage = new ArrDaoPackage();
        arrDaoPackage.setFund(fund);
        arrDaoPackage.setDigitalRepository(repository);
        arrDaoPackage.setCode(daoPackage.getIdentifier()); // TODO Lebeda - založit pokud nepřijde??

        final DaoBatchInfo daoBatchInfo = daoPackage.getDaoBatchInfo();
        if (daoBatchInfo != null) {
            ArrDaoBatchInfo arrDaoBatchInfo = daoBatchInfoRepository.findOneByCode(daoBatchInfo.getIdentifier());
            final String label = daoBatchInfo.getLabel();
            if (arrDaoBatchInfo == null) {
                arrDaoBatchInfo = new ArrDaoBatchInfo();
                arrDaoBatchInfo.setCode(daoBatchInfo.getIdentifier());
                arrDaoBatchInfo.setLabel(label);
                arrDaoBatchInfo = daoBatchInfoRepository.save(arrDaoBatchInfo);
            } else {
                if (!StringUtils.equals(arrDaoBatchInfo.getLabel(), label)) {
                    arrDaoBatchInfo.setLabel(label);
                    arrDaoBatchInfo = daoBatchInfoRepository.save(arrDaoBatchInfo);
                }
            }
            arrDaoPackage.setDaoBatchInfo(arrDaoBatchInfo);
        }

        arrDaoPackage = daoPackageRepository.save(arrDaoPackage);

        for (Dao dao : daoPackage.getDaoset().getDao()) {
            ArrDao arrDao = new ArrDao();
            arrDao.setCode(dao.getIdentifier());
            arrDao.setLabel(dao.getLabel());
            arrDao.setValid(true);
            arrDao.setDaoPackage(arrDaoPackage);
            arrDao = daoRepository.save(arrDao);

            for (File file : dao.getFileGroup().getFile()) {
                ArrDaoFile arrDaoFile = daoFileRepository.save(createArrDaoFile(arrDao, null, file));
            }

            for (RelatedFileGroup relatedFileGroup : dao.getRelatedFileGroup()) {
                ArrDaoFileGroup arrDaoFileGroup = new ArrDaoFileGroup();
                arrDaoFileGroup.setCode(relatedFileGroup.getIdentifier());
                arrDaoFileGroup.setLabel(relatedFileGroup.getLabel());
                arrDaoFileGroup.setDao(arrDao);
                arrDaoFileGroup = daoFileGroupRepository.save(arrDaoFileGroup);

                for (File file : relatedFileGroup.getFileGroup().getFile() ) {
                    ArrDaoFile arrDaoFile = daoFileRepository.save(createArrDaoFile(null, arrDaoFileGroup, file));
                }
            }
        }

        return arrDaoPackage;
    }

    private ArrDaoFile createArrDaoFile(@Nullable ArrDao arrDao, @Nullable ArrDaoFileGroup arrDaoFileGroup, File file) {
        ArrDaoFile arrDaoFile = new ArrDaoFile();
        arrDaoFile.setCode(file.getIdentifier());
        arrDaoFile.setChecksumType(getChecksumType(file.getChecksumType()));
        arrDaoFile.setChecksum(file.getChecksum());
        arrDaoFile.setCreated(file.getCreated().toGregorianCalendar().toZonedDateTime().toLocalDateTime());
        arrDaoFile.setMimetype(file.getMimetype());
        arrDaoFile.setSize(file.getSize());
        arrDaoFile.setImageHeight(file.getImageHeight().intValueExact());
        arrDaoFile.setImageWidth(file.getImageWidth().intValueExact());
        arrDaoFile.setSourceXDimesionUnit(getDimensionUnit(file.getSourceXDimensionUnit()));
        arrDaoFile.setSourceXDimesionValue(file.getSourceXDimensionValue().doubleValue());
        arrDaoFile.setSourceYDimesionUnit(getDimensionUnit(file.getSourceYDimensionUnit()));
        arrDaoFile.setSourceYDimesionValue(file.getSourceYDimensionValue().doubleValue());
        arrDaoFile.setDuration(file.getDuration());

        if (arrDao != null) {
            arrDaoFile.setDao(arrDao);
        }
        if (arrDaoFileGroup != null) {
            arrDaoFile.setDaoFileGroup(arrDaoFileGroup);
        }
        return arrDaoFile;
    }

    private cz.tacr.elza.api.UnitOfMeasure getDimensionUnit(UnitOfMeasure sourceDimensionUnit) {
        switch (sourceDimensionUnit) {
            case IN: return cz.tacr.elza.api.UnitOfMeasure.IN;
            case MM: return cz.tacr.elza.api.UnitOfMeasure.MM;
            default: throw new BusinessException(PackageCode.PARSE_ERROR);
        }
    }

    private cz.tacr.elza.api.ArrDaoFile.ChecksumType getChecksumType(ChecksumType checksumType) {
        switch (checksumType) {
            case MD_5: return cz.tacr.elza.api.ArrDaoFile.ChecksumType.MD5;
            case SHA_1: return cz.tacr.elza.api.ArrDaoFile.ChecksumType.SHA1;
            case SHA_256: return cz.tacr.elza.api.ArrDaoFile.ChecksumType.SHA256;
            case SHA_384: return cz.tacr.elza.api.ArrDaoFile.ChecksumType.SHA384;
            case SHA_512: return cz.tacr.elza.api.ArrDaoFile.ChecksumType.SHA512;
            default: throw new BusinessException(PackageCode.PARSE_ERROR);
        }
    }

    /*
     * @see cz.tacr.elza.ws.core.v1.DaoService#addPackage(cz.tacr.elza.ws.types.v1.DaoPackage daoPackage)*
     */
    @Transactional
    public String addPackage(DaoPackage daoPackage) throws CoreServiceException   {
        logger.info("Executing operation addPackage");
        final ArrDaoPackage arrDaoPackage = createArrDaoPackage(daoPackage);
        logger.info("Ending operation addPackage");
        return arrDaoPackage.getCode();
    }

    /* (non-Javadoc)
     * @see cz.tacr.elza.ws.core.v1.DaoService#removePackage(java.lang.String packageIdentifier)*
     */
    @Transactional
    public void removePackage(String packageIdentifier) throws CoreServiceException   {
        logger.info("Executing operation removePackage");
        Assert.hasText(packageIdentifier);

        final ArrDaoPackage arrDaoPackage = daoPackageRepository.findOneByCode(packageIdentifier);
        if (arrDaoPackage == null) {
            throw new ObjectNotFoundException(DigitizationCode.PACKAGE_NOT_FOUND).set("code", packageIdentifier);
        }

        final List<ArrDao> arrDaoList = daoService.deleteDaosWithoutLinks(daoRepository.findByPackage(arrDaoPackage));

        // TODO Lebeda - jak zneplatnit samotnou package

        logger.info("Ending operation removePackage");
    }

    /*
     * @see cz.tacr.elza.ws.core.v1.DaoService#link(cz.tacr.elza.ws.types.v1.DaoLink daoLink)*
     */
    @Transactional
    public void link(DaoLink daoLink) throws CoreServiceException   {
        logger.info("Executing operation link");

        final ArrDaoLink arrDaoLink = createArrDaoLink(daoLink);

        logger.info("Ending operation link");
    }

    /* (non-Javadoc)
     * @see cz.tacr.elza.ws.core.v1.DaoService#removeDao(java.lang.String packageIdentifier)*
     */
    @Transactional
    public void removeDao(String daoIdentifier) throws CoreServiceException   {
        logger.info("Executing operation removeDao");
        Assert.hasText(daoIdentifier);

        final ArrDao arrDao = daoRepository.findOneByCode(daoIdentifier);
        if (arrDao == null) {
            throw new ObjectNotFoundException(DigitizationCode.DAO_NOT_FOUND).set("code", daoIdentifier);
        }

        daoService.deleteDaosWithoutLinks(Collections.singletonList(arrDao));

        logger.info("Ending operation removeDao");
    }

    /* (non-Javadoc)
         * @see cz.tacr.elza.ws.core.v1.DaoService#getDid(java.lang.String packageIdentifier)*
         */
    public Did getDid(String nodeIdentifier) throws CoreServiceException {
        logger.info("Executing operation getDid");

        // TODO Lebeda - jak se dostanu z package na ArrNode???
        //        final ArrDaoPackage arrDaoPackage = daoPackageRepository.findOneByCode(packageIdentifier);
        //        Assert.notNull(arrDaoPackage);
        //
        //        final ArrFund arrFund = arrDaoPackage.getFund();
        //        Assert.notNull(arrFund);

        final ArrNode arrNode = nodeRepository.findOneByUuid(nodeIdentifier);
        Assert.notNull(arrNode, "Node ID=" + nodeIdentifier + " wasn't found.");

        final Did did = groovyScriptService.createDid(arrNode);

        logger.info("Ending operation getDid");
        return did;
    }

}
