package cz.tacr.elza.service.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoPackage;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Image of file system
 * 
 *
 */
public class FileSystemImage {

    final String repoPath;
    private Path rootPath;

    Map<String, FileSystemItem> items = new TreeMap<>();
    private List<ArrDao> daos = new ArrayList<>();
    private ArrDaoPackage virtPackage = new ArrDaoPackage();

    /*!
     * ID of external system definition
     */
    private Integer digiRepId;

    public FileSystemImage(final String repoPath, final ArrDigitalRepository digiRep) {
        this.digiRepId = digiRep.getExternalSystemId();
        this.rootPath = Paths.get(repoPath).toAbsolutePath();
        this.repoPath = rootPath.toString();
        if (!Files.isDirectory(rootPath)) {
            throw new BusinessException("Incorrect path, path: repoPath", BaseCode.INVALID_STATE);
        }
        virtPackage.setCode(repoPath);
        virtPackage.setDaoPackageId(-digiRep.getExternalSystemId());
        virtPackage.setDigitalRepository(digiRep);
    }

    public Integer getDigiRepId() {
        return digiRepId;
    }

    public void loadData() throws IOException {
        
        try (Stream<Path> walk = Files.walk(rootPath)) {
            walk.forEach(fsi-> {
                String relatPathName = this.getRelatPath(fsi);
                if (StringUtils.isEmpty(relatPathName)) {
                    // skip root item
                    return;
                }
                FileSystemItem item;
                if(Files.isDirectory(fsi)) {
                    FileSystemFolder fsd = new FileSystemFolder(virtPackage, fsi, -(items.size() + 1), relatPathName);
                    item = fsd;
                } else {
                    FileSystemFile fsf = new FileSystemFile(virtPackage, fsi, -(items.size() + 1), relatPathName);
                    item = fsf;                    
                }
                // add to parent
                Path parentPath = fsi.getParent();
                String parentName = rootPath.relativize(parentPath).toString();
                FileSystemItem parent = items.get(parentName);
                if(parent!=null && parent instanceof FileSystemFolder) {
                    FileSystemFolder parentFolder = (FileSystemFolder)parent;
                    parentFolder.addItem(item);
                }
                
                items.put(relatPathName, item);
            });
        }

        // create Daos
        items.forEach((relatPath, item) -> {
            daos.add(item.getDao());
        });
    }

    public List<ArrDao> getDaos() {
        return daos;
    }

    public ArrDaoPackage getVirtPackage() {
        return virtPackage;
    }

    public ArrDao findDaoById(Integer daoId) {
        for (ArrDao dao : daos) {
            if (dao.getDaoId().equals(daoId)) {
                return dao;
            }
        }
        return null;
    }

    public String getRepoPath() {
        return this.repoPath;
    }

    public FileSystemItem getItem(String filePath) {
        return items.get(filePath);
    }

    public void walk(String filePath, Consumer<? super Path> consumer) throws IOException {
        Path p = Paths.get(repoPath, filePath);
        if (Files.isDirectory(p)) {
            try (Stream<Path> stream = Files.walk(p)) {
                stream.forEach(consumer);
            }
        } else {
            consumer.accept(p);
        }
    }

    public String getRelatPath(Path itemPath) {
        return rootPath.relativize(itemPath).toString();
    }

    public InputStream getInputStream(String filePath) throws IOException {
        Path p = rootPath.resolve(filePath);
        return Files.newInputStream(p);
    }

}
