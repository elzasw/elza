package cz.tacr.elza.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import cz.tacr.elza.exception.SystemException;

/**
 * Serviska pro práci s procesy.
 *
 */
@Service
public class ProcessService {
    private static final Logger logger = LoggerFactory.getLogger(ProcessService.class);
    @Autowired
    private BeanFactory beanFactory;

    /**
     * Zpracování procesu - pro předaný proces zpracuje jeho výstup, zařídí čekání na dokončení
     *
     * @param process   proces
     * @param maxmillis maxilální doba čekání na dkokončení, pokud je 0, čeká se nekonečně dlouho
     * @throws IOException chyba při práci s procesem
     */
    public void process(final Process process, final long maxmillis) throws IOException {
        Future<Integer> future = beanFactory.getBean(ProcessService.class).manageProcess(process);
        try {
            waitForProcess(future, process.toString(), maxmillis);
        } catch (Exception ex) {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
            throw ex;
        }
    }

    /**
     * Asynchronně zpracováva spuštěný proces - vypisuje výstup do logu, čeká na exit code apod.
     *
     * @param process process
     * @return výsledek procesu - exit value
     * @throws IOException chyba
     */
    @Async
    public Future<Integer> manageProcess(final Process process) throws IOException {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = input.readLine()) != null) {
                logger.info("  " + line);
            }
        }

        // Čekáme na dokončení
        try {
            while (!process.waitFor(250, TimeUnit.MILLISECONDS)) {
                // nop
            }
        } catch (InterruptedException e) {
            // Nothing to do with this
            Thread.currentThread().interrupt();
        }

        // Získání chybového výstupu
        int exitValue = process.exitValue();
        if (exitValue != 0) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = input.readLine()) != null) {
                    sb.append(line + "\n");
                }
            }
            throw new SystemException("V externím procesu nastala chyba (" + sb.toString() + ")");
        }

        return new AsyncResult<>(exitValue);
    }

    /**
     * Čekání na dokončení procesu.
     *
     * @param future    výstup zpracování procesu
     * @param processId id procesu - pouze pro logovací účely
     * @param maxmillis jakou dobu max. čekat, pokud je 0, čeká se nekonečně dlouho
     */
    public void waitForProcess(final Future<Integer> future, final String processId, final long maxmillis) {
        long waitingMillis = 0;
        while (true) {

            if (future.isDone()) {
                break;
            }
            try {
                Thread.sleep(100);
                waitingMillis += 100;
            } catch (InterruptedException ex) {
                // Nothing to do with this
                Thread.currentThread().interrupt();
            }

            if (waitingMillis % 1000 == 0) {
                logger.info("Čeká se na dokončení procesu {} ({} s)", processId, waitingMillis / 1000);
            }

            if (maxmillis != 0 && waitingMillis > maxmillis) {
                throw new SystemException("Proces nebyl dokončen v požadovaný čas (" + (maxmillis / 1000) + " s)");
            }
        }
    }
}
