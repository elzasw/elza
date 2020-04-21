package cz.tacr.elza.dao.bo.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DaoConfig {

	public static final String FILE_IDENTIFIER_ATTR_NAME = "identifier";
	public static final String FILE_MIME_TYPE_ATTR_NAME = "mimeType";
	public static final String FILE_CREATED_ATTR_NAME = "created";
	public static final String FILE_SIZE_ATTR_NAME = "size";

	private List<Map<String, Object>> fileAttributes = new ArrayList<>();

	private String didIdentifier;

	private String label;

    private Map<String, String> attributes = new HashMap<>();


	public List<Map<String, Object>> getFileAttributes() {
		return fileAttributes;
	}

	public void setFileAttributes(List<Map<String, Object>> fileAttributes) {
		this.fileAttributes = fileAttributes;
	}

	public String getDidIdentifier() {
		return didIdentifier;
	}

	public void setDidIdentifier(String didIdentifier) {
		this.didIdentifier = didIdentifier;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
}