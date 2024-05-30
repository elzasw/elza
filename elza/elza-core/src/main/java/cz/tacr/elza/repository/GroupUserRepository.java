package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrGroupUser;
import cz.tacr.elza.domain.UsrUser;

/**
 * Respozitory pro {@link UsrGroupUser}.
 *
 */
@Repository
public interface GroupUserRepository extends JpaRepository<UsrGroupUser, Integer> {

    void deleteByGroup(UsrGroup group);

    List<UsrGroupUser> findByGroup(UsrGroup group);

    /**
     * Return collection of relations between group and user
     * 
     * Note: Due to the bug in the data there might be multiple relations
     *       for same user and group
     * @param group
     * @param user
     * @return
     */
    List<UsrGroupUser> findByGroupAndUser(UsrGroup group, UsrUser user);

    /**
     * Return list of all user membership in groups
     * 
     * @param user
     * @return
     */
    List<UsrGroupUser> findByUser(UsrUser user);
}
