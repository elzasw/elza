package cz.tacr.elza.service.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.controller.vo.FsItem;
import cz.tacr.elza.controller.vo.FsItemSortType;
import cz.tacr.elza.controller.vo.FsItemType;
import cz.tacr.elza.controller.vo.FsItems;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Directory-listing and browsing operations over a filesystem repository.
 * Primitives (path resolution, mime detection, input streams) live in
 * {@link FileSystemRepoService}; this class composes them into the UI-facing
 * browse contract.
 */
@Service
public class FileSystemRepoBrowser {

    @Autowired
    private FileSystemRepoService fileSystemRepoService;

    public FsItems browseItems(ArrDigitalRepository digiRepo, ArrFund fund,
                               String path, FsItemType filterType,
                               String lastKey, FsItemSortType sortingType,
                               String fileFilter) throws IOException {
        Path itemPath = fileSystemRepoService.resolvePath(digiRepo, fund, path);
        if (!Files.isDirectory(itemPath)) {
            throw new BusinessException("Item is not directory.", BaseCode.INVALID_STATE)
                    .set("fsrepoId", digiRepo.getExternalSystemId())
                    .set("path", path)
                    .set("itemPath", itemPath);
        }

        int maxItems = 1000;
        if (digiRepo.getCode() != null && digiRepo.getCode().endsWith("_DEBUG")) {
            // TODO: replace with pageSize request parameter (Phase 2)
            maxItems = 2;
        }
        int offset = (lastKey != null) ? Integer.parseInt(lastKey) : 0;

        Function<Path, Boolean> acceptor = prepareFSFilter(filterType);

        List<FsItem> fsItemList = new ArrayList<>();
        try (Stream<Path> ds = Files.list(itemPath)) {
            Iterator<Path> it = ds.iterator();
            int counter = 0;
            // TODO: expose truncation flag in FsItems (Phase 2)
            while (it.hasNext() && counter < 10000) {
                Path item = it.next();
                if (acceptor.apply(item)) {
                    BasicFileAttributes attrs = Files
                            .getFileAttributeView(item, BasicFileAttributeView.class)
                            .readAttributes();
                    FsItem fsItem = new FsItem();
                    fsItem.setName(item.getFileName().toString());
                    if (attrs.isRegularFile()) {
                        fsItem.setItemType(FsItemType.FILE);
                        fsItem.setSize(attrs.size());
                    } else {
                        fsItem.setItemType(FsItemType.FOLDER);
                    }
                    fsItem.setLastChange(attrs.lastModifiedTime().toInstant().atOffset(ZoneOffset.UTC));
                    fsItemList.add(fsItem);
                }
                counter++;
            }
        }

        fsItemList.sort((c1, c2) -> {
            if (c1.getItemType() == FsItemType.FILE && c2.getItemType() == FsItemType.FOLDER) return 1;
            if (c1.getItemType() == FsItemType.FOLDER && c2.getItemType() == FsItemType.FILE) return -1;
            return c1.getName().compareTo(c2.getName());
        });

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
}