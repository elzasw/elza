package cz.tacr.elza.api;

import java.time.LocalDateTime;

/**
 * Soubor k DAO.
 *
 * @author Martin Šlapa
 * @since 06.12.2016
 */
public interface ArrDaoFile<D extends ArrDao, DFG extends ArrDaoFileGroup> {

    Integer getDaoFileId();

    void setDaoFileId(Integer daoFileId);

    D getDao();

    void setDao(D dao);

    DFG getDaoFileGroup();

    void setDaoFileGroup(DFG daoFileGroup);

    String getChecksum();

    void setChecksum(String checksum);

    ChecksumType getChecksumType();

    void setChecksumType(ChecksumType checksumType);

    LocalDateTime getCreated();

    void setCreated(LocalDateTime created);

    String getMimetype();

    void setMimetype(String mimetype);

    Long getSize();

    void setSize(Long size);

    Integer getImageHeight();

    void setImageHeight(Integer imageHeight);

    Integer getImageWidth();

    void setImageWidth(Integer imageWidth);

    UnitOfMeasure getSourceXDimesionUnit();

    void setSourceXDimesionUnit(UnitOfMeasure sourceXDimesionUnit);

    Double getSourceXDimesionValue();

    void setSourceXDimesionValue(Double sourceXDimesionValue);

    UnitOfMeasure getSourceYDimesionUnit();

    void setSourceYDimesionUnit(UnitOfMeasure sourceYDimesionUnit);

    Double getSourceYDimesionValue();

    void setSourceYDimesionValue(Double sourceYDimesionValue);

    String getDuration();

    void setDuration(String duration);

    String getCode();

    void setCode(String code);

    enum ChecksumType {

        MD5,
        SHA1,
        SHA256,
        SHA384,
        SHA512,

    }

}
