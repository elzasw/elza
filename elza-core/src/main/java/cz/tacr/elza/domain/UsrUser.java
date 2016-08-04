package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

/**
 * Implementace {@link cz.tacr.elza.api.UsrUser}.
 *
 * @author Martin Šlapa
 * @since 11.04.2016
 */
@Entity(name = "usr_user")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class UsrUser implements cz.tacr.elza.api.UsrUser<ParParty>, Serializable {

    @Id
    @GeneratedValue
    private Integer userId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParParty.class)
    @JoinColumn(name = "partyId", nullable = false)
    private ParParty party;

    @Column(length = 250, nullable = false, unique = true)
    private String username;

    @Column(length = 64, nullable = false)
    private String password;

    @Column(nullable = false)
    private Boolean active;

    @Column(length = 250)
    private String description;

    /* Konstanty pro vazby a fieldy. */
    public static final String USER_ID = "userId";
    public static final String PARTY = "party";
    public static final String USERNAME = "username";
    public static final String DESCRIPTION = "description";
    public static final String ACTIVE = "active";

    @Override
    public Integer getUserId() {
        return userId;
    }

    @Override
    public void setUserId(final Integer userId) {
        this.userId = userId;
    }

    @Override
    public ParParty getParty() {
        return party;
    }

    @Override
    public void setParty(final ParParty party) {
        this.party = party;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(final String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(final String password) {
        this.password = password;
    }

    @Override
    public Boolean getActive() {
        return active;
    }

    @Override
    public void setActive(final Boolean active) {
        this.active = active;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }
}
