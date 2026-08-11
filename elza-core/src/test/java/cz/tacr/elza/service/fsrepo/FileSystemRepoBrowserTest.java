package cz.tacr.elza.service.fsrepo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import cz.tacr.elza.controller.vo.FsItem;
import cz.tacr.elza.controller.vo.FsItemSortType;
import cz.tacr.elza.controller.vo.FsItemType;
import cz.tacr.elza.controller.vo.FsItems;
import cz.tacr.elza.controller.vo.FsLink;
import cz.tacr.elza.controller.vo.FsRepo;
import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.repository.ArrFsLinkRepository;
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

	private ElzaLocale elzaLocaleMock;
	private FileSystemRepoService serviceMock;
    private ArrFsLinkRepository daoLinkRepositoryMock;
    private FileSystemRepoBrowser browser;
    private ArrDigitalRepository repo;
    private ArrFund fund;

    @BeforeEach
    void setUp() {
    	elzaLocaleMock = Mockito.mock(ElzaLocale.class);
    	Locale csLocale = new Locale("cs");
    	Mockito.when(elzaLocaleMock.getLocale()).thenReturn(csLocale);
    	Mockito.when(elzaLocaleMock.getCollator()).thenAnswer(inv -> Collator.getInstance(csLocale));

    	serviceMock = Mockito.mock(FileSystemRepoService.class);
        daoLinkRepositoryMock = Mockito.mock(ArrFsLinkRepository.class);
        // Default: no links — tests that care override with Mockito.when(...)
        Mockito.when(daoLinkRepositoryMock.findLinksByDigitalRepository(Mockito.any()))
                .thenReturn(Collections.emptyList());

        browser = new FileSystemRepoBrowser();
        setField(browser, "fileSystemRepoService", serviceMock);
        setField(browser, "fsLinkRepository", daoLinkRepositoryMock);
        setField(browser, "elzaLocale", elzaLocaleMock);

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

        FsItems result = browser.browseItems(repo, fund, null, null, null, null, null, null, null, null);

        assertTrue(result.getItems().isEmpty());
        assertNull(result.getLastKey());
    }

    @Test
    void browse_singleLevel_sortsFoldersBeforeFiles(@TempDir Path root) throws IOException {
        Files.createFile(root.resolve("b_file.txt"));
        Files.createDirectory(root.resolve("a_folder"));
        Files.createFile(root.resolve("a_file.txt"));

        Mockito.when(serviceMock.resolvePath(repo, fund, null)).thenReturn(root);

        FsItems result = browser.browseItems(repo, fund, null, null, null, null, null, null, null, null);

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

        FsItems result = browser.browseItems(repo, fund, null, null, null, null, null, null, null, null);

        assertEquals(5L, result.getItems().get(0).getSize());
    }

    @Test
    void browse_pathIsFile_throws(@TempDir Path root) throws IOException {
        Path file = Files.createFile(root.resolve("f.txt"));
        Mockito.when(serviceMock.resolvePath(repo, fund, "f.txt")).thenReturn(file);

        assertThrows(BusinessException.class,
                () -> browser.browseItems(repo, fund, "f.txt", null, null, null, null, null, null, null));
    }

    @Test
    void browse_filterType_file_hidesFolders(@TempDir Path root) throws IOException {
        Files.createDirectory(root.resolve("folder"));
        Files.createFile(root.resolve("file.txt"));
        Mockito.when(serviceMock.resolvePath(repo, fund, null)).thenReturn(root);

        FsItems result = browser.browseItems(repo, fund, null, FsItemType.FILE, null, null, null, null, null, null);

        assertEquals(1, result.getItems().size());
        assertEquals(FsItemType.FILE, result.getItems().get(0).getItemType());
    }

    @Test
    void browse_pagination_lastKeyAdvances(@TempDir Path root) throws IOException {
        for (int i = 0; i < 5; i++) {
            Files.createFile(root.resolve(String.format("f%02d.txt", i)));
        }
        Mockito.when(serviceMock.resolvePath(eq(repo), eq(fund), any())).thenReturn(root);

        FsItems page1 = browser.browseItems(repo, fund, null, null, null, null, null, null, 2, null);
        assertEquals(2, page1.getItems().size());
        assertEquals("2", page1.getLastKey());

        FsItems page2 = browser.browseItems(repo, fund, null, null, "2", null, null, null, 2, null);
        assertEquals(2, page2.getItems().size());
        assertEquals("4", page2.getLastKey());

        FsItems page3 = browser.browseItems(repo, fund, null, null, "4", null, null, null, 2, null);
        assertEquals(1, page3.getItems().size());
        assertNull(page3.getLastKey());
    }

    @Test
    void browse_foldersFirstFalse_sortsUniformlyBySize(@TempDir Path root) throws IOException {
        Files.createDirectory(root.resolve("a-dir"));
        Files.write(root.resolve("z-file.bin"), new byte[]{1, 2, 3});
        Mockito.when(serviceMock.resolvePath(repo, fund, null)).thenReturn(root);

        FsItems result = browser.browseItems(repo, fund, null, null, null, null,
                FsItemSortType.SIZE_DESC, null, null, false);

        // With foldersFirst=false, the file (size=3) sorts before the folder (size=0).
        assertEquals("z-file.bin", result.getItems().get(0).getName());
        assertEquals("a-dir",      result.getItems().get(1).getName());
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

        FsItems result = browser.browseItems(repo, fund, null, null, null, null, null, null, null, null);
        assertEquals("a.txt", result.getItems().get(0).getName());
        assertEquals("b.txt", result.getItems().get(1).getName());
        assertEquals("c.txt", result.getItems().get(2).getName());
    }

    @Test
    void browse_sortNameDesc(@TempDir Path root) throws IOException {
        Files.createFile(root.resolve("a.txt"));
        Files.createFile(root.resolve("c.txt"));
        Mockito.when(serviceMock.resolvePath(repo, fund, null)).thenReturn(root);

        FsItems result = browser.browseItems(repo, fund, null, null, null, null, FsItemSortType.NAME_DESC, null, null, null);
        assertEquals("c.txt", result.getItems().get(0).getName());
        assertEquals("a.txt", result.getItems().get(1).getName());
    }

    @Test
    void browse_sortSizeDesc(@TempDir Path root) throws IOException {
        Files.write(root.resolve("small.txt"), new byte[10]);
        Files.write(root.resolve("large.txt"), new byte[1000]);
        Files.write(root.resolve("medium.txt"), new byte[100]);
        Mockito.when(serviceMock.resolvePath(repo, fund, null)).thenReturn(root);

        FsItems result = browser.browseItems(repo, fund, null, null, null, null, FsItemSortType.SIZE_DESC, null, null, null);
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

        FsItems result = browser.browseItems(repo, fund, null, null, null, null, FsItemSortType.NAME_ASC, null, null, null);
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

        FsItems result = browser.browseItems(repo, fund, null, null, null, null, null, "photo", null, null);
        assertEquals(2, result.getItems().size());
        // Case-insensitive
    }

    @Test
    void browse_fileFilter_blank_ignored(@TempDir Path root) throws IOException {
        Files.createFile(root.resolve("a.txt"));
        Files.createFile(root.resolve("b.txt"));
        Mockito.when(serviceMock.resolvePath(repo, fund, null)).thenReturn(root);

        FsItems result = browser.browseItems(repo, fund, null, null, null, null, null, "", null, null);
        assertEquals(2, result.getItems().size());
    }

    // ---------- daoLinkRepository ----------
    
    @Test
    void browse_links_matchesFullRelatPath(@TempDir Path root) throws IOException {
        Files.createFile(root.resolve("linked.jpg"));
        Files.createFile(root.resolve("unlinked.jpg"));
        Mockito.when(serviceMock.resolvePath(repo, fund, null)).thenReturn(root);
        Object[] linkedRow = {"linked.jpg", 100, 1, "Fond A"};
        Mockito.when(daoLinkRepositoryMock.findLinksByDigitalRepository(repo))
                .thenReturn(Collections.<Object[]>singletonList(linkedRow));

        FsItems result = browser.browseItems(repo, fund, null, null, null, null, null, null, null, null);

        FsItem linked = result.getItems().stream()
                .filter(f -> f.getName().equals("linked.jpg")).findFirst().orElseThrow();
        FsItem unlinked = result.getItems().stream()
                .filter(f -> f.getName().equals("unlinked.jpg")).findFirst().orElseThrow();
        assertEquals(1, linked.getLinks().size());
        FsLink link = linked.getLinks().get(0);
        assertEquals(Integer.valueOf(100), link.getNodeId());
        assertEquals(Integer.valueOf(1), link.getFundId());
        assertEquals("Fond A", link.getFundName());
        assertEquals("Uzel #100", link.getNodeLabel());
        assertTrue(unlinked.getLinks().isEmpty());
    }

    @Test
    void browse_links_nestedPath(@TempDir Path root) throws IOException {
        Path sub = Files.createDirectory(root.resolve("sub"));
        Files.createFile(sub.resolve("file.jpg"));
        Mockito.when(serviceMock.resolvePath(repo, fund, "sub")).thenReturn(sub);
        Object[] nestedRow = {"sub/file.jpg", 100, 1, "Fond A"};
        Mockito.when(daoLinkRepositoryMock.findLinksByDigitalRepository(repo))
                .thenReturn(Collections.<Object[]>singletonList(nestedRow));

        FsItems result = browser.browseItems(repo, fund, "sub", null, null, null, null, null, null, null);

        assertEquals(1, result.getItems().get(0).getLinks().size());
    }

    @Test
    void browse_multipleLinks_returnsAll(@TempDir Path root) throws IOException {
        Files.createFile(root.resolve("shared.jpg"));
        Mockito.when(serviceMock.resolvePath(repo, fund, null)).thenReturn(root);
        Object[] row1 = {"shared.jpg", 100, 1, "Fond A"};
        Object[] row2 = {"shared.jpg", 200, 2, "Fond B"};
        Mockito.when(daoLinkRepositoryMock.findLinksByDigitalRepository(repo))
                .thenReturn(Arrays.<Object[]>asList(row1, row2));

        FsItems result = browser.browseItems(repo, fund, null, null, null, null, null, null, null, null);

        List<FsLink> links = result.getItems().get(0).getLinks();
        assertEquals(2, links.size());
    }

    // ---------- listDaoFiles ----------

    /** Routes getPath/resolvePath through the real path arithmetic on the temp dir. */
    private void stubDaoFileResolution(Path root) {
        Path normRoot = root.normalize();
        Mockito.when(serviceMock.getPath(repo, fund)).thenReturn(root);
        Mockito.when(serviceMock.resolvePath(any(Path.class), any()))
                .thenAnswer(inv -> {
                    String rel = inv.getArgument(1);
                    return (rel == null || rel.isBlank()) ? normRoot : normRoot.resolve(rel);
                });
    }

    @Test
    void listDaoFiles_singleFile(@TempDir Path root) throws IOException {
        Files.write(root.resolve("scan.jpg"), new byte[]{1, 2, 3});
        stubDaoFileResolution(root);
        Mockito.when(serviceMock.getMimetype("scan.jpg")).thenReturn("image/jpeg");

        FileSystemRepoBrowser.FsDaoListing listing = browser.listDaoFiles(repo, fund, "scan.jpg", 10);

        assertEquals(1, listing.files().size());
        assertFalse(listing.truncated());
        assertEquals("scan.jpg", listing.files().get(0).relatPath());
        assertEquals("scan.jpg", listing.files().get(0).fileName());
        assertEquals(3L, listing.files().get(0).size());
        assertEquals("image/jpeg", listing.files().get(0).mimetype());
    }

    @Test
    void listDaoFiles_recursive_flatSortedByPath(@TempDir Path root) throws IOException {
        Path dir = Files.createDirectory(root.resolve("dir"));
        Path sub = Files.createDirectory(dir.resolve("sub"));
        Files.createFile(dir.resolve("b.txt"));
        Files.createFile(dir.resolve("a.txt"));
        Files.createFile(sub.resolve("deep.txt"));
        stubDaoFileResolution(root);

        FileSystemRepoBrowser.FsDaoListing listing = browser.listDaoFiles(repo, fund, "dir", 10);

        // flat list of regular files only, ordered by repository-relative path
        assertEquals(3, listing.files().size());
        assertFalse(listing.truncated());
        assertEquals("dir/a.txt", listing.files().get(0).relatPath());
        assertEquals("dir/b.txt", listing.files().get(1).relatPath());
        assertEquals("dir/sub/deep.txt", listing.files().get(2).relatPath());
        assertEquals("deep.txt", listing.files().get(2).fileName());
    }

    @Test
    void listDaoFiles_capEnforced_setsTruncated(@TempDir Path root) throws IOException {
        Path dir = Files.createDirectory(root.resolve("dir"));
        for (int i = 0; i < 5; i++) {
            Files.createFile(dir.resolve("f" + i + ".txt"));
        }
        stubDaoFileResolution(root);

        FileSystemRepoBrowser.FsDaoListing listing = browser.listDaoFiles(repo, fund, "dir", 3);

        assertEquals(3, listing.files().size());
        assertTrue(listing.truncated());
    }

    @Test
    void listDaoFiles_exactlyAtCap_notTruncated(@TempDir Path root) throws IOException {
        Path dir = Files.createDirectory(root.resolve("dir"));
        for (int i = 0; i < 3; i++) {
            Files.createFile(dir.resolve("f" + i + ".txt"));
        }
        stubDaoFileResolution(root);

        FileSystemRepoBrowser.FsDaoListing listing = browser.listDaoFiles(repo, fund, "dir", 3);

        assertEquals(3, listing.files().size());
        assertFalse(listing.truncated());
    }

    @Test
    void listDaoFiles_missingPath_returnsSyntheticEntry(@TempDir Path root) throws IOException {
        stubDaoFileResolution(root);

        FileSystemRepoBrowser.FsDaoListing listing = browser.listDaoFiles(repo, fund, "does-not-exist", 10);

        assertEquals(1, listing.files().size());
        assertFalse(listing.truncated());
        FileSystemRepoBrowser.FsDaoFile stub = listing.files().get(0);
        assertEquals("does-not-exist", stub.relatPath());
        assertEquals("does-not-exist", stub.fileName());
        assertEquals(0L, stub.size());
    }

    @Test
    void normalizeRelatPath_variants() {
        assertNull(FileSystemRepoService.normalizeRelatPath(null));
        assertEquals("", FileSystemRepoService.normalizeRelatPath(""));
        assertEquals("file.jpg", FileSystemRepoService.normalizeRelatPath("file.jpg"));
        assertEquals("folder/file.jpg", FileSystemRepoService.normalizeRelatPath("folder/file.jpg"));
        assertEquals("folder/file.jpg", FileSystemRepoService.normalizeRelatPath("folder\\file.jpg"));
        assertEquals("a/b/c/d.txt", FileSystemRepoService.normalizeRelatPath("a\\b\\c\\d.txt"));
        assertEquals("a/b/c/file.jpg", FileSystemRepoService.normalizeRelatPath("a/b\\c/file.jpg"));
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