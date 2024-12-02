package cz.tacr.elza.domain.factory;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataBit;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDate;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.domain.table.ElzaRow;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DataBitRepository;
import cz.tacr.elza.repository.DataCoordinatesRepository;
import cz.tacr.elza.repository.DataDateRepository;
import cz.tacr.elza.repository.DataDecimalRepository;
import cz.tacr.elza.repository.DataFileRefRepository;
import cz.tacr.elza.repository.DataIntegerRepository;
import cz.tacr.elza.repository.DataJsonTableRepository;
import cz.tacr.elza.repository.DataNullRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.DataStringRepository;
import cz.tacr.elza.repository.DataStructureRefRepository;
import cz.tacr.elza.repository.DataTextRepository;
import cz.tacr.elza.repository.DataUnitdateRepository;
import cz.tacr.elza.repository.DataUnitidRepository;
import cz.tacr.elza.repository.DataUriRefRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.NodeRepository;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Factory pro vytváření a manipulaci s prvkama popisu a jejich hodnotama.
 *
 */
@Component
public class DescItemFactory implements InitializingBean {

    public final static String ELZA_NODE = "elza-node";

    /**
     * Povolené protokoly
     */
    private final String PATTERN_PROTOCOL = "^(https?|elza-node)://.*";

    /**
     * Povolenoné zkratky
     */
    public final String PATTERN_UNIT_DATA = "(C|Y|YM|D|DT|)";

    /**
     * Oddělovač intervalu datace
     */
    public final String INTERVAL_DELIMITER_UNIT_DATA = "-";

    private LinkedHashMap<Class<? extends ArrData>, JpaRepository<? extends ArrData,Integer> > mapRepository;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private DataCoordinatesRepository dataCoordinatesRepository;

    @Autowired
    private DataIntegerRepository dataIntegerRepository;

    @Autowired
    private DataRecordRefRepository dataRecordRefRepository;

    @Autowired
    private DataStringRepository dataStringRepository;

    @Autowired
    private DataTextRepository dataTextRepository;

    @Autowired
    private DataUnitdateRepository dataUnitdateRepository;

    @Autowired
    private DataUnitidRepository dataUnitidRepository;

    @Autowired
    private DataDecimalRepository dataDecimalRepository;

    @Autowired
    private DataStructureRefRepository dataStructureRefRepository;

    @Autowired
    private DataFileRefRepository dataFileRefRepository;

    @Autowired
    private DataNullRepository dataNullRepository;

    @Autowired
    private DataJsonTableRepository dataJsonTableRepository;

    @Autowired
    private DataUriRefRepository dataUriRefRepository;

    @Autowired
    private DataDateRepository dataDateRepository;

    @Autowired
    private DataBitRepository dataBitRepository;

    @Autowired
    private NodeRepository nodeRepository;

    public DescItemFactory() {
    }

    public DescItemRepository getDescItemRepository() {
        return descItemRepository;
    }

    @Override
    public void afterPropertiesSet() {
        initMapRepository();
    }

    /**
     * Inicializace mapy pro všechny repository.
     */
    private void initMapRepository() {
        mapRepository = new LinkedHashMap<>();
        mapRepository.put(ArrDataCoordinates.class, dataCoordinatesRepository);
        mapRepository.put(ArrDataInteger.class, dataIntegerRepository);
        mapRepository.put(ArrDataRecordRef.class, dataRecordRefRepository);
        mapRepository.put(ArrDataString.class, dataStringRepository);
        mapRepository.put(ArrDataText.class, dataTextRepository);
        mapRepository.put(ArrDataUnitdate.class, dataUnitdateRepository);
        mapRepository.put(ArrDataUnitid.class, dataUnitidRepository);
        mapRepository.put(ArrDataDecimal.class, dataDecimalRepository);
        mapRepository.put(ArrDataStructureRef.class, dataStructureRefRepository);
        mapRepository.put(ArrDataFileRef.class, dataFileRefRepository);
        mapRepository.put(ArrDataNull.class, dataNullRepository);
        mapRepository.put(ArrDataJsonTable.class, dataJsonTableRepository);
        mapRepository.put(ArrDataDate.class, dataDateRepository);
        mapRepository.put(ArrDataUriRef.class, dataUriRefRepository);
        mapRepository.put(ArrDataBit.class, dataBitRepository);
    }

	/**
	 * Save data
	 *
	 * @param data
	 *            Data to be saved. Data can be null
	 * @return
	 */
	public ArrData saveData(RulItemType itemType, ArrData data) {
		// null values are allowed and ignored
		if (data == null) {
			return null;
		}
		// Check data
		if (data instanceof ArrDataJsonTable) {
			checkJsonTableData(((ArrDataJsonTable) data).getValue(), (List<ElzaColumn>) itemType.getViewDefinition());
		}

        if(data instanceof ArrDataUriRef) {
            ArrDataUriRef dataTemp = (ArrDataUriRef) data;
            String uriRefValue = dataTemp.getUriRefValue();
            if(StringUtils.isEmpty(uriRefValue)) {
                throw new IllegalArgumentException("Nebyl zadán odkaz, nebo je odkaz prázdný");
            }
            if (!uriRefValue.matches(PATTERN_PROTOCOL)) {
                throw new BusinessException("Zadaný odkaz URI není platný, hodnota: " + uriRefValue, BaseCode.INVALID_URI)
                    .set("uri", uriRefValue);
            }
            URI tempUri = URI.create(uriRefValue).normalize();
            dataTemp.setSchema(tempUri.getScheme());

            if(!dataTemp.isDeletingProcess() && dataTemp.getSchema().equals(ELZA_NODE)) {
                ArrNode node = nodeRepository.findOneByUuid(tempUri.getAuthority()); //hledání podle UUID
                if(node != null) {
                    dataTemp.setArrNode(node);
                }
            }
        }

		// Set data type
		// dataType is not set when object is received as JSON from client
		if (data.getDataType() == null) {
			data.setDataType(itemType.getDataType());
		}

		// Get repository
		JpaRepository dataRepos = mapRepository.get(data.getClass());
		if (dataRepos == null) {
			throw new NotImplementedException("Nebyla namapována repozitory pro datový typ: " + data.getClass());
		}
		return (ArrData) dataRepos.save(data);
	}

	/**
	 * Save data as new
	 *
	 * @param itemType
	 * @param srcData
	 *            source data object. Can be null
	 * @return
	 */
	public ArrData saveDataAsNew(RulItemType itemType, ArrData srcData) {
		if (srcData == null) {
			return null;
		}

		// dataType cannot be null
		srcData.setDataType(itemType.getDataType());

		ArrData dataNew = srcData.makeCopy();

		return saveData(itemType, dataNew);
	}

    /**
     * Uloží hodnotu atributu i s daty.
     *
     * @param descItem         hodnota atributu
     * @param createNewVersion vytvořit novou verzi?
     *                         true - vytvoří novou hodnoty atributu
     *                         false - načte původní hodnotu a upraví jí podle nové
     * @return uložená hodnota atributu
     */
    public ArrDescItem saveItemVersionWithData(final ArrDescItem descItem, final Boolean createNewVersion) {
        ArrData data = HibernateUtils.unproxy(descItem.getData());
        ArrData savedData;

        if (data != null) {
            if (data instanceof ArrDataJsonTable) {
                checkJsonTableData(((ArrDataJsonTable) data).getValue(), (List<ElzaColumn>) descItem.getItemType().getViewDefinition());
            }

            if(data instanceof ArrDataUriRef) {
                ArrDataUriRef dataTemp = (ArrDataUriRef) data;
                String uriRefValue = dataTemp.getUriRefValue();
                if (StringUtils.isEmpty(uriRefValue)) {
                    throw new IllegalArgumentException("Nebyl zadán odkaz, nebo je odkaz prázdný");
                }
                if (!uriRefValue.matches(PATTERN_PROTOCOL)) {
                    throw new BusinessException("Zadaný odkaz URI není platný, hodnota: " + uriRefValue, BaseCode.INVALID_URI)
                        .set("uri", uriRefValue);
                }
                URI tempUri = URI.create(uriRefValue).normalize();
                dataTemp.setDataType(descItem.getItemType().getDataType());
                if (StringUtils.isEmpty(tempUri.getScheme())) {
                    throw new BusinessException("Nebylo zadáno schéma v souladu s RFC2396, hodnota: " + uriRefValue, BaseCode.PROPERTY_IS_INVALID)
                        .set("uri", uriRefValue);
                }
                dataTemp.setSchema(tempUri.getScheme());

                if(dataTemp.getSchema().equals(ELZA_NODE)) {
                    ArrNode node = nodeRepository.findOneByUuid(tempUri.getAuthority()); //hledání podle UUID
                    if(node != null) {
                        dataTemp.setArrNode(node);
                    }
                }
            }

            ArrData dataNew;
            if (createNewVersion) {
                dataNew = saveDataAsNew(descItem.getItemType(), data);
                descItem.setData(dataNew);
            } else {
                dataNew = HibernateUtils.unproxy(descItem.getData());
            }

            savedData = saveData(descItem.getItemType(), dataNew);
        } else {
        	savedData = null;
        }
        descItem.setData(savedData);

        return descItemRepository.save(descItem);
    }

    /**
     * Kontrola sloupců v JSON tabulce.
     *
     * @param table   kontrolovaná tabulka
     * @param columns seznam definicí sloupců
     */
    static public void checkJsonTableData(@NotNull final ElzaTable table,
                                   @NotEmpty final List<ElzaColumn> columns) {
        Map<String, ElzaColumn.DataType> typeMap = columns.stream().collect(Collectors.toMap(ElzaColumn::getCode, ElzaColumn::getDataType));
        for (ElzaRow row : table.getRows()) {
            for (Map.Entry<String, String> entry : row.getValues().entrySet()) {
                ElzaColumn.DataType dataType = typeMap.get(entry.getKey());
                if (dataType == null) {
                    throw new BusinessException("Sloupec s kódem '" + entry.getKey() +  "' neexistuje v definici tabulky", BaseCode.PROPERTY_IS_INVALID)
                    .set("property", entry.getKey());
                }

                switch (dataType) {
                    case INTEGER:
                        try {
                            Integer.parseInt(entry.getValue());
                        } catch (NumberFormatException e) {
                            throw new BusinessException("Neplatný vstup: Hodnota sloupce '" + entry.getKey() + "' musí být celé číslo", e,
                                    BaseCode.PROPERTY_IS_INVALID)
                            .set("property", entry.getKey());
                        }
                        break;

                    case TEXT:
                        if (entry.getValue() == null) {
                            throw new BusinessException("Neplatný vstup: Hodnota sloupce '" + entry.getKey() + "' nesmí být null",
                                    BaseCode.PROPERTY_IS_INVALID)
                            .set("property", entry.getKey());
                        }
                        break;

                    default:
                        throw new BusinessException("Neznámý typ sloupce '" + dataType.name() + "' ve validaci JSON tabulky",
                                BaseCode.PROPERTY_IS_INVALID)
                        .set("property", dataType.name());
                }
            }
        }
    }
}
