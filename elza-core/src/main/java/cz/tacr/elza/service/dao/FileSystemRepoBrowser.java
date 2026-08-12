package cz.tacr.elza.service.dao;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.Collator;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import cz.tacr.elza.controller.vo.DigitalRepositoryTestResult;
import cz.tacr.elza.controller.vo.FsItem;
import cz.tacr.elza.controller.vo.FsItemFilterByLinked;
import cz.tacr.elza.controller.vo.FsItemSortType;
import cz.tacr.elza.controller.vo.FsItemType;
import cz.tacr.elza.controller.vo.FsItems;
import cz.tacr.elza.controller.vo.FsLink;
import cz.tacr.elza.controller.vo.FsRepo;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ArrFsLinkRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.LevelTreeCacheService;

/**
 * Directory-listing and browsing operations over a filesystem repository.
 * Primitives (path resolution, mime detection, input streams) live in
 * {@link FileSystemRepoService}; this class composes them into the UI-facing
 * browse contract.
 */
@Service
public class FileSystemRepoBrowser {

	private static final Logger log = LoggerFactory.getLogger(FileSystemRepoBrowser.class);

	private static final int DEFAULT_PAGE_SIZE = 1_000;
	private static final int MAX_PAGE_SIZE = 10_000;
	private static final int SCAN_CAP = 10_000;

	/** Maximum number of files returned by {@link #listDaoFiles} for one DAO. */
	public static final int DAO_FILE_LIMIT = 1_000;

	/** Number of root entries returned by {@link #testRepository} as a configuration sample. */
	private static final int REPO_TEST_ITEM_LIMIT = 10;

	@Autowired
	private ElzaLocale elzaLocale;	
	
	@Autowired
	private ArrFsLinkRepository fsLinkRepository;

	@Autowired
    private FileSystemRepoService fileSystemRepoService;

	@Autowired
	private ArrangementService arrangementService;

	@Autowired
	private LevelTreeCacheService levelTreeCacheService;

    public FsItems browseItems(ArrDigitalRepository digiRepo, 
    		                   ArrFund fund,
                               String path, 
                               FsItemType filterType,
                               String lastKey, 
                               FsItemFilterByLinked filterByLink, 
                               FsItemSortType sortingType,
                               String fileFilter,
                               Integer pageSize,
                               Boolean foldersFirst) throws IOException {
        Path itemPath = fileSystemRepoService.resolvePath(digiRepo, fund, path);
        if (!Files.isDirectory(itemPath)) {
            throw new BusinessException("Item is not directory.", BaseCode.INVALID_STATE)
                    .set("fsrepoId", digiRepo.getExternalSystemId())
                    .set("path", path)
                    .set("itemPath", itemPath);
        }

        Map<String, List<FsLink>> linksByCode = new HashMap<>();
        List<Object[]> rows = fsLinkRepository.findLinksByDigitalRepository(digiRepo);

        Map<Integer, List<Object[]>> rowsByFund = new HashMap<>();
        for (Object[] row : rows) {
            Integer fundId = (Integer) row[2];
            rowsByFund.computeIfAbsent(fundId, k -> new ArrayList<>()).add(row);
        }

        for (Map.Entry<Integer, List<Object[]>> entry : rowsByFund.entrySet()) {
            Integer linkFundId = entry.getKey();
            List<Object[]> fundRows = entry.getValue();
            Set<Integer> nodeIds = fundRows.stream()
                    .map(r -> (Integer) r[1])
                    .collect(Collectors.toSet());

            Map<Integer, TreeNodeVO> nodeMap = Collections.emptyMap();
            boolean readable = false;
            try {
                ArrFund linkFund = arrangementService.getFund(linkFundId);
                ArrFundVersion linkVersion = arrangementService.getOpenVersionByFund(linkFund);
                List<TreeNodeVO> treeNodes = levelTreeCacheService.getNodesByIds(nodeIds, linkVersion);
                nodeMap = treeNodes.stream().collect(Collectors.toMap(TreeNodeVO::getId, Function.identity()));
                readable = true;
            } catch (Exception e) {
            	log.warn("Failed to resolve tree nodes for fund {}: {}", linkFundId, e.toString(), e);
            }

            for (Object[] row : fundRows) {
                String code = (String) row[0];
                Integer nodeId = (Integer) row[1];
                String fundName = (String) row[3];

                FsLink link = new FsLink();
                link.setNodeId(nodeId);
                link.setFundId(linkFundId);
                link.setFundName(fundName);
                link.setReadable(readable);

                TreeNodeVO tvo = nodeMap.get(nodeId);
                if (tvo != null && tvo.getName() != null && !tvo.getName().isBlank()) {
                    link.setNodeLabel(tvo.getName());
                    if (tvo.getReferenceMark() != null && tvo.getReferenceMark().length > 0) {
                        link.setNodePath(String.join(" / ", tvo.getReferenceMark()));
                    }
                } else {
                    link.setNodeLabel("Uzel #" + nodeId);
                }

                linksByCode.computeIfAbsent(code, k -> new ArrayList<>()).add(link);
            }
        }        

        int effectivePageSize = clampPageSize(pageSize);
        boolean foldersFirstFlag = foldersFirst == null ? true : foldersFirst;
        int offset = (lastKey != null) ? Integer.parseInt(lastKey) : 0;

        List<FsItem> fsItemList = new ArrayList<>();
        boolean[] truncated = {false};
        int[] counter = {0};

        // normalize the substring filter once (Czech locale — lowercase preserves diacritics)
        String normalizedFilter = (fileFilter != null && !fileFilter.isBlank())
                ? fileFilter.toLowerCase(elzaLocale.getLocale())
                : null;

        Files.walkFileTree(itemPath, EnumSet.noneOf(FileVisitOption.class), 1, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (dir.equals(itemPath)) {
                    return FileVisitResult.CONTINUE;   // enter the root itself
                }
                return processEntry(dir, attrs);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                return processEntry(file, attrs);
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                // Broken symlink/junction on Windows, permission denied, etc.
                log.warn("Skipping unreadable filesystem entry: {}: {}", file, exc.toString());
                counter[0]++;
                return counter[0] >= SCAN_CAP ? FileVisitResult.TERMINATE : FileVisitResult.SKIP_SUBTREE;
            }

            private FileVisitResult processEntry(Path item, BasicFileAttributes attrs) {
                if (counter[0] >= SCAN_CAP) {
                    truncated[0] = true;
                    return FileVisitResult.TERMINATE;
                }
                if (!matchesTypeFilter(filterType, attrs)) {
                    counter[0]++;
                    return FileVisitResult.SKIP_SUBTREE;
                }
                String name = item.getFileName().toString();
                if (normalizedFilter != null
                        && !name.toLowerCase(elzaLocale.getLocale()).contains(normalizedFilter)) {
                    counter[0]++;
                    return FileVisitResult.SKIP_SUBTREE;
                }
                if (!attrs.isRegularFile() && !attrs.isDirectory()) {
                    // Skip unknown entry types: broken symlinks, reparse points, device/pipe files.
                    log.warn("Skipping unrecognized filesystem entry: {}", item);
                    counter[0]++;
                    return FileVisitResult.SKIP_SUBTREE;
                }
                FsItem fsItem = new FsItem();
                fsItem.setName(name);
                if (attrs.isRegularFile()) {
                    fsItem.setItemType(FsItemType.FILE);
                    fsItem.setSize(attrs.size());
                } else {
                    fsItem.setItemType(FsItemType.FOLDER);
                    fsItem.setHasChildren(directoryHasSubfolders(item));
                }
                fsItem.setLastChange(attrs.lastModifiedTime().toInstant().atOffset(ZoneOffset.UTC));
                String fullRelatPath = (path == null || path.isEmpty() || path.equals("/"))
                        ? name
                        : FileSystemRepoService.normalizeRelatPath(path) + "/" + name;
                List<FsLink> itemLinks = linksByCode.getOrDefault(fullRelatPath, Collections.emptyList());
                fsItem.setLinks(itemLinks);
                boolean isLinked = !itemLinks.isEmpty();
                if (!matchesLinkFilter(isLinked, filterByLink)) {
                    counter[0]++;
                    return FileVisitResult.SKIP_SUBTREE;
                }
                fsItemList.add(fsItem);
                counter[0]++;
                return FileVisitResult.SKIP_SUBTREE;
            }
        });

        fsItemList.sort(comparatorFor(sortingType, foldersFirstFlag));

        FsItems result = new FsItems();
        Integer nextOffset = null;
        List<FsItem> appendItems;
        if ((fsItemList.size() - offset) <= effectivePageSize) {
            appendItems = (offset == 0) ? fsItemList : fsItemList.subList(offset, fsItemList.size());
        } else {
            nextOffset = offset + effectivePageSize;
            appendItems = fsItemList.subList(offset, nextOffset);
        }
        result.getItems().addAll(appendItems);
        if (nextOffset != null) {
            result.setLastKey(nextOffset.toString());
        }
        if (truncated[0]) {
            result.setTruncated(true);
        }
        return result;
    }

    private static int clampPageSize(Integer requested) {
        if (requested == null || requested <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(requested, MAX_PAGE_SIZE);
    }    

    private Comparator<FsItem> comparatorFor(FsItemSortType sortingType, boolean foldersFirst) {
        Collator collator = elzaLocale.getCollator();
        collator.setStrength(Collator.SECONDARY);

        Comparator<FsItem> foldersFirstCmp = foldersFirst
                ? (a, b) -> {
                    if (a.getItemType() == FsItemType.FOLDER && b.getItemType() == FsItemType.FILE) return -1;
                    if (a.getItemType() == FsItemType.FILE && b.getItemType() == FsItemType.FOLDER) return 1;
                    return 0;
                }
                : (a, b) -> 0;

        FsItemSortType effective = (sortingType != null) ? sortingType : FsItemSortType.NAME_ASC;
        switch (effective) {
            case NAME_ASC:
                return foldersFirstCmp.thenComparing(FsItem::getName, collator);
            case NAME_DESC:
                return foldersFirstCmp.thenComparing((a, b) -> collator.compare(b.getName(), a.getName()));
            case SIZE_ASC:
                return foldersFirstCmp.thenComparing(a -> a.getSize() == null ? 0L : a.getSize());
            case SIZE_DESC:
                return foldersFirstCmp.thenComparing((a, b) -> Long.compare(
                        b.getSize() == null ? 0L : b.getSize(),
                        a.getSize() == null ? 0L : a.getSize()));
            default:
                return foldersFirstCmp.thenComparing(FsItem::getName, collator);
        }
    }

    private boolean matchesLinkFilter(boolean isLinked, FsItemFilterByLinked filterByLink) {
        if (filterByLink == null) {
            return true;
        }
        switch (filterByLink) {
            case LINKED:
            	return isLinked;
            case UNLINKED:
            	return !isLinked;
            case ALL:
            default:
            	return true;
        }
    }    
    
    private static boolean matchesTypeFilter(FsItemType filterType, BasicFileAttributes attrs) {
        if (filterType == null) {
            return true;
        }
        switch (filterType) {
            case FILE:   return attrs.isRegularFile();
            case FOLDER: return attrs.isDirectory();
            default:
                throw new BusinessException("Invalid filter.", BaseCode.INVALID_STATE)
                        .set("filterType", filterType);
        }
    }

    /**
     * One file of a filesystem DAO, read live from disk.
     */
    public record FsDaoFile(String relatPath, String fileName, long size, String mimetype) {
    }

    /**
     * Result of {@link #listDaoFiles(ArrDigitalRepository, ArrFund, String, int)}:
     * the collected files and a flag telling the caller whether the {@code maxEntries}
     * cap was hit before all files were listed.
     */
    public record FsDaoListing(List<FsDaoFile> files, boolean truncated) {
    }

    /**
     * Lists files of a filesystem DAO live from disk. The DAO's code is a
     * repository-relative path: a regular file yields a single entry, a
     * directory is walked recursively into a flat list ordered by relative
     * path and capped at {@code maxEntries}. Unreadable entries are skipped
     * with a logged warning; a path that no longer exists yields a synthetic
     * "missing" entry so the UI keeps the link visible.
     */
    public FsDaoListing listDaoFiles(ArrDigitalRepository digiRepo,
                                     ArrFund fund,
                                     String relatPath,
                                     int maxEntries) throws IOException {
        Path rootPath = fileSystemRepoService.getPath(digiRepo, fund).normalize();
        Path itemPath = fileSystemRepoService.resolvePath(rootPath, relatPath);
        if (Files.isRegularFile(itemPath)) {
            return new FsDaoListing(
                    Collections.singletonList(createFsDaoFile(rootPath, itemPath, Files.size(itemPath))),
                    false);
        }
        if (!Files.isDirectory(itemPath)) {
            log.warn("Filesystem DAO path is not available: {}", itemPath);
            // Synthetic entry for the missing path — item-data returns 404,
            // the UI renders the orange placeholder.
            String fileName = itemPath.getFileName().toString();
            return new FsDaoListing(
                    Collections.singletonList(new FsDaoFile(relatPath, fileName, 0L,
                            fileSystemRepoService.getMimetype(fileName))),
                    false);
        }
        List<FsDaoFile> result = new ArrayList<>();
        boolean[] truncated = {false};
        Files.walkFileTree(itemPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (result.size() >= maxEntries) {
                    truncated[0] = true;
                    return FileVisitResult.TERMINATE;
                }
                if (attrs.isRegularFile()) {
                    result.add(createFsDaoFile(rootPath, file, attrs.size()));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                log.warn("Skipping unreadable filesystem entry: {}: {}", file, exc.toString());
                return FileVisitResult.CONTINUE;
            }
        });
        result.sort(Comparator.comparing(FsDaoFile::relatPath));
        return new FsDaoListing(result, truncated[0]);
    }

    private FsDaoFile createFsDaoFile(Path rootPath, Path file, long size) {
        String relatPath = FileSystemRepoService.getRelatPath(rootPath, file);
        String fileName = file.getFileName().toString();
        return new FsDaoFile(relatPath, fileName, size, fileSystemRepoService.getMimetype(fileName));
    }

    public List<FsRepo> listRepos(ArrFund fund, List<ArrDigitalRepository> digitalRepositories) {
        List<FsRepo> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(digitalRepositories)) {
            return result;
        }
        for (ArrDigitalRepository digiRepo : digitalRepositories) {
            if (!fileSystemRepoService.isFileSystemRepository(digiRepo)) {
                continue;
            }
            Path repoPath;
            try {
                repoPath = fileSystemRepoService.getPath(digiRepo, fund);
            } catch (RuntimeException e) {
                // Unusable configuration (missing URL and similar). Report the repository as
                // unavailable instead of dropping it, otherwise the misconfiguration is invisible.
                log.warn("Filesystem repository root cannot be resolved, repository: {}: {}",
                         digiRepo.getCode(), e.toString());
                result.add(createFsRepo(digiRepo, StringUtils.defaultString(digiRepo.getUrl()), false));
                continue;
            }
            boolean available = Files.isDirectory(repoPath);
            if (!available) {
                if (FileSystemRepoService.isTemplatedUrl(digiRepo.getUrl())) {
                    // The root is fund dependent and this fund has none — not a misconfiguration,
                    // the repository simply holds nothing here.
                    continue;
                }
                log.warn("Filesystem repository root is not available, repository: {}, path: {}",
                         digiRepo.getCode(), repoPath);
            }
            result.add(createFsRepo(digiRepo, repoPath.toString(), available));
        }
        Collator collator = elzaLocale.getCollator();
        collator.setStrength(Collator.SECONDARY);
        result.sort((a, b) -> collator.compare(a.getName(), b.getName()));
        return result;
    }

    private FsRepo createFsRepo(ArrDigitalRepository digiRepo, String path, boolean available) {
        FsRepo fsRepo = new FsRepo();
        fsRepo.setFsRepoId(digiRepo.getExternalSystemId());
        fsRepo.setName(digiRepo.getName());
        fsRepo.setCode(digiRepo.getCode());
        fsRepo.setPath(path);
        fsRepo.setAvailable(available);
        return fsRepo;
    }

    /**
     * Checks the configuration of a digital repository: whether its root is a readable
     * directory and what it contains. Never throws for a broken configuration — the
     * problem is described in the returned result.
     */
    public DigitalRepositoryTestResult testRepository(ArrDigitalRepository digiRepo) {
        DigitalRepositoryTestResult result = new DigitalRepositoryTestResult();
        result.setTemplated(false);
        result.setAvailable(false);

        if (!fileSystemRepoService.isFileSystemRepository(digiRepo)) {
            result.setMessage("Repository type " + digiRepo.getDigitalRepositoryType()
                    + " has no filesystem root to test");
            return result;
        }
        String url = digiRepo.getUrl();
        if (StringUtils.isBlank(url)) {
            result.setMessage("Repository URL is not configured");
            return result;
        }

        boolean templated = FileSystemRepoService.isTemplatedUrl(url);
        result.setTemplated(templated);
        // A fund dependent root cannot be resolved outside a fund; test its fixed part instead.
        String testedUrl = templated ? FileSystemRepoService.getFixedUrlPrefix(url) : url;

        Path rootPath;
        try {
            rootPath = Paths.get(testedUrl).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            result.setPath(testedUrl);
            result.setMessage("Path is not valid: " + e.getMessage());
            return result;
        }
        result.setPath(rootPath.toString());

        if (!Files.exists(rootPath)) {
            result.setMessage("Path does not exist");
            return result;
        }
        if (!Files.isDirectory(rootPath)) {
            result.setMessage("Path is not a directory");
            return result;
        }
        if (!Files.isReadable(rootPath)) {
            result.setMessage("Directory is not readable");
            return result;
        }

        try {
            result.setItems(listFirstItems(rootPath, REPO_TEST_ITEM_LIMIT));
        } catch (IOException e) {
            result.setMessage("Directory cannot be listed: " + e.toString());
            return result;
        }
        result.setAvailable(true);
        if (templated) {
            result.setMessage("Repository root depends on the fund; only the fixed part of the path was tested");
        }
        return result;
    }

    /**
     * First {@code maxItems} entries of a directory, in the order the filesystem returns
     * them. Entries that cannot be read are skipped.
     */
    private List<FsItem> listFirstItems(Path dirPath, int maxItems) throws IOException {
        List<FsItem> items = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dirPath)) {
            for (Path item : ds) {
                if (items.size() >= maxItems) {
                    break;
                }
                BasicFileAttributes attrs;
                try {
                    attrs = Files.readAttributes(item, BasicFileAttributes.class);
                } catch (IOException e) {
                    log.warn("Skipping unreadable filesystem entry: {}: {}", item, e.toString());
                    continue;
                }
                if (!attrs.isRegularFile() && !attrs.isDirectory()) {
                    continue;
                }
                FsItem fsItem = new FsItem();
                fsItem.setName(item.getFileName().toString());
                if (attrs.isRegularFile()) {
                    fsItem.setItemType(FsItemType.FILE);
                    fsItem.setSize(attrs.size());
                } else {
                    fsItem.setItemType(FsItemType.FOLDER);
                }
                fsItem.setLastChange(attrs.lastModifiedTime().toInstant().atOffset(ZoneOffset.UTC));
                items.add(fsItem);
            }
        }
        return items;
    }

    private boolean directoryHasSubfolders(Path dir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, Files::isDirectory)) {
            return stream.iterator().hasNext();
        } catch (IOException e) {
            log.warn("Failed to probe subfolders in {}: {}", dir, e.toString());
            return false;
        }
    }
}