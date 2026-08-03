package cz.tacr.elza.service.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.Collator;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import cz.tacr.elza.controller.vo.FsItem;
import cz.tacr.elza.controller.vo.FsItemFilterByLinked;
import cz.tacr.elza.controller.vo.FsItemSortType;
import cz.tacr.elza.controller.vo.FsItemType;
import cz.tacr.elza.controller.vo.FsItems;
import cz.tacr.elza.controller.vo.FsRepo;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DaoLinkRepository;

/**
 * Directory-listing and browsing operations over a filesystem repository.
 * Primitives (path resolution, mime detection, input streams) live in
 * {@link FileSystemRepoService}; this class composes them into the UI-facing
 * browse contract.
 */
@Service
public class FileSystemRepoBrowser {

	@Autowired
	private DaoLinkRepository daoLinkRepository;

	@Autowired
    private FileSystemRepoService fileSystemRepoService;

    public FsItems browseItems(ArrDigitalRepository digiRepo, 
    		                   ArrFund fund,
                               String path, 
                               FsItemType filterType,
                               String lastKey, 
                               FsItemFilterByLinked filterByLink, 
                               FsItemSortType sortingType,
                               String fileFilter) throws IOException {
        Path itemPath = fileSystemRepoService.resolvePath(digiRepo, fund, path);
        if (!Files.isDirectory(itemPath)) {
            throw new BusinessException("Item is not directory.", BaseCode.INVALID_STATE)
                    .set("fsrepoId", digiRepo.getExternalSystemId())
                    .set("path", path)
                    .set("itemPath", itemPath);
        }

        Set<String> linkedPaths = new HashSet<>(daoLinkRepository.findLinkedCodesByDigitalRepository(digiRepo));

        int maxItems = 1000;
        if (digiRepo.getCode() != null && digiRepo.getCode().endsWith("_DEBUG")) {
            // TODO: replace with pageSize request parameter (Phase 2)
            maxItems = 2;
        }
        int offset = (lastKey != null) ? Integer.parseInt(lastKey) : 0;

        Function<Path, Boolean> acceptor = prepareFSFilter(filterType);

        List<FsItem> fsItemList = new ArrayList<>();

        // normalize the substring filter once (Czech locale — lowercase preserves diacritics)
        String normalizedFilter = (fileFilter != null && !fileFilter.isBlank())
                ? fileFilter.toLowerCase(new Locale("cs"))
                : null;

        try (Stream<Path> ds = Files.list(itemPath)) {
            Iterator<Path> it = ds.iterator();
            int counter = 0;
            // TODO: expose truncation flag in FsItems (Phase 2)
            while (it.hasNext() && counter < 10000) {
                Path item = it.next();
                if (acceptor.apply(item)) {
                    String name = item.getFileName().toString();
                    if (normalizedFilter != null
                            && !name.toLowerCase(new Locale("cs")).contains(normalizedFilter)) {
                        counter++;
                        continue;
                    }
                    BasicFileAttributes attrs = Files
                            .getFileAttributeView(item, BasicFileAttributeView.class)
                            .readAttributes();
                    FsItem fsItem = new FsItem();
                    fsItem.setName(name);
                    if (attrs.isRegularFile()) {
                        fsItem.setItemType(FsItemType.FILE);
                        fsItem.setSize(attrs.size());
                    } else {
                        fsItem.setItemType(FsItemType.FOLDER);
                    }
                    fsItem.setLastChange(attrs.lastModifiedTime().toInstant().atOffset(ZoneOffset.UTC));
                    String fullRelatPath = (path == null || path.isEmpty() || path.equals("/"))
                            ? name
                            : FileSystemRepoService.normalizeRelatPath(path) + "/" + name;
                    boolean isLinked = linkedPaths.contains(fullRelatPath);
                    fsItem.setIsLinked(isLinked);
                    if (!matchesLinkFilter(isLinked, filterByLink)) {
                        counter++;
                        continue;
                    }
                    fsItemList.add(fsItem);
                }
                counter++;
            }
        }

        fsItemList.sort(comparatorFor(sortingType));

        FsItems result = new FsItems();
        Integer nextOffset = null;
        List<FsItem> appendItems;
        if ((fsItemList.size() - offset) <= maxItems) {
            appendItems = (offset == 0) ? fsItemList : fsItemList.subList(offset, fsItemList.size());
        } else {
            nextOffset = offset + maxItems;
            appendItems = fsItemList.subList(offset, nextOffset);
        }
        result.getItems().addAll(appendItems);
        if (nextOffset != null) {
            result.setLastKey(nextOffset.toString());
        }
        return result;
    }

    private Comparator<FsItem> comparatorFor(FsItemSortType sortingType) {
        Collator collator = Collator.getInstance(new Locale("cs"));
        collator.setStrength(Collator.SECONDARY);   // case-insensitive, keeps diacritics

        Comparator<FsItem> foldersFirst = (a, b) -> {
            if (a.getItemType() == FsItemType.FOLDER && b.getItemType() == FsItemType.FILE) return -1;
            if (a.getItemType() == FsItemType.FILE && b.getItemType() == FsItemType.FOLDER) return 1;
            return 0;
        };

        FsItemSortType effective = (sortingType != null) ? sortingType : FsItemSortType.NAME_ASC;
        switch (effective) {
            case NAME_ASC:
                return foldersFirst.thenComparing(FsItem::getName, collator);
            case NAME_DESC:
                return foldersFirst.thenComparing((a, b) -> collator.compare(b.getName(), a.getName()));
            case SIZE_ASC:
                return foldersFirst.thenComparing(a -> a.getSize() == null ? 0L : a.getSize());
            case SIZE_DESC:
                return foldersFirst.thenComparing((a, b) -> Long.compare(
                        b.getSize() == null ? 0L : b.getSize(),
                        a.getSize() == null ? 0L : a.getSize()));
            default:
                return foldersFirst.thenComparing(FsItem::getName, collator);
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
    
    private Function<Path, Boolean> prepareFSFilter(FsItemType filterType) {
        if (filterType == null) {
            return p -> true;
        }
        switch (filterType) {
            case FILE:   return Files::isRegularFile;
            case FOLDER: return Files::isDirectory;
            default:
                throw new BusinessException("Invalid filter.", BaseCode.INVALID_STATE)
                        .set("filterType", filterType);
        }
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
            Path repoPath = fileSystemRepoService.getPath(digiRepo, fund);
            // skip repositories whose (possibly templated) root is not currently available
            if (!Files.isDirectory(repoPath)) {
                continue;
            }
            FsRepo fsRepo = new FsRepo();
            fsRepo.setFsRepoId(digiRepo.getExternalSystemId());
            fsRepo.setName(digiRepo.getName());
            fsRepo.setCode(digiRepo.getCode());
            fsRepo.setPath(repoPath.toString());
            result.add(fsRepo);
        }
        return result;
    }
}