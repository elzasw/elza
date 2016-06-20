package cz.tacr.elza.controller;

import cz.tacr.elza.controller.config.ClientFactoryDO;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.VOWithCount;
import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.domain.vo.DmsFileVO;
import cz.tacr.elza.service.DmsService;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nullable;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

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

    /**
     * Vytvoření souboru
     * @param fileVO objekt souboru
     * @throws IOException
     */
    @Transactional
    @RequestMapping(value = "/api/dms/dmsFiles", method = RequestMethod.POST)
    public void createFile(@RequestBody DmsFileVO fileVO) throws IOException {
        Assert.notNull(fileVO);
        MultipartFile file = fileVO.getFile();
        Assert.notNull(file);
        DmsFile dmsFile = factoryDO.createDmsFile(fileVO);
        dmsFile.setFileName(file.getName());
        dmsFile.setMimeType(file.getContentType());
        dmsFile.setFileSize((int) file.getSize());
        dmsService.storeFile(dmsFile);
        storeFile(fileVO.getFile(), dmsFile);
        dmsService.storeFile(dmsFile);
    }

    /**
     * Stažení souboru
     * @param response http odpověd
     * @param fileId id souboru
     * @throws IOException
     */
    @RequestMapping(value = "/api/dms/dmsFiles/{fileId}", method = RequestMethod.GET)
    public void getFile(HttpServletResponse response,  @PathVariable(value = "fileId") Integer fileId) throws IOException {
        Assert.notNull(fileId);
        DmsFile file = dmsService.getFile(fileId);
        response.setHeader("Content-Disposition", "attachment;filename="+file.getFileName());

        ServletOutputStream out = response.getOutputStream();
        FileInputStream na = new FileInputStream(dmsService.getFilePath(file));
        IOUtils.copy(na, out);
        IOUtils.closeQuietly(out);
    }

    /**
     * Smazání souboru
     *
     * @param fileId id souboru
     */
    @RequestMapping(value = "/api/dms/dmsFiles/{fileId}", method = RequestMethod.DELETE)
    public void deleteFile(@PathVariable(value = "fileId") Integer fileId) {
        dmsService.deleteFile(fileId);
    }

    /**
     * Update souboru
     *
     * @param fileId id souboru
     * @param fileVO objekt souboru
     * @throws IOException
     */
    @Transactional
    // kvůli IE nelze použít PUT protože nemůžeme uploadovat soubor
    @RequestMapping(value = "/api/dms/dmsFiles/{fileId}", method = RequestMethod.POST)
    public void updateFile(@PathVariable(value = "fileId") Integer fileId, @RequestBody DmsFileVO fileVO) throws IOException {
        Assert.notNull(fileId);
        Assert.notNull(fileVO);
        dmsService.getFile(fileId);
        DmsFile dmsFile = factoryDO.createDmsFile(fileVO);
        if (fileVO.getFile() != null) {
            MultipartFile file = fileVO.getFile();
            dmsFile.setFileName(file.getName());
            dmsFile.setMimeType(file.getContentType());
            dmsFile.setFileSize((int) file.getSize());
            storeFile(file, dmsFile);
        }
        dmsService.storeFile(dmsFile);
    }

    /**
     * Uložení souboru a získání počtu stran pokud je to PDF
     *
     * @param file soubor
     * @param dmsFile soubor v DB
     * @throws IOException
     */
    private void storeFile(final MultipartFile file, final DmsFile dmsFile) throws IOException {
        String filePath = dmsService.getFilePath(dmsFile);
        FileOutputStream out = new FileOutputStream(filePath);
        IOUtils.copy(file.getInputStream(), out);
        IOUtils.closeQuietly(out);

        if (file.getName().endsWith(".pdf")) {
            PDDocument reader = PDDocument.load(new File(filePath));
            dmsFile.setPagesCount(reader.getNumberOfPages());
            reader.close();
        } else {
            dmsFile.setPagesCount(null);
        }
    }

    /**
     * Vyhledávání
     * @param search vyhledávaný text
     * @param from od záznamu
     * @param count  počet záznamů
     * @return list záznamů
     */
    @RequestMapping(value = "/api/dms/dmsFiles", method = RequestMethod.GET)
    public VOWithCount<DmsFileVO> findFiles(@RequestParam(required = false) @Nullable final String search,
                                 @RequestParam final Integer from,
                                 @RequestParam final Integer count) {
        final long foundCount = dmsService.findFileByTextCount(search);

        if (foundCount == 0) {
            return new VOWithCount<>(Collections.EMPTY_LIST, foundCount);
        }

        List<DmsFile> foundFiles = dmsService.findFileByText(search, from, count);
        return new VOWithCount<>(factoryVO.createFilesList(foundFiles), foundCount);
    }
}
