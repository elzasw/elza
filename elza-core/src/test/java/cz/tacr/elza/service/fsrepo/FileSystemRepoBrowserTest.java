package cz.tacr.elza.service.fsrepo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import cz.tacr.elza.controller.vo.FsItem;
import cz.tacr.elza.controller.vo.FsItemSortType;
import cz.tacr.elza.controller.vo.FsItemType;
import cz.tacr.elza.controller.vo.FsItems;
import cz.tacr.elza.controller.vo.FsRepo;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.service.dao.FileSystemRepoBrowser;
import cz.tacr.elza.service.dao.FileSystemRepoService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class FileSystemRepoBrowserTest {

    private FileSystemRepoService serviceMock;
    private DaoLinkRepository daoLinkRepositoryMock;
    private FileSystemRepoBrowser browser;
    private ArrDigitalRepository repo;
    private ArrFund fund;

    @BeforeEach
    void setUp() {
        serviceMock = Mockito.mock(FileSystemRepoService.class);
        daoLinkRepositoryMock = Mockito.mock(DaoLinkRepository.class);
        // Default: no linked paths — tests that care override with Mockito.when(...)
        Mockito.when(daoLinkRepositoryMock.findLinkedCodesByDigitalRepository(Mockito.any()))
                .thenReturn(Collections.emptyList());

        browser = new FileSystemRepoBrowser();
        setField(browser, "fileSystemRepoService", serviceMock);
        setField(browser, "daoLinkRepository", daoLinkRepositoryMock);

        repo = new ArrDigitalRepository();
        repo.setExternalSystemId(42);
        repo.setName("test-repo");
        repo.setCode("REPO");

        fund = new ArrFund();
        fund.setFundId(1);
    }

    // ---------- browseItems ----------

    @Test
    void browse_emptyDirectory(@TempDir Path root) throws IOException {
        Mockito.when(serviceMock.resolvePath(repo, fund, null)).thenReturn(root);

        FsItems result = browser.browseItems(repo, fund, null, null, null, null, null, null);

        assertTrue(result.getItems().isEmpty());
        assertNull(result.getLastKey());
    }

    @Test
    void browse_singleLevel_sortsFoldersBeforeFiles(@TempDir Path root) throws IOException {
        Files.createFile(root.resolve("b_file.txt"));
        Files.createDirectory(root.resolve("a_folder"));
        Files.createFile(root.resolve("a_file.txt"));

        Mockito.when(serviceMock.resolvePath(repo, fund, null)).thenReturn(root);

        FsItems result = browser.browseItems(repo, fund, null, null, null, null, null, null);

        assertEquals(3, result.getItems().size());
        assertEquals(FsItemType.FOLDER, result.getItems().get(0).getItemType());
        assertEquals("a_folder", result.getItems().get(0).getName());
        assertEquals(FsItemType.FILE, result.getItems().get(1).getItemType());
        assertEquals("a_file.txt", result.getItems().get(1).getName());
        assertEquals("b_file.txt", result.getItems().get(2).getName());
    }

    @Test
    void browse_fileSize_isPreserved(@TempDir Path root) throws IOException {
        Path file = root.resolve("data.bin");
        Files.write(file, new byte[] { 1, 2, 3, 4, 5 });
        Mockito.when(serviceMock.resolvePath(repo, fund, null)).thenReturn(root);

        FsItems result = browser.browseItems(repo, fund, null, null, null, null, null, null);

        assertEquals(5L, result.getItems().get(0).getSize());
    }

    @Test
    void browse_pathIsFile_throws(@TempDir Path root) throws IOException {
        Path file = Files.createFile(root.resolve("f.txt"));
        Mockito.when(serviceMock.resolvePath(repo, fund, "f.txt")).thenReturn(file);

        assertThrows(BusinessException.class,
                () -> browser.browseItems(repo, fund, "f.txt", null, null, null, null, null));
    }

    @Test
    void browse_filterType_file_hidesFolders(@TempDir Path root) throws IOException {
        Files.createDirectory(root.resolve("folder"));
        Files.createFile(root.resolve("file.txt"));
        Mockito.when(serviceMock.resolvePath(repo, fund, null)).thenReturn(root);

        FsItems result = browser.browseItems(repo, fund, null, FsItemType.FILE, null, null, null, null);

        assertEquals(1, result.getItems().size());
        assertEquals(FsItemType.FILE, result.getItems().get(0).getItemType());
    }

    @Test
    void browse_pagination_lastKeyAdvances(@TempDir Path root) throws IOException {
        repo.setCode("REPO_DEBUG");   // triggers maxItems=2
        for (int i = 0; i < 5; i++) {
            Files.createFile(root.resolve(String.format("f%02d.txt", i)));
        }
        Mockito.when(serviceMock.resolvePath(eq(repo), eq(fund), any())).thenReturn(root);

        FsItems page1 = browser.browseItems(repo, fund, null, null, null, null, null, null);
        assertEquals(2, page1.getItems().size());
        assertEquals("2", page1.getLastKey());

        FsItems page2 = browser.browseItems(repo, fund, null, null, "2", null, null, null);
        assertEquals(2, page2.getItems().size());
        assertEquals("4", page2.getLastKey());

        FsItems page3 = browser.browseItems(repo, fund, null, null, "4", null, null, null);
        assertEquals(1, page3.getItems().size());
        assertNull(page3.getLastKey());
    }

    // ---------- listRepos ----------

    @Test
    void listRepos_availableFsRepoIsIncluded(@TempDir Path root) {
        Mockito.when(serviceMock.isFileSystemRepository(repo)).thenReturn(true);
        Mockito.when(serviceMock.getPath(repo, fund)).thenReturn(root);

        List<FsRepo> result = browser.listRepos(fund, Collections.singletonList(repo));

        assertEquals(1, result.size());
        assertEquals(42, result.get(0).getFsRepoId());
        assertEquals("test-repo", result.get(0).getName());
    }

    @Test
    void listRepos_unavailableRepoIsSkipped(@TempDir Path root) {
        Path missing = root.resolve("does-not-exist");
        Mockito.when(serviceMock.isFileSystemRepository(repo)).thenReturn(true);
        Mockito.when(serviceMock.getPath(repo, fund)).thenReturn(missing);

        List<FsRepo> result = browser.listRepos(fund, Collections.singletonList(repo));

        assertTrue(result.isEmpty());
    }

    @Test
    void listRepos_nonFsRepoIsSkipped() {
        Mockito.when(serviceMock.isFileSystemRepository(repo)).thenReturn(false);

        List<FsRepo> result = browser.listRepos(fund, Collections.singletonList(repo));

        assertTrue(result.isEmpty());
        Mockito.verify(serviceMock, Mockito.never()).getPath(any(), any());
    }

    @Test
    void listRepos_nullOrEmptyList_returnsEmpty() {
        assertTrue(browser.listRepos(fund, null).isEmpty());
        assertTrue(browser.listRepos(fund, Collections.emptyList()).isEmpty());
    }

    @Test
    void browse_sortNameAsc_isDefault(@TempDir Path root) throws IOException {
        Files.createFile(root.resolve("c.txt"));
        Files.createFile(root.resolve("a.txt"));
        Files.createFile(root.resolve("b.txt"));
        Mockito.when(serviceMock.resolvePath(repo, fund, null)).thenReturn(root);

        FsItems result = browser.browseItems(repo, fund, null, null, null, null, null, null);
        assertEquals("a.txt", result.getItems().get(0).getName());
        assertEquals("b.txt", result.getItems().get(1).getName());
        assertEquals("c.txt", result.getItems().get(2).getName());
    }

    @Test
    void browse_sortNameDesc(@TempDir Path root) throws IOException {
        Files.createFile(root.resolve("a.txt"));
        Files.createFile(root.resolve("c.txt"));
        Mockito.when(serviceMock.resolvePath(repo, fund, null)).thenReturn(root);

        FsItems result = browser.browseItems(repo, fund, null, null, null, null, FsItemSortType.NAME_DESC, null);
        assertEquals("c.txt", result.getItems().get(0).getName());
        assertEquals("a.txt", result.getItems().get(1).getName());
    }

    @Test
    void browse_sortSizeDesc(@TempDir Path root) throws IOException {
        Files.write(root.resolve("small.txt"), new byte[10]);
        Files.write(root.resolve("large.txt"), new byte[1000]);
        Files.write(root.resolve("medium.txt"), new byte[100]);
        Mockito.when(serviceMock.resolvePath(repo, fund, null)).thenReturn(root);

        FsItems result = browser.browseItems(repo, fund, null, null, null, null, FsItemSortType.SIZE_DESC, null);
        assertEquals("large.txt", result.getItems().get(0).getName());
        assertEquals("medium.txt", result.getItems().get(1).getName());
        assertEquals("small.txt", result.getItems().get(2).getName());
    }

    @Test
    void browse_czechDiacritics_sortedByCollator(@TempDir Path root) throws IOException {
        Files.createFile(root.resolve("cepy.txt"));
        Files.createFile(root.resolve("čepy.txt"));
        Files.createFile(root.resolve("dnes.txt"));
        Mockito.when(serviceMock.resolvePath(repo, fund, null)).thenReturn(root);

        FsItems result = browser.browseItems(repo, fund, null, null, null, null, FsItemSortType.NAME_ASC, null);
        // In Czech: c < č < d
        assertEquals("cepy.txt", result.getItems().get(0).getName());
        assertEquals("čepy.txt", result.getItems().get(1).getName());
        assertEquals("dnes.txt", result.getItems().get(2).getName());
    }

    @Test
    void browse_fileFilter_substringMatch(@TempDir Path root) throws IOException {
        Files.createFile(root.resolve("photo_2024.jpg"));
        Files.createFile(root.resolve("scan_2024.jpg"));
        Files.createFile(root.resolve("PHOTO_older.jpg"));
        Mockito.when(serviceMock.resolvePath(repo, fund, null)).thenReturn(root);

        FsItems result = browser.browseItems(repo, fund, null, null, null, null, null, "photo");
        assertEquals(2, result.getItems().size());
        // Case-insensitive
    }

    @Test
    void browse_fileFilter_blank_ignored(@TempDir Path root) throws IOException {
        Files.createFile(root.resolve("a.txt"));
        Files.createFile(root.resolve("b.txt"));
        Mockito.when(serviceMock.resolvePath(repo, fund, null)).thenReturn(root);

        FsItems result = browser.browseItems(repo, fund, null, null, null, null, null, "");
        assertEquals(2, result.getItems().size());
    }

    // ---------- daoLinkRepository ----------
    
    @Test
    void browse_isLinked_matchesFullRelatPath(@TempDir Path root) throws IOException {
        Files.createFile(root.resolve("linked.jpg"));
        Files.createFile(root.resolve("unlinked.jpg"));
        Mockito.when(serviceMock.resolvePath(repo, fund, null)).thenReturn(root);
        Mockito.when(daoLinkRepositoryMock.findLinkedCodesByDigitalRepository(repo))
                .thenReturn(List.of("linked.jpg"));

        FsItems result = browser.browseItems(repo, fund, null, null, null, null, null, null);

        FsItem linked = result.getItems().stream()
                .filter(f -> f.getName().equals("linked.jpg")).findFirst().orElseThrow();
        FsItem unlinked = result.getItems().stream()
                .filter(f -> f.getName().equals("unlinked.jpg")).findFirst().orElseThrow();
        assertTrue(linked.getIsLinked());
        assertFalse(unlinked.getIsLinked());
    }

    @Test
    void browse_isLinked_nestedPath(@TempDir Path root) throws IOException {
        Path sub = Files.createDirectory(root.resolve("sub"));
        Files.createFile(sub.resolve("file.jpg"));
        Mockito.when(serviceMock.resolvePath(repo, fund, "sub")).thenReturn(sub);
        Mockito.when(daoLinkRepositoryMock.findLinkedCodesByDigitalRepository(repo))
                .thenReturn(List.of("sub/file.jpg"));

        FsItems result = browser.browseItems(repo, fund, "sub", null, null, null, null, null);

        assertTrue(result.getItems().get(0).getIsLinked());
    }

    @Test
    void browse_isLinked_windowsSeparators_normalized(@TempDir Path root) throws IOException {
        Path sub = Files.createDirectory(root.resolve("sub"));
        Files.createFile(sub.resolve("file.jpg"));
        Mockito.when(serviceMock.resolvePath(repo, fund, "sub")).thenReturn(sub);
        // DB stored the code with backslash (Windows-deployed server)
        Mockito.when(daoLinkRepositoryMock.findLinkedCodesByDigitalRepository(repo))
                .thenReturn(List.of("sub\\file.jpg"));

        FsItems result = browser.browseItems(repo, fund, "sub", null, null, null, null, null);

        assertTrue(result.getItems().get(0).getIsLinked());
    }

    // ---------- helper ----------

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}