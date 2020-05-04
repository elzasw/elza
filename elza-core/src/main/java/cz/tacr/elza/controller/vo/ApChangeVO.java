package cz.tacr.elza.controller.vo;

import java.time.LocalDateTime;

public class ApChangeVO {

    /**
     * Identifikátor
     */
    private Integer id;

    /**
     * Časová značka změny
     */
    private LocalDateTime change;

    /**
     * Uživatel, který proveld změnu
     */
    private UserVO user;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getChange() {
        return change;
    }

    public void setChange(LocalDateTime change) {
        this.change = change;
    }

    public UserVO getUser() {
        return user;
    }

    public void setUser(UserVO user) {
        this.user = user;
    }
}
