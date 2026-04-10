package cz.tacr.elza.packageimport.xml;

import java.io.IOException;
import java.util.List;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "node-search-settings", namespace = "node-search-settings")
public class SettingNodeSearch extends Setting {
	
	private static final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
	
    @XmlElement(name = "pinned-filter", type = PresetFilter.class, namespace = "node-search-settings")
    protected List<PresetFilter> options;
    
    public SettingNodeSearch() {
        super(UISettings.SettingsType.SEARCH_NODE_FILTERS.toString(),
                // bez vazby na konkretni typ entity
                null);
    }
    
	public List<PresetFilter> getOptions() {
		return options;
	}
	
	public void setOptions(List<PresetFilter> options) {
		this.options = options;
	}

    /**
     * Single menu option
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "pinned-filter", namespace = "node-search-settings")
    public static class PresetFilter {

		@XmlAttribute(name = "item-type", required = true)
        String itemType;
    	
        @XmlAttribute(name = "item-spec", required = false)
        String itemSpec;
        
        @XmlAttribute(name = "operation", required = false)
        String operation;

        @XmlAttribute(name = "name", required = false)
        String name;

		@XmlAttribute(name = "fixed-field", required = false)
        boolean fixedField = false;

		public String getItemType() {
			return itemType;
		}

		public void setItemType(String itemType) {
			this.itemType = itemType;
		}

		public String getItemSpec() {
			return itemSpec;
		}

		public void setItemSpec(String itemSpec) {
			this.itemSpec = itemSpec;
		}

		public String getOperation() {
			return operation;
		}

		public void setOperation(String operation) {
			this.operation = operation;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Boolean getFixedField() {
			return fixedField;
		}

		public void setFixedField(Boolean fixedField) {
			this.fixedField = fixedField;
		}
    }

	@Override
	void store(UISettings uiSettings) {
        try {
            uiSettings.setValue(objectMapper.writeValueAsString(this));
        } catch (JsonProcessingException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }		
	}

    public static SettingNodeSearch newInstance(UISettings uis) {
    	SettingNodeSearch settsm = new SettingNodeSearch();
        try {
            settsm.setOptions(objectMapper.readValue(uis.getValue(), SettingNodeSearch.class).getOptions());;
        } catch (IOException e) {
            throw new SystemException(e.getMessage(), e, BaseCode.JSON_PARSE);
        }
        return settsm;
    }
}
