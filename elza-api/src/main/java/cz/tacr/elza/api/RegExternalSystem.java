package cz.tacr.elza.api;

/**
 * Externí systémy pro rejstříky/osoby.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 23. 11. 2016
 */
public interface RegExternalSystem extends SysExternalSystem {

    RegExternalSystemType getType();

    void setType(RegExternalSystemType type);
}
