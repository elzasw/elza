package cz.tacr.elza.controller;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import jakarta.annotation.Nullable;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cz.tacr.elza.common.CloseablePathResource;
import cz.tacr.elza.common.FileDownload;
import cz.tacr.elza.controller.vo.ArrFileVO;
import cz.tacr.elza.controller.vo.ArrOutputFileVO;
import cz.tacr.elza.controller.vo.DmsFileVO;
import cz.tacr.elza.controller.vo.FilteredResultVO;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrChange.Type;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.OutputRepository;
import cz.tacr.elza.repository.OutputResultRepository;
import cz.tacr.elza.security.AuthorizationRequest;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.DmsService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.attachment.AttachmentService;
import cz.tacr.elza.service.dao.FileSystemRepoService;

/**
 * 
 * @since 20.6.2016
 */
@RestController
public class DmsController {

    private ThreadLocal<SimpleDateFormat> FORMATTER_DATE_TIME = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss"));

    @Autowired
    private OutputResultRepository outputResultRepository;

    @Autowired
    private OutputRepository outputRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private ArrangementInternalService arrangementInternalService;

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private DmsService dmsService;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private FileSystemRepoService fileSystemRepoService;
    
    @Autowired
    private UserService userService;    

    /**
     * Načtení seznamu editovatelných mime typů.
     *
     * @return seznam editovatelných mime typů
     */
    @RequestMapping(value = "/api/attachment/mimeTypes", method = RequestMethod.GET)
    public List<String> getMimeTypes() {
        return attachmentService.getEditableMimeTypes();
    }

    /**
     * Vytvoření souboru
     * @param arrFile objekt souboru
     * @throws IOException
     */
    @Transactional
    @RequestMapping(value = "/api/dms/fund", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArrFileVO createFile(final ArrFileVO arrFile) throws IOException {
        dmsService.checkFundWritePermission(arrFile.getFundId());

        ArrChange createChange = arrangementInternalService.createChange(Type.ADD_ATTACHMENT);
        ArrFile file = create(arrFile, (fileVO) -> arrFile.createEntity(fundRepository, createChange));
        return ArrFileVO.newInstance(file, attachmentService);
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
        ArrFile arrFile = dmsService.getArrFile(fileId);
        dmsService.checkFundWritePermission(arrFile.getFund().getFundId());
        update(fileId, object, (fileVO) -> object.createEntity(fundRepository, null));
    }

    /**
     * Pomocná metoda pro úpravu DMS souboru
     * @param dmsFileVO VO
     * @param factory továrna pro změnu VO na DO
     * @return DO pro vrácení klientovi
     * @throws IOException
     */
    private <T extends DmsFile> T update(final Integer fileId, final DmsFileVO dmsFileVO,
                                         final Function<DmsFileVO, T> factory) throws IOException {
        Validate.notNull(fileId, "Identifikátor souboru musí být vyplněn");
        Validate.notNull(dmsFileVO, "Soubor musí být vyplněn");
        Validate.isTrue(fileId.equals(dmsFileVO.getId()), "Id v URL neodpovídá ID objektu");

        final FileInfo fileInfo = getFileInfo(dmsFileVO);
        if (fileInfo != null) {
            updateDmsFile(dmsFileVO, fileInfo);
        }

        T objDO = factory.apply(dmsFileVO);
        final InputStream inputStream = fileInfo != null ? fileInfo.getInputStream() : null;

        dmsService.updateFile(objDO, inputStream);

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
        Validate.notNull(fileId, "Identifikátor souboru musí být vyplněn");
        DmsFile file = dmsService.getFile(fileId);
        Validate.notNull(file, "Soubor s fileId %s neexistuje!", fileId);

        dmsService.checkFundReadPermission(file.getFundId());

        FileDownload.addContentDispositionAsAttachment(response, file.getFileName());

        try (ServletOutputStream out = response.getOutputStream();
                InputStream in = dmsService.newInputStream(file);) {
            IOUtils.copy(in, out);
        }
    }

    // TODO: In Spring6 change to: "/api/digirepo/{repoId}/{*filePath}"
    @RequestMapping(value = "/api/digirepo/{repoId}", method = RequestMethod.GET)
    @Transactional
    public void getFile(HttpServletResponse response, @PathVariable(value = "repoId") Integer repoId,
                        @RequestParam(value = "filePath") String filePath)
            throws IOException {
        // check permissions RD_ALL 
        userService.authorizeRequest(AuthorizationRequest.hasPermission(UsrPermission.Permission.FUND_RD_ALL));

        // read file repo
        ArrDigitalRepository digiRep = externalSystemService.getDigitalRepository(repoId);

        Path fp = fileSystemRepoService.resolvePath(digiRep, filePath);
        String contentType = fileSystemRepoService.getMimetype(filePath);
        if (StringUtils.isEmpty(contentType)) {
            contentType = "application/binary";
            FileDownload.addContentDispositionAsAttachment(response, fp.getFileName().toString());
        }
        response.setContentType(filePath);
        
        try (ServletOutputStream out = response.getOutputStream();
                InputStream in = fileSystemRepoService.getInputStream(digiRep, filePath);) {
            IOUtils.copy(in, out);
        }

    }    

    /**
     * Stažení souboru
     * @param response http odpověd
     * @param outputResultId id souboru
     * @throws IOException
     */
    @RequestMapping(value = "/api/outputResults/{outputId}", method = RequestMethod.GET)
    @Transactional
    public void getOutputResultsZip(HttpServletResponse response, @PathVariable(value = "outputId") Integer outputId) throws IOException {
        Validate.notNull(outputId, "Identifikátor výstupu musí být vyplněn");
        ArrOutput output = outputRepository.findByOutputId(outputId);

        dmsService.checkFundReadPermission(output.getFundId());

        List<ArrOutputResult> outputResults = outputResultRepository.findByOutput(output);
        List<ArrOutputFile> outputFiles = new ArrayList<>();
		for(ArrOutputResult outputResult: outputResults) {
        	outputFiles.addAll(outputResult.getOutputFiles());
        }

        ServletOutputStream out = response.getOutputStream();

        File fileForDownload = null;
        String fileName;

        InputStream in = null;
        try {
            if (outputFiles.size() == 1) {
                // single file download directly
                ArrOutputFile singleFile = outputFiles.get(0);
                in = dmsService.newInputStream(singleFile);
                fileName = singleFile.getFileName();

            } else {
                // multiple files have to be zipped
                fileForDownload = dmsService.getOutputFilesZip(outputFiles).toFile();
                fileName = output.getName() + ".zip";
                in = new BufferedInputStream(new FileInputStream(fileForDownload));
            }
            FileDownload.addContentDispositionAsAttachment(response, fileName);

            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
            if (fileForDownload != null) {
                fileForDownload.delete();
            }
        }
    }

    /**
     * Stažení souboru
     * @param response http odpověd
     * @param outputResultId id souboru
     * @throws IOException
     */
    @RequestMapping(value = "/api/outputResult/{outputResultId}", method = RequestMethod.GET)
    @Transactional
    public void getOutputResultZip(HttpServletResponse response, @PathVariable(value = "outputResultId") Integer outputResultId) throws IOException {
        Validate.notNull(outputResultId, "Identifikátor výstupu musí být vyplněn");
        ArrOutputResult result = outputResultRepository.getOneCheckExist(outputResultId);
        ArrOutput output = result.getOutput();

        dmsService.checkFundReadPermission(output.getFundId());

        // check number of files
        List<ArrOutputFile> outputFiles = result.getOutputFiles();

        ServletOutputStream out = response.getOutputStream();

        File fileForDownload = null;
        String fileName;

        InputStream in = null;
        try {
            if (outputFiles.size() == 1) {
                // single file download directly
                ArrOutputFile singleFile = outputFiles.get(0);
                in = dmsService.newInputStream(singleFile);
                fileName = singleFile.getFileName();

            } else {
                // multiple files have to be zipped
                fileForDownload = dmsService.getOutputFilesZip(result.getOutputFiles()).toFile();
                fileName = output.getName() + ".zip";
                in = new BufferedInputStream(new FileInputStream(fileForDownload));
            }
            FileDownload.addContentDispositionAsAttachment(response, fileName);

            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
            if (fileForDownload != null) {
                fileForDownload.delete();
            }
        }
    }

    /**
     * Vyhledávání
     * @param search vyhledávaný text
     * @param from od záznamu
     * @param count  počet záznamů
     * @return list záznamů
     */
    @RequestMapping(value = "/api/dms/fund/{fundId}", method = RequestMethod.GET)
	@Transactional
    public FilteredResultVO<ArrFileVO> findFundFiles(@PathVariable final Integer fundId,
                                                     @RequestParam(required = false) @Nullable final String search,
                                                    @RequestParam(required = false, defaultValue = "0") final Integer from,
                                                    @RequestParam(required = false, defaultValue = "20") final Integer count) {
        dmsService.checkFundReadPermission(fundId);
        FilteredResult<ArrFile> files = dmsService.findArrFiles(search, fundId, from, count);
        return new FilteredResultVO<>(files.getList(), (entity) -> ArrFileVO.newInstance(entity, attachmentService),
                files.getTotalCount());
    }

    /**
     * Načtení konkrétního objektu s informacemi o souboru s vyžádáním obsahu pro editovatelný (text) soubor.
     *
     * @param fundId id AS
     * @param fileId id souboru
     * @return list záznamů
     */
    @RequestMapping(value = "/api/dms/fund/{fundId}/{fileId}", method = RequestMethod.GET)
    public ArrFileVO getEditableFundFile(@PathVariable("fundId") final Integer fundId,
                                         @PathVariable("fileId") final Integer fileId) throws IOException {
        Assert.notNull(fundId, "Fund id musí být uvedeno");
        dmsService.checkFundReadPermission(fundId);

        ArrFile file = dmsService.getArrFile(fileId);
        Assert.isTrue(fundId.equals(file.getFund().getFundId()), "Nesouhlasí id AS");

        ArrFileVO result = ArrFileVO.newInstance(file, attachmentService);

        if (attachmentService.isEditable(file.getMimeType())) {
	        try (InputStream is = dmsService.newInputStream(file)) {
	            String text = IOUtils.toString(is, "utf-8");
	            result.setContent(text);
	        }
        }

        return result;
    }

    /**
     * Načtení konkrétního objektu s informacemi o souboru s vyžádáním obsahu pro editovatelný soubor.
     *
     * @param fundId id AS
     * @param fileId id souboru
     * @return list záznamů
     */
    @RequestMapping(value = "/api/dms/fund/{fundId}/{fileId}/generated", method = RequestMethod.GET)
    public void generateEditableFundFile(HttpServletResponse response,
                                         @PathVariable("fundId") final Integer fundId,
                                         @PathVariable("fileId") final Integer fileId,
                                         @RequestParam("mimeType") final String mimeType) throws IOException {
        Assert.notNull(fundId, "Fund id musí být uvedeno");
        dmsService.checkFundReadPermission(fundId);

        ArrFile file = dmsService.getArrFile(fileId);
        Assert.isTrue(fundId.equals(file.getFund().getFundId()), "Nesouhlasí id AS");

        ServletOutputStream out = response.getOutputStream();

        try (CloseablePathResource output = attachmentService.generate(file, mimeType)) {
            FileDownload.addContentDispositionAsAttachment(response, output.getFilename());
            response.setContentLengthLong(output.contentLength());

            // Copy to output stream
            output.writeTo(out);

            IOUtils.closeQuietly(out);
        }
    }

    /**
     * Vyhledávání
     * @param outputId ID output
     * @return list záznamů
     */
    @RequestMapping(value = "/api/dms/output/{outputId}", method = RequestMethod.GET)
    @Transactional
    public FilteredResultVO<ArrOutputFileVO> findOutputFiles(@PathVariable final Integer outputId) {
    	ArrOutput output = outputRepository.findByOutputId(outputId);
        dmsService.checkFundReadPermission(output.getFundId());

    	List<ArrOutputFile> outputFiles = dmsService.findOutputFiles(output.getFundId(), output);

        return new FilteredResultVO<>(outputFiles,
                (entity) -> ArrOutputFileVO.newInstance(entity),
                outputFiles.size());
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
     * Aktualizuje vo objekt z předaných informací o souboru.
     * @param dmsFile vo pro aktualizaci - do něj se zapisuje
     * @param fileInfo vstupní objekt s daty pro zapsání
     */
    private void updateDmsFile(final DmsFileVO dmsFile, final FileInfo fileInfo) {
        dmsFile.setFileSize((int) fileInfo.getFileSize());
        dmsFile.setFileName(fileInfo.getFileName());
        dmsFile.setMimeType(fileInfo.getMimeType());
    }

    /**
     * Pomocná metoda pro vytvoření DMS souboru
     * @param dmsFile VO
     * @param factory továrna pro změnu VO na DO
     * @return DO pro vrácení klientovi
     * @throws IOException
     */
    private <T extends DmsFile> T create(final DmsFileVO dmsFile, final Function<DmsFileVO, T> factory)
            throws IOException {
        Assert.notNull(dmsFile, "Soubor musí být vyplněn");

        final FileInfo fileInfo = getFileInfo(dmsFile);
        if (fileInfo == null) {
            throw new BusinessException("Soubor nebo textový obsah souboru musí být vyplněny", BaseCode.PROPERTY_NOT_EXIST).set("property", "file, content");
        }

        updateDmsFile(dmsFile, fileInfo);

        T objDO = factory.apply(dmsFile);

        final InputStream inputStream = fileInfo.getInputStream();
        dmsService.createFile(objDO, inputStream);

        return objDO;
    }

    /**
     * Načtení informací o souboru i s ohledem na typ dat - ze souboru, editovatelná apod.
     *
     * @param dmsFile vo objekt z klienta
     * @return info o souboru a jeho datech nebo null, pokud se v datech soubor nevyskytoval
     * @throws IOException chyba
     */
    private FileInfo getFileInfo(final DmsFileVO dmsFile) throws IOException {
        final MultipartFile multipartFile = dmsFile.getFile();
        final String content = dmsFile.getContent();
        final InputStream inputStream;
        final String mimeType;
        final long fileSize;
        final String fileName;

        if (multipartFile != null) {
            mimeType = multipartFile.getContentType();
            fileSize = multipartFile.getSize();
            fileName = FilenameUtils.getName(multipartFile.getOriginalFilename());
            inputStream = multipartFile.getInputStream();
        } else if (content != null) {
            mimeType = dmsFile.getMimeType();

            if (StringUtils.isEmpty(dmsFile.getFileName())) {
                String baseFileName = "attachment-" + FORMATTER_DATE_TIME.get().format(new Date());

                String fileExtension = null;
                try {
                    // TODO [stanekpa] - chceme v budoucnu nějak řešit? MimeTypes tuto informaci má, ale mapu přípony na mime typ má jako private, případně by šlo použít Apache Tika
                    //                MimeType mimeType = MimeType.valueOf(objVO.getMimeType());
                    fileExtension = "dat";
                } catch (Exception ex) {
                    fileExtension = "dat";
                }
                fileName = baseFileName + "." + fileExtension;
            } else {
                fileName = dmsFile.getFileName();
            }

            fileSize = content.length();

            inputStream = new ByteArrayInputStream(content.getBytes());
        } else {
            return null;
        }

        return new FileInfo(inputStream, fileSize, fileName, mimeType);
    }

    private static class FileInfo {
        private InputStream inputStream;
        private long fileSize;
        private String fileName;
        private String mimeType;

        public FileInfo(final InputStream inputStream, final long fileSize, final String fileName, final String mimeType) {
            this.setInputStream(inputStream);
            this.setFileSize(fileSize);
            this.setFileName(fileName);
            this.setMimeType(mimeType);
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public void setInputStream(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        public long getFileSize() {
            return fileSize;
        }

        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }
    }
}
