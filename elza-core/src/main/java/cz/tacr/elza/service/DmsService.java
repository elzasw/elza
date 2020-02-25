package cz.tacr.elza.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.transaction.Transactional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.common.AutoDeletingTempFile;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.repository.FileRepository;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.repository.FundFileRepository;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.OutputFileRepository;
import cz.tacr.elza.repository.OutputResultRepository;
import cz.tacr.elza.security.AuthorizationRequest;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventStringInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.exception.DeleteFailedException;

/**
 * Dms Service
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.2016
 */
@Service
public class DmsService {

    public static final String MIME_TYPE_APPLICATION_PDF = "application/pdf";

    @Autowired
    private ResourcePathResolver resourcePathResolver;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FundFileRepository fundFileRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private OutputFileRepository outputFileRepository;

    @Autowired
    private OutputResultRepository outputResultRepository;

    @Autowired
    private EventNotificationService eventNotificationService;
    
    @Autowired
    private UserService userService;

    /**
     * Uloží DMS soubor se streamem a publishne event
     *
     * @param dmsFile    DO
     * @param fileStream stream pro uložení souboru, po použití jej uzavře
     * @throws IOException
     */
    public void createFile(final DmsFile dmsFile, final InputStream fileStream) throws IOException {
        Validate.notNull(dmsFile, "Soubor musí být vyplněn");
        Validate.notNull(fileStream, "Stream souboru musí být vyplněn");

        fileRepository.save(dmsFile);

        File outputFile = getFilePath(dmsFile).toFile();
        if (outputFile.exists()) {
            throw new SystemException("Nelze soubor již existuje: " + outputFile.getPath(), ArrangementCode.ALREADY_CREATED);
        }
        saveFile(dmsFile, fileStream, outputFile);

        fileRepository.save(dmsFile);
        publishFileChange(dmsFile);
    }

    /**
     * Uloží DMS soubor se streamem a publishne event
     *
     * @param dmsFile
     *            DO
     * @param fileStream
     *            stream pro uložení souboru, po použití jej uzavře
     * @throws IOException
     */
    public void createFile(final DmsFile dmsFile, final Consumer<OutputStream> dataProvider) throws IOException {
        Validate.notNull(dmsFile, "Soubor musí být vyplněn");
        Validate.notNull(dataProvider, "DataProvider souboru musí být vyplněn");

        fileRepository.save(dmsFile);

        File outputFile = getFilePath(dmsFile).toFile();
        if (outputFile.exists()) {
            throw new SystemException("Nelze soubor již existuje: " + outputFile.getPath(),
                    ArrangementCode.ALREADY_CREATED);
        }
        saveFile(dmsFile, dataProvider, outputFile);

        fileRepository.save(dmsFile);
        publishFileChange(dmsFile);
    }

    /**
     * Publish eventu
     *
     * @param dmsFile
     *            dms sobor
     */
    private void publishFileChange(final DmsFile dmsFile) {
        Integer notifyId;
        if (dmsFile instanceof ArrFile) {
            ArrFile file = (ArrFile) dmsFile;
            notifyId = file.getFund().getFundId();
        } else if (dmsFile instanceof ArrOutputFile) {
            ArrOutputFile file = (ArrOutputFile) dmsFile;
            notifyId = file.getOutputResult().getOutputResultId();
        } else {
            return;
        }
        EventStringInVersion event = EventFactory.createStringInVersionEvent(EventType.FILES_CHANGE, notifyId, dmsFile.getClass().getSimpleName());
        eventNotificationService.publishEvent(event);
    }

    /**
     * Ověření oprávnění na zápis souborů.
     *
     * @param fundId identifikátor archivního souboru
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR, UsrPermission.Permission.FUND_ARR_ALL})
    public void checkFundWritePermission(@AuthParam(type = AuthParam.Type.FUND) final Integer fundId) {
        Assert.notNull(fundId, "Nebyl vyplněn identifikátor AS");
    }

    /**
     * Úprava detailů souboru či jeho nahrazení
     *
     * @param newFile    nové DO DMS file
     * @param fileStream Stream, po použití jej uzavře
     * @throws IOException
     */
    public void updateFile(final DmsFile newFile, final InputStream fileStream) throws IOException {
        Assert.notNull(newFile, "Soubor musí být vyplněn");
        Assert.notNull(newFile.getFileId(), "Identifikátor souboru musí být vyplněn");

        DmsFile dbFile = getFile(newFile.getFileId());

        if (newFile.getFileName() != null) {
            dbFile.setFileName(newFile.getFileName());
        }
        if (newFile.getMimeType() != null) {
            dbFile.setMimeType(newFile.getMimeType());
        }
        if (newFile.getFileSize() != null) {
            dbFile.setFileSize(newFile.getFileSize());
        }


        if (newFile.getName() != null && !newFile.getName().isEmpty()) {
            dbFile.setName(newFile.getName());
        }

        fileRepository.save(dbFile);
        if (fileStream != null) {
            File outputFile = getFilePath(dbFile).toFile();
            if (outputFile.exists() && !outputFile.delete()) {
                throw new SystemException("Nelze odstranit existující soubor");
            }
            saveFile(dbFile, fileStream, outputFile);

            fileRepository.save(dbFile);
        }
        publishFileChange(dbFile);
    }

    /**
     * Vrátí stream pro stažení souboru
     *
     * @param resourcePathResolver
     *            Resource resolver
     * @param dmsFile
     *            dms Soubor ke stažení
     * @return stream
     */
    static public InputStream downloadFile(final ResourcePathResolver resourcePathResolver, final DmsFile dmsFile) {
        Validate.notNull(dmsFile, "Soubor musí být vyplněn");

        Path dmsFilePath = resourcePathResolver.getDmsDir().resolve(dmsFile.getFileId().toString());
        //File outputFile = 
        if (!Files.exists(dmsFilePath)) {
            throw new SystemException("Požadovaný soubor neexistuje")
                    .set("fileId", dmsFile.getFileId().toString())
                    .set("filePath", dmsFilePath.toString());
        }

        try {
            return new BufferedInputStream(Files.newInputStream(dmsFilePath));
        } catch (IOException e) {
            throw new SystemException("Požadovaný soubor nebyl nalezen", e);
        }
    }

    /**
     * Vrátí stream pro stažení souboru
     *
     * @param dmsFile
     *            dms Soubor ke stažení
     * @return stream
     */
    public InputStream downloadFile(final DmsFile dmsFile) {
        return downloadFile(this.resourcePathResolver, dmsFile);
    }

    /**
     * Smazání DMS file pomocí ID
     *
     * @param fileId ID
     * @throws IOException
     */
    public void deleteFile(final Integer fileId) throws IOException {
        deleteFile(fileRepository.getOneCheckExist(fileId));
    }


    /**
     * Smazání Arr file pomocí ID
     *
     * @param file soubor
     * @param fund archivní soubor
     * @throws IOException
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_RD, UsrPermission.Permission.FUND_RD_ALL})
    public void deleteArrFile(final ArrFile file,
                              @AuthParam(type = AuthParam.Type.FUND) final ArrFund fund) throws IOException {
        deleteFile(file);
    }


    /**
     * Smazání Output file pomocí ID
     *
     * @param outputFileId ID
     * @throws IOException
     */
    public void deleteOutputFile(final Integer outputFileId) throws IOException {
        deleteFile(outputFileRepository.getOneCheckExist(outputFileId));
    }

    /**
     * Smazání DMS soboru včetně reálného souboru
     *
     * @param dmsFile DMS soubor ke smazání
     * @throws IOException
     */
    public void deleteFile(final DmsFile dmsFile) throws IOException {
        Assert.notNull(dmsFile, "Soubor musí být vyplněn");

        fileRepository.delete(dmsFile);

        File outputFile = getFilePath(dmsFile).toFile();
        if (outputFile.exists() && !outputFile.delete()) {
            throw new SystemException("Nelze odstranit existující soubor");
        }
        publishFileChange(dmsFile);
    }

    /**
     * Uložení do souboru
     *
     * @param dmsFile    DO
     * @param fileStream stream souboru, po použití jej uzavře
     * @param outputFile místo k uložení souboru
     * @throws IOException
     */
    private void saveFile(final DmsFile dmsFile, final InputStream fileStream, final File outputFile) throws IOException {

        saveFile(dmsFile, (outputStream) -> {
            try {
                IOUtils.copy(fileStream, outputStream);
            } catch (IOException e) {
                throw new SystemException("Failed to copy to the output", e);
            } finally {
                IOUtils.closeQuietly(fileStream);
            }
        }, outputFile);
    }

    /**
     * Uložení do souboru
     *
     * @param dmsFile
     *            DO
     * @param dataProvider
     *            Funkce, ktera provede ulozeni do vysledneho streamu
     * @param outputFile
     *            místo k uložení souboru
     * @throws IOException
     */
    private void saveFile(final DmsFile dmsFile, final Consumer<OutputStream> dataProvider, final File outputFile)
            throws IOException {

        FileUtils.touch(outputFile);

        try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile, false))) {
            dataProvider.accept(outputStream);
        } catch (IOException e) {
            if (outputFile.exists()) {
                FileUtils.forceDelete(outputFile);
            }
            throw e;
        }

        dmsFile.setFileSize((int) outputFile.length());

        if (dmsFile.getMimeType().toLowerCase().equals(MIME_TYPE_APPLICATION_PDF)) {
            PDDocument reader = PDDocument.load(outputFile);
            dmsFile.setPagesCount(reader.getNumberOfPages());
            reader.close();
        } else {
            dmsFile.setPagesCount(null);
        }
    }

    /**
     * Zíkání cesty k reálnému souboru
     *
     * @param file dms file
     * @return cesta
     */
    public Path getFilePath(final DmsFile file) {
        int fileId = file.getFileId();
        return getFilePath(fileId);
    }

    /**
     * Zíkání cesty k reálnému souboru
     *
     * @param fileId dms file id
     * @return cesta
     */
    public Path getFilePath(final int fileId) {
        String strFileId = Integer.toString(fileId);
        return resourcePathResolver.getDmsDir().resolve(strFileId);
    }

    /**
     * Vrátí soubor dle id
     *
     * @param fileId id souboru
     * @return soubor
     */
    public DmsFile getFile(final Integer fileId) {
        Assert.notNull(fileId, "Identifikátor souboru musí být vyplněn");
        return fileRepository.getOneCheckExist(fileId);
    }

    /**
     * Vyhledávání DMS file
     *
     * @param search text
     * @param from   od záznamu
     * @param count  počet
     * @return filtrovaný list
     */
    public FilteredResult<DmsFile> findDmsFiles(final String search, final Integer from, final Integer count) {
        return fileRepository.findByText(search, from, count);
    }

    /**
     * Vyhledávání Arr file
     *
     * @param search text
     * @param from   od záznamu
     * @param count  počet
     * @return filtrovaný list
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_RD, UsrPermission.Permission.FUND_RD_ALL})
    public FilteredResult<ArrFile> findArrFiles(final String search,
                                                @AuthParam(type = AuthParam.Type.FUND) final Integer fundId,
                                                final Integer from,
                                                final Integer count) {
        Assert.notNull(fundId, "Nebyl vyplněn identifikátor AS");
        return fundFileRepository.findByTextAndFund(search, fundRepository.getOneCheckExist(fundId), from, count);
    }


    /**
     * Vyhledávání Arr file
     *
     * @param search text
     * @param from   od záznamu
     * @param count  počet
     * @return filtrovaný list
     */
	@Transactional
    public FilteredResult<ArrOutputFile> findOutputFiles(final String search, final Integer outputResultId, final Integer from, final Integer count) {
		Assert.notNull(outputResultId, "Identifikátor výstupu musí být vyplněn");
		
		// get output result to check permissions
		ArrOutputResult outputResult = outputResultRepository.findOneByOutputResultId(outputResultId);
		ArrOutput output = outputResult.getOutput();
		
		// check permissions
		AuthorizationRequest authRequest = AuthorizationRequest.hasPermission(UsrPermission.Permission.FUND_RD_ALL)
		    .or(UsrPermission.Permission.FUND_RD, output.getFundId());
		userService.authorizeRequest(authRequest);
		
        return outputFileRepository.findByTextAndResult(search, outputResult, from, count);
    }

    public Path getOutputFilesZip(final ArrOutputResult result) throws IOException {
        Path ret;

        try (AutoDeletingTempFile tempFile = AutoDeletingTempFile.createTempFile("ElzaOutput", ".zip");
                FileOutputStream fos = new FileOutputStream(tempFile.getPath().toFile());
                ZipOutputStream zos = new ZipOutputStream(fos);) {

            for (ArrOutputFile outputFile : result.getOutputFiles()) {
                File dmsFile = getFilePath(outputFile).toFile();
                if (dmsFile.exists()) {
                    addToZipFile(outputFile.getFileName(), dmsFile, zos);
                }
            }

            ret = tempFile.release();

        }
        return ret;
    }

    /**
     * Přidání souboru do zip souboru.
     *
     * @param fileName název souboru v zip
     * @param file     zdrojový soubor
     * @param zos      stream zip souboru
     */
    private void addToZipFile(final String fileName, final File file, final ZipOutputStream zos) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            zos.closeEntry();
        }
    }

    /**
     * Získání DO objektu souboru z pořádání podle id.
     *
     * @param fileId id souboru
     * @return DO objekt souboru
     */
    public ArrFile getArrFile(final Integer fileId) {
        return fundFileRepository.getOneCheckExist(fileId);
    }

    public void deleteFilesByFund(final ArrFund fund) {
        List<ArrFile> files = fundFileRepository.findByFund(fund);
        for (ArrFile file : files) {
            try {
                deleteFile(file);
            } catch (IOException e) {
                throw new DeleteFailedException("Nepodařilo se smazat přílohu: " + file.getName(), e);
            }
        }
    }
}
