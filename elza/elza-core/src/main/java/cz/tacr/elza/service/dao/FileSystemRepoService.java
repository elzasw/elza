package cz.tacr.elza.service.dao;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.controller.vo.CreateDaoResult;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDao.DaoType;
import cz.tacr.elza.domain.ArrDaoFile;
import cz.tacr.elza.domain.ArrDaoFileGroup;
import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DaoPackageRepository;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.service.ExternalSystemService;

@Service
public class FileSystemRepoService {

	private static final Logger log = LoggerFactory.getLogger(FileSystemRepoService.class);

    public static String FILE_URI_PREFIX = "file://";

    @Autowired
    private DaoPackageRepository daoPackageRepos;

    @Autowired
    private DaoRepository daoRepository;

    @Autowired
    private DaoServiceInternal daoServiceInternal;

    @Autowired
    private ExternalSystemService externalSystemService;

    public CreateDaoResult createDao(ArrDigitalRepository digiRepo, ArrFundVersion fundVersion, String itemRelatPath) {
    	itemRelatPath = normalizeRelatPath(itemRelatPath);
        Path repoPath = getPath(digiRepo, fundVersion.getFund());
        Path filePath = resolvePath(repoPath, itemRelatPath);

        ArrDigitalRepository digiRep = externalSystemService.getDigitalRepository(digiRepo.getExternalSystemId());
        // check if package exists
        List<ArrDaoPackage> daoPackages = this.daoPackageRepos.findAllByDigitalRepositoryAndFund(digiRep, fundVersion.getFund());
        ArrDaoPackage daoPackage;
        if (CollectionUtils.isEmpty(daoPackages)) {
            // arr_dao_package.code has a global UNIQUE constraint, so the raw repository
            // path cannot be reused across funds — include the fund id in the code to
            // keep it unique per (repository, fund).
            String packageCode = repoPath.toString() + "#fund=" + fundVersion.getFundId();
            daoPackage = daoServiceInternal.createDaoPackage(fundVersion.getFund(), digiRep, packageCode, null);
        } else {
            daoPackage = daoPackages.get(0);
        }
        // Check if Dao exists
        List<ArrDao> daos = daoRepository.findDettachedByFundAndCodes(digiRep, fundVersion.getFund(),
                                                                      Collections.singletonList(itemRelatPath));

        ArrDao dao;
        if (CollectionUtils.isNotEmpty(daos)) {
            // return first available
            dao = daos.get(0);
        } else {
            // create dao
            dao = daoServiceInternal.createDao(daoPackage, itemRelatPath, itemRelatPath, null, DaoType.ATTACHMENT);
            dao = daoServiceInternal.persistDao(dao);
        }

        // sync files and folders
        List<String> skipped;
        try {
        	skipped = syncFilesAndFolders(dao, repoPath, filePath);
        } catch (IOException e) {
            throw new BusinessException("Failed to sync path: " + filePath, e, BaseCode.INVALID_STATE);
        }
        return new CreateDaoResult(dao, skipped);
    }

    /**
     * Repository-relative paths are stored and compared with forward slashes,
     * regardless of the OS the server runs on. Call at every write site so
     * DB values stay consistent.
     */
    public static String normalizeRelatPath(String path) {
        return path == null ? null : path.replace('\\', '/');
    }

    public static String getRelatPath(Path rootPath, Path itemPath) {
        return normalizeRelatPath(rootPath.relativize(itemPath).toString());
    }

    private List<String> syncFilesAndFolders(ArrDao dao, Path repoPath, Path srcItemPath) throws IOException {
        List<ArrDaoFile> daoFiles = daoServiceInternal.getFilesByDao(dao);
        List<ArrDaoFileGroup> daoFileGroups = daoServiceInternal.getFileGroupsByDao(dao);

        Map<String, ArrDaoFile> daoFilesMap = daoFiles.stream()
                .collect(Collectors.toMap(ArrDaoFile::getCode, Function.identity(),
                        (a, b) -> { throw new BusinessException(
                                "Duplicate ArrDaoFile.code: " + a.getCode(), BaseCode.INVALID_STATE)
                                .set("daoId", a.getDao().getDaoId()); }));

        Map<String, ArrDaoFileGroup> daoFileGroupsMap = daoFileGroups.stream()
                .collect(Collectors.toMap(ArrDaoFileGroup::getCode, Function.identity(),
                        (a, b) -> { throw new BusinessException(
                                "Duplicate ArrDaoFileGroup.code: " + a.getCode(), BaseCode.INVALID_STATE)
                                .set("daoId", a.getDao().getDaoId()); }));

        List<Path> createFiles = new ArrayList<>();
        Map<String, ArrDaoFile> existingFiles = new HashMap<>();
        List<Path> createFileGroups = new ArrayList<>();
        Map<String, ArrDaoFileGroup> existingFileGroups = new HashMap<>();

        // Skip root folder
        Set<Path> skipItems;
        if (Files.isDirectory(srcItemPath)) {
            skipItems = Collections.singleton(srcItemPath);
        } else {
            skipItems = Collections.emptySet();
        }

        List<String> skipped = new ArrayList<>();
        Files.walkFileTree(srcItemPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (skipItems.contains(dir)) {
                    return FileVisitResult.CONTINUE;
                }
                // Broken junction/symlink on Windows shows up as a "directory" here —
                // but opening it will fail. Follow-links check tells us whether the
                // target actually exists.
                if (!Files.isDirectory(dir)) {
                    log.warn("Skipping non-traversable directory-like entry: {}", dir);
                    skipped.add(getRelatPath(repoPath, dir));
                    return FileVisitResult.SKIP_SUBTREE;
                }
                String relatName = getRelatPath(repoPath, dir);
                ArrDaoFileGroup daoFileGroup = daoFileGroupsMap.remove(relatName);
                if (daoFileGroup != null) {
                    existingFileGroups.put(relatName, daoFileGroup);
                } else {
                    createFileGroups.add(dir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isRegularFile()) {
                    String relatName = getRelatPath(repoPath, file);
                    ArrDaoFile daoFile = daoFilesMap.remove(relatName);
                    if (daoFile != null) {
                        updateDaoFile(daoFile, file);
                        daoFile = daoServiceInternal.persistDaoFile(daoFile);
                        existingFiles.put(relatName, daoFile);
                    } else {
                        createFiles.add(file);
                    }
                } else {
                    // Broken symlinks, device/pipe files, unreadable reparse points.
                    log.warn("Skipping unrecognized filesystem entry: {}", file);
                    skipped.add(getRelatPath(repoPath, file));
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                log.warn("Skipping unreadable filesystem entry: {}: {}", file, exc.toString());
                skipped.add(getRelatPath(repoPath, file));
                return FileVisitResult.CONTINUE;
            }
        });

        // drop old files
        daoServiceInternal.deleteDaoFiles(daoFilesMap.values());
        // drop old groups
        daoServiceInternal.deleteDaoFileGroups(daoFileGroupsMap.values());

        // create missing folders
        if(CollectionUtils.isNotEmpty(createFileGroups)) {
            createFileGroups.sort((p1, p2) -> p1.compareTo(p2) );
            for (Path fileGroupPath : createFileGroups) {
                String relatPath = getRelatPath(repoPath, fileGroupPath);
                ArrDaoFileGroup dfg = daoServiceInternal.createDaoFileGroup(relatPath, relatPath, dao);
                existingFileGroups.put(relatPath, dfg);
            }
        }
        // create files
        if (CollectionUtils.isNotEmpty(createFiles)) {
            for (Path fp : createFiles) {
                String relatPath = getRelatPath(repoPath, fp);
                ArrDaoFileGroup parentFileGroup = null;
                Path parentPath = fp.getParent();
                // do not create skipped items
                if (!fp.equals(srcItemPath) && !skipItems.contains(parentPath)) {

                    // find parent group
                    String parentName = getRelatPath(repoPath, parentPath);
                    parentFileGroup = existingFileGroups.get(parentName);
                    if (parentFileGroup == null) {
                        throw new BusinessException(
                                "Missing parent group: " + parentName + " for item: " + relatPath,
                                BaseCode.INVALID_STATE);
                    }
                }
                String fileName = fp.getFileName().toString();
                ArrDaoFile dff = daoServiceInternal.createDaoFile(relatPath, fileName, parentFileGroup, dao);
                updateDaoFile(dff, fp);
                dff = daoServiceInternal.persistDaoFile(dff);
                existingFiles.put(relatPath, dff);
            }
        }
        return skipped;
    }

    private void updateDaoFile(ArrDaoFile daoFile, Path itemPath) {
        try {
            long fileSize = Files.size(itemPath);
            daoFile.setSize(fileSize);
        } catch (IOException e) {
            throw new BusinessException("Failed to get size, path: " + itemPath, e, BaseCode.INVALID_STATE);
        }
        String mimetype = getMimetype(itemPath);
        daoFile.setMimetype(mimetype);
    }

    public String getMimetype(Path fp) {
        try {
            String type = Files.probeContentType(fp);
            if (type != null) {
                return type;
            }
        } catch (IOException e) {
        	throw new BusinessException("Failed detecting file type, path: " + fp, e, BaseCode.INVALID_STATE);
        }
        return getMimetype(fp.getFileName().toString());
    }

    public String getMimetype(String name) {
	    String type = URLConnection.guessContentTypeFromName(name);
	    if (type != null) {
	        return type;
	    }
	    String ext = FilenameUtils.getExtension(name).toLowerCase();
	    switch (ext) {
	        case "jpg":
	        case "jpeg": return "image/jpeg";
	        case "png":  return "image/png";
	        case "tif":
	        case "tiff": return "image/tiff";
	        case "gif":  return "image/gif";
	        case "webp": return "image/webp";
	        case "bmp":  return "image/bmp";
	        case "pdf":  return "application/pdf";
	        case "txt":  return "text/plain";
	        case "xml":  return "application/xml";
	        case "json": return "application/json";
	        default:     return null;
	    }
    }

    public boolean isFileSystemRepository(ArrDigitalRepository digiRep) {
        String repoUrl = digiRep.getUrl();
        if (StringUtils.isNotEmpty(repoUrl) && repoUrl.startsWith(FILE_URI_PREFIX)) {
            // we have fileSystemRepo
            return true;
        }
        return false;
    }

    public InputStream getInputStream(ArrDigitalRepository digiRepo, String filePath) throws IOException {
        Path fp = resolvePath(digiRepo, filePath);
        return Files.newInputStream(fp);
    }

    public Path resolvePath(ArrDigitalRepository digiRepo, String filePath) {
        if (!isFileSystemRepository(digiRepo)) {
            throw new BusinessException("Not a FileSystemRepository", BaseCode.INVALID_STATE)
                    .set("RepositoryId", digiRepo.getExternalSystemId());
        }
        String repoPath = digiRepo.getUrl().substring(FILE_URI_PREFIX.length());
        Path rootPath = Paths.get(repoPath).toAbsolutePath();
        return resolveInsideRoot(rootPath, filePath);
    }

    public Path getPath(ArrDigitalRepository digiRepo, ArrFund fund) {
        if (!isFileSystemRepository(digiRepo)) {
            throw new BusinessException("Not a FileSystemRepository", BaseCode.INVALID_STATE)
                    .set("RepositoryId", digiRepo.getExternalSystemId());
        }
        String repoPath = digiRepo.getUrl().substring(FILE_URI_PREFIX.length());

        ElzaTools.UrlParams params = ElzaTools.createUrlParams()
                .add("repoId", digiRepo.getExternalSystemId())
                .add("repoCode", digiRepo.getElzaCode())
                .add("repoElzaCode", digiRepo.getElzaCode());
        if (fund != null) {
            params.add("fundId", fund.getFundId())
                    .add("fundNumber", fund.getFundNumber())
                    .add("fundCode", fund.getInternalCode())
                    .add("fundMark", fund.getMark())
                    .add("institutionId", fund.getInstitution().getInstitutionId())
                    .add("institutionCode", fund.getInstitution().getInternalCode());
        }
        repoPath = ElzaTools.bindingUrlParams(repoPath, params);
        Path path = Paths.get(repoPath).toAbsolutePath();
        return path;
    }

    public Path resolvePath(ArrDigitalRepository digiRepo, ArrFund fund, String itemPath) {
        Path rootPath = getPath(digiRepo, fund);
        // check if dir exists
        if (!Files.isDirectory(rootPath)) {
            throw new BusinessException("Repository is not vailable", BaseCode.INVALID_STATE)
                    .set("repoPath", rootPath)
                    .set("fundId", fund.getFundId())
                    .set("fsrepoId", digiRepo.getExternalSystemId());
        }

        return resolvePath(rootPath, itemPath);
    }

    public Path resolvePath(Path rootRepoPath, String itemPath) {
    	return resolveInsideRoot(rootRepoPath, itemPath);
    }

    /**
     * Resolves a repository-relative path against a root and asserts the result
     * stays inside that root. Blocks directory traversal ("../foo") and absolute
     * paths that would replace the root ("/etc/passwd").
     */
    private Path resolveInsideRoot(Path rootPath, String itemPath) {
        Path normalizedRoot = rootPath.normalize();
        if (StringUtils.isBlank(itemPath)) {
            return normalizedRoot;
        }
        Path resolved = normalizedRoot.resolve(itemPath).normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            throw new BusinessException("Path escapes repository root", BaseCode.INVALID_STATE)
                    .set("root", normalizedRoot.toString())
                    .set("requested", itemPath);
        }
        return resolved;
    }

    public static boolean isInlineRenderable(String contentType) {
        if (contentType == null) {
            return false;
        }
        String lower = contentType.toLowerCase();
        return lower.startsWith("image/")
            || lower.equals("application/pdf")
            || lower.startsWith("text/");
    }
}
