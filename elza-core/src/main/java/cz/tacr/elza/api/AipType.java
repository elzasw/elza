package cz.tacr.elza.api;


public enum AipType {

    PACKAGE_INFO("package_info"),

    ARCHDESC("archdesc"),

    METADATA_BASE("metadata_base"),

    AIP_BASE("aip_base"),

    AIP_RAW("aip_raw");

    private final String value;

    AipType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }


}
