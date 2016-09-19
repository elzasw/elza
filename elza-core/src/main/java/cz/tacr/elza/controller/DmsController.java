package cz.tacr.elza.controller;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.ArrFileVO;
import cz.tacr.elza.controller.vo.ArrOutputFileVO;
import cz.tacr.elza.controller.vo.DmsFileVO;
import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.repository.OutputResultRepository;
import cz.tacr.elza.service.DmsService;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nullable;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.function.Function;

/**
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.2016
 */
@RestController
/// @RequestMapping("/api/dms") - Mapping není funkční pro upload
public class DmsController {

    @Autowired
    private ClientFactoryDO factoryDO;

    @Autowired
    private ClientFactoryVO factoryVO;

    @Autowired
    private DmsService dmsService;

    @Autowired
    private OutputResultRepository outputResultRepository;

    /**
     * Vytvoření souboru
     * @param object objekt souboru
     * @throws IOException
     */
    @Transactional
    @RequestMapping(value = "/api/dms/common", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void createFile(final DmsFileVO object) throws IOException {
        create(object, factoryDO::createDmsFile);
    }

    /**
     * Vytvoření souboru
     * @param object objekt souboru
     * @throws IOException
     */
    @Transactional
    @RequestMapping(value = "/api/dms/fund", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArrFileVO createFile(final ArrFileVO object) throws IOException {
        dmsService.checkFundWritePermission(object.getFundId());
        DmsFile file = create(object, (fileVO) -> factoryDO.createArrFile((ArrFileVO) fileVO));
        return factoryVO.createArrFile((ArrFile) file);
    }

    /**
     * Vytvoření souboru
     * @param object objekt souboru
     * @throws IOException
     */
    @Transactional
    @RequestMapping(value = "/api/dms/output", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void createFile(final ArrOutputFileVO object) throws IOException {
        create(object, (fileVO) -> factoryDO.createArrOutputFile((ArrOutputFileVO) fileVO));
    }

    /**
     * Pomocná metoda pro vytvoření DMS souboru
     * @param objVO VO
     * @param factory továrna pro změnu VO na DO
     * @return DO pro vrácení klientovi
     * @throws IOException
     */
    private DmsFile create(final DmsFileVO objVO, final Function<DmsFileVO, DmsFile> factory) throws IOException {
        Assert.notNull(objVO);
        MultipartFile multipartFile = objVO.getFile();
        Assert.notNull(multipartFile);

        objVO.setFileName(multipartFile.getOriginalFilename());
        objVO.setMimeType(multipartFile.getContentType());
        objVO.setFileSize((int) multipartFile.getSize());

        DmsFile objDO = factory.apply(objVO);
        InputStream inputStream = multipartFile.getInputStream();
        dmsService.createFile(objDO, inputStream);
        inputStream.close();
        return objDO;
    }

    /**
     * Update souboru
     *
     * @param fileId id souboru
     * @param object objekt souboru
     * @throws IOException
     */
    @Transactional
    // kvůli IE nelze použít PUT protože nemůžeme uploadovat soubor
    @RequestMapping(value = "/api/dms/common/{fileId}", method = RequestMethod.POST)
    public void updateFile(@PathVariable(value = "fileId") Integer fileId, final DmsFileVO object) throws IOException {
        update(fileId, object, factoryDO::createDmsFile);
    }

    /**
     * Update souboru
     *
     * @param fileId id souboru
     * @param object objekt souboru
     * @throws IOException
     */
    @Transactional
    // kvůli IE nelze použít PUT protože nemůžeme uploadovat soubor
    @RequestMapping(value = "/api/dms/fund/{fileId}", method = RequestMethod.POST)
    public void updateFile(@PathVariable(value = "fileId") Integer fileId, final ArrFileVO object) throws IOException {
        dmsService.checkFundWritePermission(object.getFundId());
        update(fileId, object, (fileVO) -> factoryDO.createArrFile((ArrFileVO) fileVO));
    }

    /**
     * Update souboru
     *
     * @param fileId id souboru
     * @param object objekt souboru
     * @throws IOException
     */
    @Transactional
    // kvůli IE nelze použít PUT protože nemůžeme uploadovat soubor
    @RequestMapping(value = "/api/dms/output/{fileId}", method = RequestMethod.POST)
    public void updateFile(@PathVariable(value = "fileId") Integer fileId, final ArrOutputFileVO object) throws IOException {
        update(fileId, object, (fileVO) -> factoryDO.createArrOutputFile((ArrOutputFileVO) fileVO));
    }


    /**
     * Pomocná metoda pro úpravu DMS souboru
     * @param objVO VO
     * @param factory továrna pro změnu VO na DO
     * @return DO pro vrácení klientovi
     * @throws IOException
     */
    private DmsFile update(final Integer fileId, final DmsFileVO objVO, final Function<DmsFileVO, DmsFile> factory) throws IOException {
        Assert.notNull(fileId);
        Assert.notNull(objVO);
        Assert.isTrue(fileId.equals(objVO.getId()), "Id v URL neodpovídá ID objektu");

        DmsFile objDO = factory.apply(objVO);

        MultipartFile multipartFile = objVO.getFile();
        boolean hasInputFile = multipartFile != null;
        InputStream inputStream = null;
        if (hasInputFile) {
            inputStream = multipartFile.getInputStream();
            objDO.setFileName(multipartFile.getOriginalFilename());
            objDO.setMimeType(multipartFile.getContentType());
            objDO.setFileSize((int) multipartFile.getSize());
        }

        dmsService.updateFile(objDO, inputStream);
        if (hasInputFile) {
            inputStream.close();
        }
        return objDO;
    }

    /**
     * Stažení souboru
     * @param response http odpověd
     * @param fileId id souboru
     * @throws IOException
     */
    @RequestMapping(value = "/api/dms/{fileId}", method = RequestMethod.GET)
    public void getFile(HttpServletResponse response, @PathVariable(value = "fileId") Integer fileId) throws IOException {
        Assert.notNull(fileId);
        DmsFile file = dmsService.getFile(fileId);
        Assert.notNull(file, "Soubor s fileId " + fileId + " neexistuje!");
        response.setHeader("Content-Disposition", "attachment;filename="+file.getFileName());

        ServletOutputStream out = response.getOutputStream();
        InputStream in = dmsService.downloadFile(file);
        IOUtils.copy(in, out);
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);
    }

    /**
     * Stažení souboru
     * @param response http odpověd
     * @param outputResultId id souboru
     * @throws IOException
     */
    @RequestMapping(value = "/api/outputResult/{outputResultId}", method = RequestMethod.GET)
    public void getOutputResultZip(HttpServletResponse response, @PathVariable(value = "outputResultId") Integer outputResultId) throws IOException {
        Assert.notNull(outputResultId);
        ArrOutputResult result = outputResultRepository.getOneCheckExist(outputResultId);
        File outputFilesZip = dmsService.getOutputFilesZip(result);
        response.setHeader("Content-Disposition", "attachment;filename="+outputFilesZip.getName());

        ServletOutputStream out = response.getOutputStream();
        InputStream in = new BufferedInputStream(new FileInputStream(outputFilesZip));
        IOUtils.copy(in, out);
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);
        outputFilesZip.delete();
    }

    /**
     * Vyhledávání
     * @param search vyhledávaný text
     * @param from od záznamu
     * @param count  počet záznamů
     * @return list záznamů
     */
    @RequestMapping(value = "/api/dms/common", method = RequestMethod.GET)
    public FilteredResultVO<DmsFileVO> findFiles(@RequestParam(required = false) @Nullable final String search,
                                                    @RequestParam(required = false, defaultValue = "0") final Integer from,
                                                    @RequestParam(required = false, defaultValue = "20") final Integer count) {
        FilteredResult<DmsFile> files = dmsService.findDmsFiles(search, from, count);
        return new FilteredResultVO<>(factoryVO.createDmsFilesList(files.getList()), files.getTotalCount());
    }

    /**
     * Vyhledávání
     * @param search vyhledávaný text
     * @param from od záznamu
     * @param count  počet záznamů
     * @return list záznamů
     */
    @RequestMapping(value = "/api/dms/fund/{fundId}", method = RequestMethod.GET)
    public FilteredResultVO<ArrFileVO> findFundFiles(@PathVariable final Integer fundId,
                                                     @RequestParam(required = false) @Nullable final String search,
                                                    @RequestParam(required = false, defaultValue = "0") final Integer from,
                                                    @RequestParam(required = false, defaultValue = "20") final Integer count) {
        FilteredResult<ArrFile> files = dmsService.findArrFiles(search, fundId, from, count);
        return new FilteredResultVO<>(factoryVO.createArrFilesList(files.getList()), files.getTotalCount());
    }

    /**
     * Vyhledávání
     * @param search vyhledávaný text
     * @param from od záznamu
     * @param count  počet záznamů
     * @return list záznamů
     */
    @RequestMapping(value = "/api/dms/output/{outputResultId}", method = RequestMethod.GET)
    public FilteredResultVO<ArrOutputFileVO> findOutputFiles(@PathVariable final Integer outputResultId,
                                                     @RequestParam(required = false) @Nullable final String search,
                                                    @RequestParam(required = false, defaultValue = "0") final Integer from,
                                                    @RequestParam(required = false, defaultValue = "20") final Integer count) {
        FilteredResult<ArrOutputFile> files = dmsService.findOutputFiles(search, outputResultId, from, count);
        return new FilteredResultVO<>(factoryVO.createArrOutputFilesList(files.getList()), files.getTotalCount());
    }

    /**
     * Smazání souboru
     *
     * @param fileId id souboru
     */
    @Transactional
    @RequestMapping(value = "/api/dms/common/{fileId}", method = RequestMethod.DELETE)
    public void deleteFile(@PathVariable(value = "fileId") Integer fileId) throws IOException {
        dmsService.deleteFile(fileId);
    }

    /**
     * Smazání souboru
     *
     * @param fileId id souboru
     */
    @Transactional
    @RequestMapping(value = "/api/dms/fund/{fileId}", method = RequestMethod.DELETE)
    public void deleteArrFile(@PathVariable(value = "fileId") Integer fileId) throws IOException {
        ArrFile arrFile = dmsService.getArrFile(fileId);
        dmsService.deleteArrFile(arrFile, arrFile.getFund());
    }

    /**
     * Smazání souboru
     *
     * @param fileId id souboru
     */
    @Transactional
    @RequestMapping(value = "/api/dms/output/{fileId}", method = RequestMethod.DELETE)
    public void deleteOutputFile(@PathVariable(value = "fileId") Integer fileId) throws IOException {
        dmsService.deleteOutputFile(fileId);
    }
}
