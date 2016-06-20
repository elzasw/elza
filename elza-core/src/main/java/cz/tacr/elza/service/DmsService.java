package cz.tacr.elza.service;

import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.repository.FileRepository;
import org.aspectj.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Dms Service
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 20.6.2016
 */
@Service
public class DmsService {

    /**
     * Složka se soubory DMS
     */
    @Value("${elza.dms.fileDir}")
    private String dmsFileDirectory;

    @Autowired
    private FileRepository fileRepository;

    /**
     * Získání souboru
     *
     * @param fileId id souboru
     * @return dms file
     */
    public DmsFile getFile(final Integer fileId) {
        Assert.notNull(fileId);

        return fileRepository.findOne(fileId);
    }

    /**
     * Uložení objektu souboru
     *
     * @param file soubor
     */
    public void storeFile(final DmsFile file) {
        fileRepository.save(file);

        if (file.getFile() != null) {
            try {
                FileUtil.copyFile(file.getFile(), new File(dmsFileDirectory + File.separator + file.getFileId()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Smazání objektu souboru
     *
     * @param fileId
     */
    public void deleteFile(final Integer fileId) {
        Assert.notNull(fileId);

        fileRepository.delete(fileId);
    }

    /**
     * Smazání objektu souboru
     *
     * @param file
     */
    public void deleteFile(final DmsFile file) {
        Assert.notNull(file);

        fileRepository.delete(file);
    }

    /**
     * Zíkání cesty k reálnému souboru
     *
     * @param file dms file
     * @return cesta
     */
    public String getFilePath(DmsFile file) {
        return dmsFileDirectory + File.separator + file.getFileId();
    }

    /**
     * Vyhledávání souborů
     *
     * @param search hledaný text
     * @param from od záznamu
     * @param count počet záznamů
     * @return list vyhledaných
     */
    public List<DmsFile> findFileByText(@Nullable final String search, @Nullable final Integer from, @Nullable final Integer count) {
        return fileRepository.findByText(search, from == null ? 0 : from, count == null ? 20 : null);
    }

    /**
     * Vrátí celkový počet nalezených záznamů
     *
     * @param search hledaný text
     * @return Celkový počet vyhledaných záznamů
     */
    public long findFileByTextCount(@Nullable final String search) {
        return search == null ? fileRepository.count() : fileRepository.findByTextCount(search);
    }
}
