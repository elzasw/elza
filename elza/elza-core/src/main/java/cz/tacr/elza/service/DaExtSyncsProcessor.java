package cz.tacr.elza.service;

import cz.tacr.da.ApiException;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaSyncQueueItem;
import jakarta.xml.bind.JAXBException;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static cz.tacr.elza.connector.DaConnector.FILE_TRANSFER_ERROR_CODE;

@Component
public class DaExtSyncsProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DaExtSyncsProcessor.class);

    @Autowired
    private DaService daService;

    @Autowired
    private PackageInfoService packageInfoService;

    private volatile Thread asyncThread = null;

    private final Object lock = new Object();

    private static final int QUEUE_CHECK_TIME_INTERVAL = 10000;

    private static final int DOWNLOAD_CHECK_TIME_INTERVAL = 100;

    private static final int DEFAULT_IMPORT_LIST_SIZE = 100;

    private int importListSize = DEFAULT_IMPORT_LIST_SIZE;

    private enum ThreadStatus {
        RUNNING, STOP_REQUEST, STOPPED
    }

    private ThreadStatus status;

    public void startExtSyncs() {
        synchronized (lock) {
            status = ThreadStatus.RUNNING;
            if (this.asyncThread == null) {
                this.asyncThread = new Thread(this,"DaExtSyncsProcessor");
                this.asyncThread.start();
            }
        }
    }

    @Override
    public void run() {
        synchronized (lock) {
            try {
                while (status == ThreadStatus.RUNNING) {
                    // pokud true - pauza po ukončení práce procesoru
                    boolean wait = true;
                    try {
                        List<DaSyncQueueItem> syncQueueItemList = daService.getNextItems(importListSize, DaSyncQueueItem.QueueItemState.UPDATE, DaSyncQueueItem.QueueItemState.IMPORT_NEW);
                        if (CollectionUtils.isNotEmpty(syncQueueItemList)) {
                            ArrDigitalRepository digitalRepository = syncQueueItemList.get(0).getDigitalRepository();
                            String batchId = daService.downloadAips(digitalRepository, syncQueueItemList);

                            while (!daService.downloadStatusFinished(digitalRepository, batchId)) {
                                try {
                                    // wake up every minute to retry
                                    lock.wait(DOWNLOAD_CHECK_TIME_INTERVAL);
                                } catch (InterruptedException e) {
                                    logger.error(e.getMessage(), e);
                                    break;
                                }
                            }

                            byte[] downloadedBytes = null;
                            try {
                                downloadedBytes = daService.downloadDownload(digitalRepository, batchId);
                            } catch (ApiException e) {
                                if (e.getCode() == FILE_TRANSFER_ERROR_CODE) {
                                    downloadedBytes = daService.downloadFileTransfer(digitalRepository, batchId);
                                }
                            }
                            processPackageInfo(downloadedBytes);



                            // pokud je vše v pořádku - maximální velikost dávky pro čtení
                            importListSize = DEFAULT_IMPORT_LIST_SIZE;
                            // pauza po ukončení práce procesoru není potřeba
                            wait = false;
                        }
                    } catch (Exception ex) {
                        logger.error("Failed to process item. ", ex);
                        // v případě chyby číst po 1 záznamu
                        importListSize = 1;
                    }
                    if (wait) {
                        try {
                            // wake up every minute to retry
                            lock.wait(QUEUE_CHECK_TIME_INTERVAL);
                        } catch (InterruptedException e) {
                            logger.error(e.getMessage(), e);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("DaExtSyncsProcessor - processor thread error", e);
            }
            status = ThreadStatus.STOPPED;
            lock.notifyAll();
            logger.error("DaExtSyncsProcessor - thread finished");
        }
    }

    public void processPackageInfo(byte[] bytes) throws IOException {
        Path tempZip = Files.createTempFile("temp", ".zip");
        try (FileOutputStream fos = new FileOutputStream(tempZip.toFile())) {
            fos.write(bytes);
        }

        Path tempDir = Files.createTempDirectory("unzipped");

        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(tempZip))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path filePath = tempDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zipInputStream, filePath);
                }
            }
        }

        try (Stream<Path> str = Files.walk(tempDir).filter(path -> path.toString().endsWith("PACKAGE-INFO.xml"))) {
            str.forEach(path -> {
                File file = path.toFile();
                try {
                    packageInfoService.processPackageInfo(file);
                } catch (Exception e) {
                    logger.error("Nastala chyba při zpracování souboru package-info.xml", e);
                }
            });
        }

        // Odstranit dočasné soubory a adresáře
        try (Stream<Path> str = Files.walk(tempDir)) {
            str.map(Path::toFile).forEach(File::delete);
        }
        Files.delete(tempZip);


    }

}
