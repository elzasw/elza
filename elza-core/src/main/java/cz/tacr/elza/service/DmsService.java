package cz.tacr.elza.service;

import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventStringInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Dms Service
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.2016
 */
@Service
public class DmsService {

    public static final String MIME_TYPE_APPLICATION_PDF = "application/pdf";
    public static final String MIME_TYPE_TEXT_CVS = "text/csv";

    /**
     * Složka se soubory DMS
     */
    @Value("${elza.dmsDir}")
    private String dmsFileDirectory;

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

    /**
     * Uloží DMS soubor se streamem a publishne event
     *
     * @param dmsFile    DO
     * @param fileStream stream pro uložení souboru
     * @throws IOException
     */
    public void createFile(final DmsFile dmsFile, final InputStream fileStream) throws IOException {
        Assert.notNull(dmsFile);
        Assert.notNull(fileStream);

        fileRepository.save(dmsFile);

        File outputFile = new File(getFilePath(dmsFile));
        if (outputFile.exists()) {
            throw new IOException("Nelze soubor již existuje");
        }
        saveFile(dmsFile, fileStream, outputFile);

        fileRepository.save(dmsFile);
        publishFileChange(dmsFile);
    }

    /**
     * Publish eventu
     *
     * @param dmsFile dms sobor
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
     * Úprava detailů souboru či jeho nahrazení
     *
     * @param newFile    nové DO DMS file
     * @param fileStream Stream
     * @throws IOException
     */
    public void updateFile(final DmsFile newFile, final InputStream fileStream) throws IOException {
        Assert.notNull(newFile);
        Assert.notNull(newFile.getFileId());

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
            File outputFile = new File(getFilePath(dbFile));
            if (outputFile.exists() && !outputFile.delete()) {
                throw new IOException("Nelze odstranit existující soubor");
            }
            saveFile(dbFile, fileStream, outputFile);

            fileRepository.save(dbFile);
        }
        publishFileChange(dbFile);
    }

    /**
     * Vrátí stream pro stažení souboru
     *
     * @param dmsFile dms Soubor ke stažení
     * @return stream
     */
    public InputStream downloadFile(final DmsFile dmsFile) {
        Assert.notNull(dmsFile);

        File outputFile = new File(getFilePath(dmsFile));
        if (!outputFile.exists()) {
            throw new IllegalStateException("Požadovaný soubor neexistuje");
        }
        if (!outputFile.isFile()) {
            throw new IllegalStateException("Požadovaný soubor není souborem ale složkou");
        }

        try {
            return new BufferedInputStream(new FileInputStream(outputFile));
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Požadovaný soubor nebyl nalezen");
        }
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
     * @param arrFileId ID
     * @throws IOException
     */
    public void deleteArrFile(final Integer arrFileId) throws IOException {
        deleteFile(fundFileRepository.getOneCheckExist(arrFileId));
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
        Assert.notNull(dmsFile);

        fileRepository.delete(dmsFile);

        File outputFile = new File(getFilePath(dmsFile));
        if (outputFile.exists() && !outputFile.delete()) {
            throw new IOException("Nelze odstranit existující soubor");
        }
        publishFileChange(dmsFile);
    }

    /**
     * Uložení do souboru
     *
     * @param dmsFile    DO
     * @param fileStream stream souboru
     * @param outputFile místo k uložení souboru
     * @throws IOException
     */
    private void saveFile(final DmsFile dmsFile, final InputStream fileStream, final File outputFile) throws IOException {

        FileUtils.touch(outputFile);
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile, false));
        IOUtils.copy(fileStream, outputStream);
        IOUtils.closeQuietly(fileStream);
        IOUtils.closeQuietly(outputStream);

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
    public String getFilePath(DmsFile file) {
        return dmsFileDirectory + File.separator + file.getFileId();
    }

    /**
     * Vrátí soubor dle id
     *
     * @param fileId id souboru
     * @return soubor
     */
    public DmsFile getFile(Integer fileId) {
        Assert.notNull(fileId);
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
    public FilteredResult<DmsFile> findDmsFiles(String search, Integer from, Integer count) {
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
    public FilteredResult<ArrFile> findArrFiles(String search, Integer fundId, Integer from, Integer count) {
        Assert.notNull(fundId);
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
    public FilteredResult<ArrOutputFile> findOutputFiles(String search, Integer outputResultId, Integer from, Integer count) {
        Assert.notNull(outputResultId);
        return outputFileRepository.findByTextAndResult(search, outputResultRepository.getOneCheckExist(outputResultId), from, count);
    }

    public File getOutputFilesZip(ArrOutputResult result) {

        File file = null;
        FileOutputStream fos = null;
        ZipOutputStream zos = null;

        try {
            file = File.createTempFile(result.getOutputDefinition().getName(), ".zip");
            fos = new FileOutputStream(file);
            zos = new ZipOutputStream(fos);

            for (ArrOutputFile outputFile : result.getOutputFiles()) {
                File dmsFile = new File(getFilePath(outputFile));
                if (dmsFile.exists()) {
                    addToZipFile(outputFile.getFileName(), dmsFile, zos);
                }
            }

            file.deleteOnExit();
            return file;
        } catch (IOException e) {

            if (file != null) {
                file.delete();
            }

            throw new IllegalStateException(e);

        } finally {

            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    /**
     * Přidání souboru do zip souboru.
     *
     * @param fileName název souboru v zip
     * @param file     zdrojový soubor
     * @param zos      stream zip souboru
     */
    private void addToZipFile(String fileName, File file, ZipOutputStream zos) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zos.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }
        zos.closeEntry();
        fis.close();
    }
}