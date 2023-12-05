package cz.tacr.elza.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.geotools.kml.KML;
import org.geotools.kml.KMLConfiguration;
import org.geotools.xsd.Parser;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.repository.DataCoordinatesRepository;
import cz.tacr.elza.security.UserDetail;

/**
 * Datová třída pro přístupové body.
 *
 * @since 24.07.2018
 */
@Service
public class AccessPointDataService {

    private static final Logger logger = LoggerFactory.getLogger(AccessPointDataService.class);

    private final EntityManager em;
    private final UserService userService;
    private final ApChangeRepository apChangeRepository;
    private final DataCoordinatesRepository dataCoordinatesRepository;

    @Autowired
    public AccessPointDataService(final EntityManager em,
                                  final UserService userService,
                                  final ApChangeRepository apChangeRepository,
                                  final DataCoordinatesRepository dataCoordinatesRepository) {
        this.em = em;
        this.userService = userService;
        this.apChangeRepository = apChangeRepository;
        this.dataCoordinatesRepository = dataCoordinatesRepository;
    }

    /**
     * Sestavení celého jména z jména a doplňku.
     *
     * @param name       jméno
     * @param complement doplněk
     * @return celé jméno
     */
    @Nullable
    public static String generateFullName(@Nullable final String name, @Nullable final String complement) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        StringBuilder sb = new StringBuilder(name.trim());
        if (StringUtils.isNotEmpty(complement)) {
            sb.append(" (").append(complement.trim()).append(')');
        }
        return sb.toString();
    }

    /**
     * Vytvoření změny daného typu.
     *
     * @param type typ změny
     * @return změna
     */
    public ApChange createChange(@Nullable final ApChange.Type type) {
        return createChange(type, null);
    }

    /**
     * Vytvoření změny s externím systémem.
     *
     * @param type           typ změny
     * @param externalSystem externí systém
     * @return změna
     */
    public ApChange createChange(@Nullable final ApChange.Type type, @Nullable ApExternalSystem externalSystem) {
        ApChange change = new ApChange();
        UserDetail userDetail = userService.getLoggedUserDetail();
        change.setChangeDate(OffsetDateTime.now());

        if (userDetail != null && userDetail.getId() != null) {
            UsrUser user = em.getReference(UsrUser.class, userDetail.getId());
            change.setUser(user);
        }

        change.setType(type);
        change.setExternalSystem(externalSystem);

        return apChangeRepository.save(change);
    }

    public String convertCoordinatesFromKml(InputStream inputStream) {
        try {
            Parser parser = new Parser(new KMLConfiguration());
            SimpleFeature simpleFeature = (SimpleFeature) parser.parse(inputStream);
            for (Property property : simpleFeature.getValue()) {
                // if file created by https://www.freemaptools.com/kml-file-creator.htm
                if (property.getType().getName().getURI().equals(KML.Geometry.getLocalPart())) {
                    return property.getValue().toString();
                }
                // if file created by https://earth.google.com/
                if (property.getType().getName().getURI().equals(KML.Feature.getLocalPart())) {
                    List<SimpleFeature> simpleFeatures = (List<SimpleFeature>) property.getValue();
                    for (SimpleFeature sf : simpleFeatures) {
                        for (Property p : sf.getValue()) {
                            if (p.getType().getName().getURI().equals(KML.Geometry.getLocalPart())) {
                                return p.getValue().toString();
                            }
                        }
                    }
                }
            }
        } catch (IOException | SAXException | ParserConfigurationException e) {
            logger.error("", e);
            throw new IllegalStateException("Chyba při importu souřadnic ze souboru", e);
        }
        return null;
    }

    public String convertCoordinatesFromGml(String coordinates) {
        return dataCoordinatesRepository.convertCoordinatesFromGml(coordinates);
    }

    public String convertCoordinatesToKml(Integer dataId) {
        return dataCoordinatesRepository.convertCoordinatesToKml(dataId);
    }

    public String convertCoordinatesToGml(Integer dataId) {
        return dataCoordinatesRepository.convertCoordinatesToGml(dataId);
    }

//    public String convertCoordinatesToEWKT(byte[] coordinates) { //TODO asi nadbytečné, nikde se nevolá
//        return dataCoordinatesRepository.convertCoordinatesToEWKT(coordinates);
//    }

    public byte[] convertGeometryToWKB(org.locationtech.jts.geom.Geometry geometry) {
        return dataCoordinatesRepository.convertGeometryToWKB(geometry);
    }

}
