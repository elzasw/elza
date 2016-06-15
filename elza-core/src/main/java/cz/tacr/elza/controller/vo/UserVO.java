package cz.tacr.elza.controller.vo;

/**
 * Vo objekt uživatele, obsahuje informace o osobě.
 *
 * @author Pavel Stánek
 * @since 15.06.2016
 */
public class UserVO {
    /** Uživatelské jméno. */
    private String username;
    /** Identifikátor uživatele. */
    private Integer id;
    /** Je aktivní. */
    private boolean active;
    /** Popis. */
    private String description;
    /** Osoba. */
    private ParPartyVO party;

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public ParPartyVO getParty() {
        return party;
    }

    public void setParty(final ParPartyVO party) {
        this.party = party;
    }
}
