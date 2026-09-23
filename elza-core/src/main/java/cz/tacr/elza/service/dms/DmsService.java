package cz.tacr.elza.service.dms;

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
import java.nio.file.StandardCopyOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import cz.tacr.elza.common.AutoDeletingTempFile;
import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrChange.Type;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputFile;
import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.repository.FileRepository;
import cz.tacr.elza.repository.FilteredResult;
import cz.tacr.elza.repository.FundFileRepository;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.OutputFileRepository;
import cz.tacr.elza.service.ArrangementInternalService;
import cz.tacr.elza.service.eventnotification.EventFactory;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventStringInVersion;
import cz.tacr.elza.service.eventnotification.events.EventType;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;

/**
 * Dms Service
 *
 * @since 20.6.2016
 */
@Service
public class DmsService {

    private static final Logger logger = LoggerFactory.getLogger(DmsService.class);

    public static final String MIME_TYPE_APPLICATION_PDF = "application/pdf";

    private static final String SHA_256 = "SHA-256";

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
    ArrangementInternalService arrangementInternalService;

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private DmsTrashService dmsTrashService;

    /**
     * On startup make sure {@code dms/} exists and {@code README.txt} matches
     * the classpath resource. If the on-disk copy differs (older version, edited
     * by an operator, missing), it is overwritten from the classpath — treated
     * as an application upgrade of the storage marker.
     */
    @PostConstruct
    public void initDmsStorage() {
        try {
            Path dmsRoot = resourcePathResolver.getDmsDir();
            Files.createDirectories(dmsRoot);
            refreshReadme(dmsRoot);
        } catch (IOException e) {
            logger.warn("Failed to initialize DMS storage: {}", e.toString());
        }
    }

    private void refreshReadme(Path dmsRoot) throws IOException {
        Path target = dmsRoot.resolve(DmsStorageLayout.README_FILE);
        byte[] resource;
        try (InputStream in = DmsService.class.getResourceAsStream(DmsStorageLayout.README_CLASSPATH)) {
            if (in == null) {
                logger.warn("Classpath resource {} not found; skipping README refresh.",
                        DmsStorageLayout.README_CLASSPATH);
                return;
            }
            resource = in.readAllBytes();
        }
        if (Files.exists(target) && Arrays.equals(Files.readAllBytes(target), resource)) {
            return;
        }
        Files.write(target, resource);
        logger.info("DMS README.txt refreshed from classpath resource.");
    }

    /**
     * Uloží DMS soubor se streamem a publishne event
     */
    public DmsFile createFile(final DmsFile dmsFile, final InputStream fileStream) throws IOException {
        Validate.notNull(dmsFile, "Soubor musí být vyplněn");
        Validate.notNull(fileStream, "Stream souboru musí být vyplněn");

        writeFile(dmsFile, out -> {
            try {
                IOUtils.copy(fileStream, out);
            } catch (IOException e) {
                throw new SystemException("Failed to copy to the output", e);
            } finally {
                IOUtils.closeQuietly(fileStream);
            }
        });
        return dmsFile;
    }

    /**
     * Uloží DMS soubor se streamem a publishne event
     */
    public void createFile(final DmsFile dmsFile, final Consumer<OutputStream> dataProvider) throws IOException {
        Validate.notNull(dmsFile, "Soubor musí být vyplněn");
        Validate.notNull(dataProvider, "DataProvider souboru musí být vyplněn");

        writeFile(dmsFile, dataProvider);
    }

    /**
     * Publish eventu.
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

    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR, UsrPermission.Permission.FUND_ARR_ALL})
    public void checkFundWritePermission(@AuthParam(type = AuthParam.Type.FUND) final Integer fundId) {
        Assert.notNull(fundId, "Nebyl vyplněn identifikátor AS");
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_RD, UsrPermission.Permission.FUND_RD_ALL})
    public void checkFundReadPermission(@AuthParam(type = AuthParam.Type.FUND) final Integer fundId) {
        Assert.notNull(fundId, "Nebyl vyplněn identifikátor AS");
    }

    /**
     * Aktualizace metadat DMS souboru (name / fileName / mimeType /
     * fileSize / display name). Obsah souboru se touto cestou nemění —
     * pro nahrazení obsahu přílohy použijte {@link #replaceArrFileContent},
     * který vytvoří nový řádek dms_file a starý zachová pro vratnost.
     */
    public void updateFile(final DmsFile newFile) {
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
        publishFileChange(dbFile);
    }

    /**
     * Vrátí stream pro stažení souboru. Caller je zodpovědný za uzavření.
     */
    public InputStream newInputStream(final DmsFile dmsFile) {
        Validate.notNull(dmsFile, "Soubor musí být vyplněn");
        Path dmsFilePath = getFilePath(dmsFile);
        if (!Files.exists(dmsFilePath)) {
            logger.error("File not exist, fileId: {}, filePath: {}", dmsFile.getFileId(), dmsFilePath);
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
     * Static přístup pro non-Spring volací kód (např. DmsFileLoader).
     * Autoritativně čte {@code storagePath}, s pádem zpět na plochý název.
     */
    public static InputStream newInputStream(final ResourcePathResolver resourcePathResolver, final DmsFile dmsFile) {
        Validate.notNull(dmsFile, "Soubor musí být vyplněn");
        String rel = dmsFile.getStoragePath();
        Path dmsFilePath = (rel != null)
                ? resourcePathResolver.getDmsDir().resolve(rel)
                : resourcePathResolver.getDmsDir().resolve(String.valueOf(dmsFile.getFileId()));
        if (!Files.exists(dmsFilePath)) {
            logger.error("File not exist, fileId: {}, filePath: {}", dmsFile.getFileId(), dmsFilePath);
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
     * Smazání/nastavení pole deleteChange u objektu ArrFile
     */
    @AuthMethod(permission = { UsrPermission.Permission.FUND_ARR, UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ADMIN })
    public void deleteArrFile(final ArrFile file, @AuthParam(type = AuthParam.Type.FUND) final ArrFund fund) throws IOException {
        Integer count = arrangementInternalService.countActiveItems(file);
        if (count > 0) {
            throw new BusinessException("Existují návazné jednotky popisu, přílohu nelze smazat",
                    ArrangementCode.ATTACHMENT_DELETE_ERROR)
                            .level(Level.WARNING)
                            .set("count", count)
                            .set("id", file.getFileId());
        }

        ArrChange deleteChange = arrangementInternalService.createChange(Type.DELETE_ATTACHMENT);
        file.setDeleteChange(deleteChange);
        fileRepository.save(file);

        publishFileChange(file);
    }

    /**
     * Nahrazení obsahu ArrFile: vytvoří nový řádek dms_file s novým
     * storage_path, starý řádek arr_file dostane delete_change_id (nový
     * arr_change typu UPDATE_ATTACHMENT). Fyzický soubor starého řádku
     * dms_file zůstává na disku, aby operace byla vratná přes
     * RevertingChangesService.
     *
     * @param oldFile původní ArrFile (bude soft-deleted)
     * @param newFile nový ArrFile bez file_id, s vyplněnou metadatou;
     *                fund a createChange se nastaví uvnitř metody
     * @param fileStream stream s novým obsahem; metoda ho uzavře
     * @return newFile s vyplněným file_id a storage_path
     */
    public ArrFile replaceArrFileContent(final ArrFile oldFile, final ArrFile newFile,
                                         final InputStream fileStream) throws IOException {
        Validate.notNull(oldFile, "Původní soubor musí být vyplněn");
        Validate.notNull(newFile, "Nový soubor musí být vyplněn");
        Validate.notNull(fileStream, "Stream souboru musí být vyplněn");

        ArrChange updateChange = arrangementInternalService.createChange(Type.UPDATE_ATTACHMENT);
        oldFile.setDeleteChange(updateChange);
        fileRepository.save(oldFile);

        newFile.setCreateChange(updateChange);
        if (newFile.getFund() == null) {
            newFile.setFund(oldFile.getFund());
        }
        if (newFile.getName() == null || newFile.getName().isEmpty()) {
            newFile.setName(oldFile.getName());
        }
        if (newFile.getFileName() == null) {
            newFile.setFileName(oldFile.getFileName());
        }
        if (newFile.getMimeType() == null) {
            newFile.setMimeType(oldFile.getMimeType());
        }
        createFile(newFile, fileStream);
        return newFile;
    }

    /**
     * Smazání DMS souboru včetně jeho fyzického obsahu (přesun do koše po commitu).
     */
    public void deleteFile(final DmsFile dmsFile) {
        Assert.notNull(dmsFile, "Soubor musí být vyplněn");

        Path path = getFilePath(dmsFile);
        fileRepository.delete(dmsFile);
        registerAfterCommitTrashMove(path, "delete-" + reasonSuffix(dmsFile));
        publishFileChange(dmsFile);
    }

    /**
     * Po commitu přesune všechny předané soubory do koše s daným důvodem.
     * Cesty se vyhodnotí ihned, aby přežily následné smazání řádku.
     */
    public void deleteFilesAfterCommit(final Collection<? extends DmsFile> files, final String reason) {
        if (files == null || files.isEmpty()) {
            return;
        }
        final List<Path> paths = files.stream().map(this::getFilePath).collect(Collectors.toList());
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // No ambient tx: the delete already committed in its own isolated tx.
            for (Path p : paths) {
                trashQuietly(p, reason);
            }
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (Path p : paths) {
                    trashQuietly(p, reason);
                }
            }
        });
    }

    /**
     * Získání cesty k reálnému souboru. Autoritativně čte {@code storagePath};
     * pro legacy řádky bez cesty spadá zpět na plochý název {@code dms/<file_id>}.
     */
    public Path getFilePath(final DmsFile file) {
        String rel = file.getStoragePath();
        if (rel != null) {
            return resourcePathResolver.getDmsDir().resolve(rel);
        }
        return resourcePathResolver.getDmsDir().resolve(String.valueOf(file.getFileId()));
    }

    public DmsFile getFile(final Integer fileId) {
        Assert.notNull(fileId, "Identifikátor souboru musí být vyplněn");
        return fileRepository.getOneCheckExist(fileId);
    }

    public FilteredResult<DmsFile> findDmsFiles(final String search, final Integer from, final Integer count) {
        return fileRepository.findByText(search, from, count);
    }

    @AuthMethod(permission = {UsrPermission.Permission.FUND_RD, UsrPermission.Permission.FUND_RD_ALL})
    public FilteredResult<ArrFile> findArrFiles(final String search,
                                                @AuthParam(type = AuthParam.Type.FUND) final Integer fundId,
                                                final Integer from,
                                                final Integer count) {
        Assert.notNull(fundId, "Nebyl vyplněn identifikátor AS");
        return fundFileRepository.findByTextAndFund(search, fundRepository.getOneCheckExist(fundId), from, count);
    }

    @Transactional(value = Transactional.TxType.MANDATORY)
    @AuthMethod(permission = {UsrPermission.Permission.FUND_RD, UsrPermission.Permission.FUND_RD_ALL})
    public List<ArrOutputFile> findOutputFiles(@AuthParam(type = AuthParam.Type.FUND) Integer fundId,
            ArrOutput output) {
        return outputFileRepository.findByOutputResultOutput(output);
    }

    public Path getOutputFilesZip(final List<ArrOutputFile> files) throws IOException {
        Path ret;
        try (AutoDeletingTempFile tempFile = AutoDeletingTempFile.createTempFile("ElzaOutput", ".zip");
                FileOutputStream fos = new FileOutputStream(tempFile.getPath().toFile());
                ZipOutputStream zos = new ZipOutputStream(fos);) {
            for (ArrOutputFile outputFile : files) {
                File dmsFile = getFilePath(outputFile).toFile();
                if (dmsFile.exists()) {
                    addToZipFile(outputFile.getFileName(), dmsFile, zos);
                }
            }
            ret = tempFile.release();
        }
        return ret;
    }

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

    public ArrFile getArrFile(final Integer fileId) {
        return fundFileRepository.getOneCheckExist(fileId);
    }

    public ArrOutputFile getOutputFile(Integer fileId) {
        return outputFileRepository.getOneCheckExist(fileId);
    }

    public void deleteFilesByFund(final ArrFund fund) {
        List<ArrFile> files = fundFileRepository.findByFund(fund);
        for (ArrFile file : files) {
            deleteFile(file);
        }
    }

    // ---------- private helpers ----------

    /**
     * Full create pipeline: allocate id, compute storage_path, write via
     * tmp+digest+fsync, atomic move, register rollback trash.
     */
    private void writeFile(DmsFile dmsFile, Consumer<OutputStream> dataProvider) throws IOException {
        dmsFile.setCreatedAt(OffsetDateTime.now());
        if (dmsFile.getFileSize() == null) {
            // NOT NULL placeholder; the real size is written by writeToTarget after streaming.
            dmsFile.setFileSize(0);
        }
        fileRepository.save(dmsFile);
        dmsFile.setStoragePath(DmsStorageLayout.relativePath(
                dmsFile.getCreatedAt(), dmsFile.getFileId(), dmsFile.getFileName()));

        Path target = getFilePath(dmsFile);
        writeToTarget(dmsFile, target, dataProvider);

        registerAfterCompletionTrashMove(target, "rollback-create");
        fileRepository.save(dmsFile);
        publishFileChange(dmsFile);
    }

    /**
     * Stream to {@code <target>.tmp}, digest SHA-256, fsync, atomic-move to
     * {@code target}. Populates size / checksum / pagesCount on {@code dmsFile}.
     * On any failure, trashes the tmp file and rethrows.
     */
    private void writeToTarget(DmsFile dmsFile, Path target, Consumer<OutputStream> dataProvider) throws IOException {
        Path tmp = DmsStorageLayout.tmpPath(target);
        Files.createDirectories(target.getParent());
        MessageDigest digest = sha256();
        try {
            try (FileOutputStream fos = new FileOutputStream(tmp.toFile());
                 DigestOutputStream dos = new DigestOutputStream(new BufferedOutputStream(fos), digest)) {
                dataProvider.accept(dos);
                dos.flush();
                fos.getFD().sync();
            }
            dmsFile.setFileSize(Math.toIntExact(Files.size(tmp)));
            dmsFile.setChecksum(Hex.encodeHexString(digest.digest()));

            if (MIME_TYPE_APPLICATION_PDF.equalsIgnoreCase(dmsFile.getMimeType())) {
                try (PDDocument document = Loader.loadPDF(new RandomAccessReadBufferedFile(tmp.toFile()))) {
                    dmsFile.setPagesCount(document.getNumberOfPages());
                }
            } else {
                dmsFile.setPagesCount(null);
            }

            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | RuntimeException e) {
            trashQuietly(tmp, "write-failed");
            throw e;
        }
    }

    private void trashQuietly(Path path, String reason) {
        try {
            dmsTrashService.moveToTrash(path, reason);
        } catch (Exception e) {
            logger.warn("Failed to trash DMS file {}: {}", path, e.toString());
        }
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance(SHA_256);
        } catch (NoSuchAlgorithmException e) {
            throw new SystemException("SHA-256 not available in this JVM", e);
        }
    }

    private void registerAfterCommitTrashMove(Path path, String reason) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // No ambient tx: the delete already committed in its own isolated tx.
            trashQuietly(path, reason);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                trashQuietly(path, reason);
            }
        });
    }

    private void registerAfterCompletionTrashMove(Path path, String reason) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // No ambient tx: no rollback is possible, nothing to compensate.
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    trashQuietly(path, reason);
                }
            }
        });
    }

    private static String reasonSuffix(DmsFile dmsFile) {
        if (dmsFile instanceof ArrFile) {
            return "attachment";
        }
        if (dmsFile instanceof ArrOutputFile) {
            return "output";
        }
        return "plain";
    }
}
