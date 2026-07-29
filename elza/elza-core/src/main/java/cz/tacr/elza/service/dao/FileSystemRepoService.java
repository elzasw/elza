package cz.tacr.elza.service.dao;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.ElzaTools;
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

    public static String FILE_URI_PREFIX = "file://";

    @Autowired
    private DaoPackageRepository daoPackageRepos;

    @Autowired
    private DaoRepository daoRepository;

    @Autowired
    private DaoServiceInternal daoServiceInternal;

    @Autowired
    private ExternalSystemService externalSystemService;

    public ArrDao createDao(ArrDigitalRepository digiRepo, ArrFundVersion fundVersion, String itemRelatPath) {
        Path repoPath = getPath(digiRepo, fundVersion.getFund());
        Path filePath = resolvePath(repoPath, itemRelatPath);

        ArrDigitalRepository digiRep = externalSystemService.getDigitalRepository(digiRepo.getExternalSystemId());
        // check if package exists
        List<ArrDaoPackage> daoPackages = this.daoPackageRepos.findAllByDigitalRepositoryAndFund(digiRep, fundVersion.getFund());
        ArrDaoPackage daoPackage;
        if (CollectionUtils.isEmpty(daoPackages)) {
            // create package for repo
            daoPackage = daoServiceInternal.createDaoPackage(fundVersion.getFund(), digiRep, repoPath.toString(), null);
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
        try {
            syncFilesAndFolders(dao, repoPath, filePath);
        } catch (IOException e) {
            throw new BusinessException("Failed to sync path: " + filePath, e, BaseCode.INVALID_STATE);
        }
        return dao;
    }

    static public String getRelatPath(Path rootPath, Path itemPath) {
        return rootPath.relativize(itemPath).toString();
    }

    private void syncFilesAndFolders(ArrDao dao, Path repoPath, Path srcItemPath) throws IOException {
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

        try (Stream<Path> stream = Files.walk(srcItemPath)) {
            stream.forEachOrdered(itemPath -> {
                if (skipItems.contains(itemPath)) {
                    return;
                }

                String relatName = getRelatPath(repoPath, itemPath);
                if (Files.isDirectory(itemPath)) {
                    ArrDaoFileGroup daoFileGroup = daoFileGroupsMap.remove(relatName);
                    if (daoFileGroup != null) {
                        // group exists -> do nothing
                        existingFileGroups.put(relatName, daoFileGroup);
                    } else {
                        // group not found -> add new one
                        createFileGroups.add(itemPath);
                    }
                } else if (Files.isRegularFile(itemPath)) {
                    // check file existance
                    ArrDaoFile daoFile = daoFilesMap.remove(relatName);
                    if (daoFile != null) {
                        // file exists -> only update
                        updateDaoFile(daoFile, itemPath);
                        daoFile = daoServiceInternal.persistDaoFile(daoFile);

                        existingFiles.put(relatName, daoFile);
                    } else {
                        // file not found -> add new one
                        createFiles.add(itemPath);
                    }
                } else {
                    throw new BusinessException("Unrecognized path: " + itemPath, BaseCode.INVALID_STATE);
                }
            });
        }

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
