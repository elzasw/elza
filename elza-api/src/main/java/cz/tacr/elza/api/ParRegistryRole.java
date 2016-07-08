package cz.tacr.elza.api;

/**
 * Vazba mezi typem rejstříku a rolí entity.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 02.02.2016
 */
public interface ParRegistryRole<RT extends RegRegisterType, RRT extends ParRelationRoleType> {

    Integer getRegistryRoleId();


    void setRegistryRoleId(Integer registryRoleId);


    RT getRegisterType();


    void setRegisterType(RT registerType);


    RRT getRoleType();


    void setRoleType(RRT roleType);

}
