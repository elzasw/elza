package cz.tacr.elza.repository;

import org.locationtech.jts.geom.Geometry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDataCoordinates;


/**
 * Coordinates repository
 */
@Repository
public interface DataCoordinatesRepository extends JpaRepository<ArrDataCoordinates, Integer>,
        DataCoordinatesRepositoryCustom {

    @Query(value = "SELECT ST_AsKML(ST_setSRID(co.value, 4326)) FROM arr_data_coordinates co WHERE co.data_id = :dataId", nativeQuery = true)
    String convertCoordinatesToKml(Integer dataId);

    @Query(value = "SELECT ST_AsGML(co.value) FROM arr_data_coordinates co WHERE co.data_id = :dataId", nativeQuery = true)
    String convertCoordinatesToGml(Integer dataId);

    @Query(value = "SELECT ST_AsText(ST_GeomFromKML(:coordinates))", nativeQuery = true)
    String convertCoordinatesFromKml(String coordinates);

    @Query(value = "SELECT ST_AsText(ST_GeomFromGML(:coordinates))", nativeQuery = true)
    String convertCoordinatesFromGml(String coordinates);

    @Query(value = "SELECT ST_AsBinary(:geometry)", nativeQuery = true)
    byte[] convertGeometryToWKB(Geometry geometry);

}
