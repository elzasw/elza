package cz.tacr.elza.service.da;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.lang.Nullable;

import gov.loc.mets.v1_11.schema.AmdSecType;
import gov.loc.mets.v1_11.schema.FileGrpType;
import gov.loc.mets.v1_11.schema.FileType;
import gov.loc.mets.v1_11.schema.MdSecType;
import gov.loc.mets.v1_11.schema.MetsType;

/**
 * Finds the files of an AIP package where the package declares them.
 *
 * The rules of the package fix only two names: the root {@code METS.xml} and
 * {@code metadata/preservation/PACKAGE-INFO.xml}. Every other file - PREMIS, EAD, components -
 * is wherever the root METS points, under whatever name its producer chose. Nothing is
 * searched for by name: a package also carries files of the same name deeper inside (the METS
 * of the original SIP in a representation, payload files), and which of them a search finds
 * first depends on the file system.
 */
final class AipPackageFiles {

    static final String METS = "METS.xml";

    static final String PACKAGE_INFO = "metadata/preservation/PACKAGE-INFO.xml";

    /** Where a package_info batch, which has no METS, carries its PACKAGE-INFO.xml. */
    static final String PACKAGE_INFO_ALONE = "PACKAGE-INFO.xml";

    static final String MDTYPE_PREMIS = "PREMIS";

    private AipPackageFiles() {
    }

    /**
     * Resolves a path taken from the package - a zip entry or a METS reference - so that it
     * cannot point outside the directory the package was unpacked to.
     */
    static Path resolveInside(Path dir, String relative) {
        Path base = dir.toAbsolutePath().normalize();
        Path resolved = base.resolve(relative).normalize();
        if (!resolved.startsWith(base) || resolved.equals(base)) {
            throw AipProblemException.metadata("Balíček odkazuje na cestu mimo svou složku: " + relative,
                                               relative, null);
        }
        return resolved;
    }

    /**
     * @param unpacked the directory a stored package was unpacked to: the package directory
     *                 itself, or the directory holding it (a stored package keeps the directory
     *                 named by the code of the AIP)
     * @return the directory with the root METS.xml
     */
    static Path packageRoot(Path unpacked) throws IOException {
        if (Files.isRegularFile(unpacked.resolve(METS))) {
            return unpacked;
        }
        List<Path> dirs;
        try (Stream<Path> children = Files.list(unpacked)) {
            dirs = children.filter(Files::isDirectory).toList();
        }
        if (dirs.size() == 1) {
            return dirs.get(0);
        }
        throw AipProblemException.metadata("Balíček neobsahuje soubor " + METS);
    }

    static Path mets(Path root) {
        Path mets = root.resolve(METS);
        if (!Files.isRegularFile(mets)) {
            throw AipProblemException.metadata("Balíček neobsahuje soubor " + METS + contentsNote(root));
        }
        return mets;
    }

    static Path packageInfo(Path root) {
        for (String candidate : List.of(PACKAGE_INFO, PACKAGE_INFO_ALONE)) {
            Path packageInfo = root.resolve(candidate);
            if (Files.isRegularFile(packageInfo)) {
                return packageInfo;
            }
        }
        throw AipProblemException.metadata("Balíček neobsahuje soubor PACKAGE-INFO.xml" + contentsNote(root));
    }

    /** How many entries of a directory a message names before it says "and more". */
    private static final int LISTED_ENTRIES = 10;

    /**
     * What a directory holds, for a message about a file that is not there: the names of its
     * entries say what the DA sent instead, which the name of the missing file does not.
     */
    static String contentsNote(Path dir) {
        List<String> names;
        try (Stream<Path> children = Files.list(dir)) {
            names = children.map(p -> p.getFileName().toString() + (Files.isDirectory(p) ? "/" : ""))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return "";
        }
        if (names.isEmpty()) {
            return " (balíček je prázdný)";
        }
        String listed = String.join(", ", names.subList(0, Math.min(names.size(), LISTED_ENTRIES)));
        if (names.size() > LISTED_ENTRIES) {
            listed += " a dalších " + (names.size() - LISTED_ENTRIES);
        }
        return " (obsahuje: " + listed + ")";
    }

    /**
     * Brings a delivered package to the layout the processing reads: the root METS and the
     * metadata directly in the directory of the AIP. A DA may deliver the package as one zip
     * file in that directory, and the package may keep its own root directory (named by its
     * UUID, as the exchange format has it) inside - both are unwrapped here, so that the stored
     * package has one layout whatever the DA does. Anything else is left as it is and reported
     * by the file that is missing.
     */
    static void normalizeDelivered(Path aipDir) throws IOException {
        List<Path> children = children(aipDir);
        if (children.size() == 1 && Files.isRegularFile(children.get(0))
                && children.get(0).getFileName().toString().toLowerCase().endsWith(".zip")) {
            Path zip = children.get(0);
            Path unpacked = Files.createTempDirectory(aipDir, "unpacked");
            unzip(zip, unpacked);
            Files.delete(zip);
            moveChildrenUp(unpacked);
            children = children(aipDir);
        }
        if (children.size() == 1 && Files.isDirectory(children.get(0))
                && !Files.isRegularFile(aipDir.resolve(METS))
                && !Files.isRegularFile(aipDir.resolve(PACKAGE_INFO_ALONE))
                && !Files.isRegularFile(aipDir.resolve(PACKAGE_INFO))) {
            moveChildrenUp(children.get(0));
        }
    }

    private static List<Path> children(Path dir) throws IOException {
        try (Stream<Path> children = Files.list(dir)) {
            return children.toList();
        }
    }

    private static void unzip(Path zip, Path into) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path filePath = resolveInside(into, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zipInputStream, filePath);
                }
            }
        }
    }

    /** Moves the entries of a directory into its parent and removes the then empty directory. */
    private static void moveChildrenUp(Path dir) throws IOException {
        Path parent = dir.getParent();
        for (Path child : children(dir)) {
            Files.move(child, parent.resolve(child.getFileName()));
        }
        Files.delete(dir);
    }

    /**
     * @return the file the METS refers to; a reference to a file the package does not carry
     *         is an error of the package
     */
    static Path referenced(Path root, String href) {
        Path file = resolveInside(root, href);
        String decoded = decodedHref(href);
        if (!Files.isRegularFile(file) && decoded != null) {
            file = resolveInside(root, decoded);
        }
        if (!Files.isRegularFile(file)) {
            throw AipProblemException.metadata("Balíček neobsahuje soubor " + href + ", na který odkazuje METS.xml",
                                               href, null);
        }
        return file;
    }

    /**
     * A METS reference is a URL, so a producer may write it percent-encoded (a space as %20);
     * the package then carries the file under the decoded name.
     *
     * @return the decoded reference, or null when it has nothing encoded
     */
    @Nullable
    static String decodedHref(String href) {
        if (href.indexOf('%') < 0) {
            return null;
        }
        try {
            // '+' is a plus in a path, not a space as in a query
            return URLDecoder.decode(href.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** @return the paths of all files of the metadata sections of the METS (descriptive and administrative) */
    static List<String> metadataHrefs(MetsType mets) {
        List<MdSecType> sections = new ArrayList<>(mets.getDmdSec());
        for (AmdSecType amdSec : mets.getAmdSec()) {
            sections.addAll(administrativeSections(amdSec));
        }
        return hrefs(sections);
    }

    /** @return the paths of the PREMIS files of the METS, PACKAGE-INFO.xml included */
    static List<String> premisHrefs(MetsType mets) {
        List<MdSecType> premis = new ArrayList<>();
        for (AmdSecType amdSec : mets.getAmdSec()) {
            for (MdSecType mdSec : administrativeSections(amdSec)) {
                if (mdSec.getMdRef() != null && MDTYPE_PREMIS.equalsIgnoreCase(mdSec.getMdRef().getMDTYPE())) {
                    premis.add(mdSec);
                }
            }
        }
        return hrefs(premis);
    }

    /**
     * @param id the ID of a file or of a metadata section of the METS - the code of its DAO
     * @return the path of that file in the package, or null when the METS has no such ID
     */
    @Nullable
    static String hrefOf(MetsType mets, String id) {
        if (mets.getFileSec() != null) {
            String href = hrefOf(new ArrayList<>(mets.getFileSec().getFileGrp()), id);
            if (href != null) {
                return href;
            }
        }
        List<MdSecType> sections = new ArrayList<>(mets.getDmdSec());
        for (AmdSecType amdSec : mets.getAmdSec()) {
            sections.addAll(administrativeSections(amdSec));
        }
        for (MdSecType mdSec : sections) {
            if (id.equals(mdSec.getID()) && mdSec.getMdRef() != null) {
                return mdSec.getMdRef().getHref();
            }
        }
        return null;
    }

    @Nullable
    private static String hrefOf(List<FileGrpType> fileGrps, String id) {
        for (FileGrpType fileGrp : fileGrps) {
            for (FileType file : fileGrp.getFile()) {
                if (id.equals(file.getID()) && CollectionUtils.isNotEmpty(file.getFLocat())) {
                    return file.getFLocat().get(0).getHref();
                }
            }
            String href = hrefOf(fileGrp.getFileGrp(), id);
            if (href != null) {
                return href;
            }
        }
        return null;
    }

    private static List<MdSecType> administrativeSections(AmdSecType amdSec) {
        List<MdSecType> sections = new ArrayList<>();
        sections.addAll(amdSec.getTechMD());
        sections.addAll(amdSec.getRightsMD());
        sections.addAll(amdSec.getSourceMD());
        sections.addAll(amdSec.getDigiprovMD());
        return sections;
    }

    private static List<String> hrefs(List<MdSecType> sections) {
        return sections.stream()
                .filter(mdSec -> mdSec.getMdRef() != null && mdSec.getMdRef().getHref() != null)
                .map(mdSec -> mdSec.getMdRef().getHref())
                .distinct()
                .toList();
    }
}
