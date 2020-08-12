package cz.tacr.elza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrDataCoordinates;


/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Repository
public interface DataCoordinatesRepository extends JpaRepository<ArrDataCoordinates, Integer> {

    @Query(value = "SELECT ST_AsKML(ST_setSRID(co.coordinates_value, 4326)) FROM arr_data_coordinates co WHERE co.data_id = :dataId", nativeQuery = true)
    String convertCoordinatesToKml(Integer dataId);

    @Query(value = "SELECT ST_AsGML(co.coordinates_value) FROM arr_data_coordinates co WHERE co.data_id = :dataId", nativeQuery = true)
    String convertCoordinatesToGml(Integer dataId);

    @Query(value = "SELECT ST_AsText(ST_GeomFromKML(:coordinates))", nativeQuery = true)
    String convertCoordinatesFromKml(String coordinates);

    @Query(value = "SELECT ST_AsText(ST_GeomFromGML(:coordinates))", nativeQuery = true)
    String convertCoordinatesFromGml(String coordinates);

}
