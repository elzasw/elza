package cz.tacr.elza.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.domain.BatchState;
import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.domain.ImpBatch;
import cz.tacr.elza.domain.ImpItem;
import cz.tacr.elza.domain.ImpItemResult;
import cz.tacr.elza.domain.ItemState;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ImpItemRepository;
import cz.tacr.elza.repository.ImpItemResultRepository;

/**
 * Manages the items inside an import batch: which files it holds, in what order, and their
 * status. The rules about when items may be added or removed live here.
 */
@Service
public class ImpItemService {

    private static final Logger logger = LoggerFactory.getLogger(ImpItemService.class);

    @Autowired
    private ImpItemRepository itemRepository;
    @Autowired
    private ImpItemResultRepository resultRepository;
    @Autowired
    private ImpBatchService batchService;
    @Autowired
    private UserService userService;
    @Autowired
    private DmsService dmsService;

    /**
     * Base directory below which "folder on the server" imports are allowed. Empty means the
     * feature is disabled: an administrator must configure a path before it is usable.
     */
    @Value("${elza.import.batchInputDir:}")
    private String batchInputDir;

    /**
     * Whether the configured folder has already been reported as missing. The listing is called
     * from the UI to decide whether to offer the folder button, so without this a mis-configured
     * deployment writes one line per page visit for a condition that does not change between two
     * clicks. Cleared once the folder is found, so a deployment that breaks again is reported again.
     */
    private final AtomicBoolean missingFolderReported = new AtomicBoolean();

    @Transactional
    public ImpItem findById(int itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ObjectNotFoundException("Import item not found: " + itemId, BaseCode.ID_NOT_EXIST).setId(itemId));
    }

    @Transactional
    public List<ImpItem> listByBatch(ImpBatch batch) {
        return itemRepository.findByBatchOrderByExecOrderAsc(batch);
    }

    @Transactional
    public List<ImpItemResult> listResults(ImpItem item) {
        return resultRepository.findByItemOrderByItemResultIdAsc(item);
    }

    /**
     * Opens a stream to the source file of the item. Null if the file is no longer in DMS
     * (retention window closed, or the source was never stored).
     */
    @Transactional
    public InputStream openSource(ImpItem item) {
        if (item.getDmsFile() == null) {
            return null;
        }
        return dmsService.newInputStream(item.getDmsFile());
    }

    /**
     * Adds a new item to the batch. If the batch is in TEST_FINISHED it is walked back to
     * PREPARATION first - a test run's verdict does not extend to items added after it.
     *
     * @param dmsFile stored source file for this item, or null if not stored yet (folder-on-server
     *                path in a later substep); the item name defaults to the file's name.
     */
    @Transactional
    public ImpItem addItem(ImpBatch batch, DmsFile dmsFile, String name) {
        if (batch.getState() == BatchState.TEST_FINISHED) {
            batchService.changeState(batch, BatchState.PREPARATION);
        }
        if (batch.getState() != BatchState.PREPARATION) {
            throw new BusinessException(
                    "Items may only be added to a batch in PREPARATION; current state: " + batch.getState(),
                    BaseCode.INVALID_STATE);
        }

        OffsetDateTime now = OffsetDateTime.now();
        UsrUser me = userService.getLoggedUser();
        ImpItem item = new ImpItem();
        item.setBatch(batch);
        item.setDmsFile(dmsFile);
        item.setItemName(name);
        item.setState(ItemState.READY);
        item.setExecOrder(itemRepository.findMaxExecOrder(batch) + 1);
        item.setCreatedAt(now);
        item.setCreatedByUser(me);
        return itemRepository.save(item);
    }

    /**
     * Uploads a single source file into the batch: stores it in DMS and adds an item pointing at
     * it. Common entry point for the "upload one file" and "select several files on disk" UI
     * gestures - the caller invokes it once per file.
     *
     * @param mimeType content type as reported by the upload; when null, a generic binary type is
     *                 recorded (the item may still be a valid XML or CSV file - the importer will
     *                 look at the content, not the DMS metadata).
     */
    @Transactional
    public ImpItem uploadItem(ImpBatch batch, String fileName, String mimeType, int size, InputStream stream) throws IOException {
        DmsFile dmsFile = new DmsFile();
        dmsFile.setName(fileName);
        dmsFile.setFileName(fileName);
        dmsFile.setFileSize(size);
        dmsFile.setMimeType(mimeType != null ? mimeType : "application/octet-stream");
        DmsFile stored = dmsService.createFile(dmsFile, stream);
        return addItem(batch, stored, fileName);
    }

    /**
     * Unpacks a ZIP container into the batch, one item per contained file. The stream is spooled
     * to a temporary file first so ZipFile can use the central directory (accurate sizes, random
     * access, no ambiguity from streaming local headers). Directories are skipped.
     */
    @Transactional
    public List<ImpItem> uploadZip(ImpBatch batch, InputStream zipStream) throws IOException {
        Path tmp = Files.createTempFile("imp-zip-", ".zip");
        try {
            Files.copy(zipStream, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            List<ImpItem> added = new ArrayList<>();
            try (ZipFile zipFile = new ZipFile(tmp.toFile())) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory()) {
                        continue;
                    }
                    long size = entry.getSize();
                    if (size < 0 || size > Integer.MAX_VALUE) {
                        throw new BusinessException(
                                "ZIP entry has unusable size: " + entry.getName() + " (" + size + ")",
                                BaseCode.INVALID_STATE);
                    }
                    String name = stripPath(entry.getName());
                    try (InputStream in = zipFile.getInputStream(entry)) {
                        added.add(uploadItem(batch, name, null, (int) size, in));
                    }
                }
            }
            return added;
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static String stripPath(String zipEntryName) {
        int slash = Math.max(zipEntryName.lastIndexOf('/'), zipEntryName.lastIndexOf('\\'));
        return slash >= 0 ? zipEntryName.substring(slash + 1) : zipEntryName;
    }

    /**
     * A single entry (file or subdirectory) inside the server-side import folder. Used by
     * {@link #listServerFolder(String)} to feed a folder-picker UI.
     */
    public record ServerFolderEntry(String name, boolean directory, Long size) {}

    /**
     * Lists direct children of a folder inside {@code elza.import.batchInputDir}. Directories are
     * returned first, then files, both alphabetically. The path is validated the same way as in
     * {@link #importFromServerFolder}. When {@code batchInputDir} is not configured returns an
     * empty list - the folder-import feature is simply unavailable and callers fall back to the
     * upload paths without a noisy error.
     */
    @Transactional(readOnly = true)
    public List<ServerFolderEntry> listServerFolder(String relativePath) throws IOException {
        if (batchInputDir == null || batchInputDir.isBlank()) {
            return List.of();
        }
        Path base = Paths.get(batchInputDir).toAbsolutePath().normalize();
        if (!Files.isDirectory(base)) {
            // The configured path is missing on disk, so the feature is off just as it is when
            // nothing is configured - the caller falls back the same way and the user is told by
            // the disabled button. Reported once per change of state rather than per call: this is
            // a deployment fault, and one line per page visit is how a log stops being read.
            if (missingFolderReported.compareAndSet(false, true)) {
                logger.warn("Configured import folder does not exist: {} (elza.import.batchInputDir)", base);
            }
            return List.of();
        }
        missingFolderReported.set(false);
        Path resolved = resolveFolder(relativePath);
        List<ServerFolderEntry> out = new ArrayList<>();
        try (Stream<Path> entries = Files.list(resolved)) {
            entries.sorted((a, b) -> {
                boolean da = Files.isDirectory(a);
                boolean db = Files.isDirectory(b);
                if (da != db) return da ? -1 : 1;
                return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
            }).forEach(p -> {
                boolean isDir = Files.isDirectory(p);
                Long size = null;
                if (!isDir) {
                    try {
                        size = Files.size(p);
                    } catch (IOException ignored) {
                        // size stays null – shown as "?" in the UI
                    }
                }
                out.add(new ServerFolderEntry(p.getFileName().toString(), isDir, size));
            });
        }
        return out;
    }

    /**
     * Adds regular files from the given server-side folder to the batch. The path is interpreted
     * relative to {@code elza.import.batchInputDir}; a path that would escape that root is
     * rejected. Directories inside the folder are not descended - the specification describes one
     * flat drop location, not a tree walk. If {@code filenames} is null or empty, every regular
     * file in the folder is added; otherwise only files whose name is listed are added.
     */
    @Transactional
    public List<ImpItem> importFromServerFolder(ImpBatch batch, String relativePath, List<String> filenames)
            throws IOException {
        Path resolved = resolveFolder(relativePath);
        java.util.Set<String> filter = (filenames == null || filenames.isEmpty())
                ? null
                : new java.util.HashSet<>(filenames);

        List<ImpItem> added = new ArrayList<>();
        try (Stream<Path> entries = Files.list(resolved)) {
            List<Path> files = entries
                    .filter(Files::isRegularFile)
                    .filter(p -> filter == null || filter.contains(p.getFileName().toString()))
                    .sorted()
                    .toList();
            for (Path file : files) {
                long size = Files.size(file);
                if (size > Integer.MAX_VALUE) {
                    throw new BusinessException(
                            "Server file too large for DMS: " + file.getFileName() + " (" + size + ")",
                            BaseCode.INVALID_STATE);
                }
                try (InputStream in = Files.newInputStream(file)) {
                    added.add(uploadItem(batch, file.getFileName().toString(), null, (int) size, in));
                }
            }
        }
        return added;
    }

    private Path resolveFolder(String relativePath) {
        if (batchInputDir == null || batchInputDir.isBlank()) {
            throw new BusinessException(
                    "Server-side folder import is not configured (elza.import.batchInputDir)",
                    BaseCode.INVALID_STATE);
        }
        Path base = Paths.get(batchInputDir).toAbsolutePath().normalize();
        Path resolved = base.resolve(relativePath == null ? "" : relativePath).normalize();
        if (!resolved.startsWith(base)) {
            throw new BusinessException(
                    "Folder path escapes the configured base directory: " + relativePath,
                    BaseCode.INVALID_STATE);
        }
        if (!Files.isDirectory(resolved)) {
            throw new BusinessException(
                    "Server folder does not exist: " + relativePath,
                    BaseCode.INVALID_STATE);
        }
        return resolved;
    }

    /**
     * Removes an item from its batch. Allowed while the batch is being prepared (any non-finished
     * item) or, when the batch is in FAILED / PAUSED, for items that ended in ERROR - so the
     * operator can clean up the broken piece after a run.
     */
    @Transactional
    public void deleteItem(ImpItem item) {
        ImpBatch batch = item.getBatch();
        BatchState bs = batch.getState();
        boolean allowed = bs == BatchState.PREPARATION
                || ((bs == BatchState.FAILED || bs == BatchState.PAUSED) && item.getState() == ItemState.ERROR);
        if (!allowed) {
            throw new BusinessException(
                    "Item cannot be removed in batch state " + bs + " / item state " + item.getState(),
                    BaseCode.INVALID_STATE);
        }
        if (item.getState() == ItemState.FINISHED) {
            throw new BusinessException(
                    "An imported item cannot be removed",
                    BaseCode.INVALID_STATE);
        }
        logger.debug("Removing import item {} from batch {}", item.getItemId(), batch.getBatchId());
        DmsFile file = item.getDmsFile();
        if (file != null) {
            item.setDmsFile(null);
            itemRepository.save(item);
            try {
                dmsService.deleteFile(file);
            } catch (RuntimeException ex) {
                logger.warn("Failed to delete DMS file {} of item {}: {}",
                        file.getFileId(), item.getItemId(), ex.getMessage());
            }
        }
        itemRepository.delete(item);
    }
}
