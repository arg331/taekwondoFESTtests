package com.taekwondo.examenes.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByRole(UserRole role);

    /**
     * Gestión de usuarios (RF-37, RNF-09): text, ya en minúsculas y sin tildes, dentro del nombre visible (también
     * sin tildes), el usuario o el email; vacío = todos. El texto llega con los comodines de LIKE escapados.
     */
    @Query("""
            select u from User u
            where :text = ''
               or cast(function('translate', lower(u.displayName), 'áàâäãéèêëíìîïóòôöõúùûüñç', 'aaaaaeeeeiiiiooooouuuunc')
                       as String) like concat('%', :text, '%') escape '\\'
               or lower(u.username) like concat('%', :text, '%') escape '\\'
               or lower(coalesce(u.email, '')) like concat('%', :text, '%') escape '\\'
            """)
    Page<User> search(String text, Pageable pageable);

    /** Miembros de un club, por nombre visible. */
    @Query("select u from User u where u.club.id = :clubId order by lower(u.displayName)")
    List<User> findAllByClubId(Long clubId);

    /**
     * Usuarios cuyo usuario o nombre visible contiene el texto, que no son de ese club;
     * con onlyWithoutClub, solo los que no tienen ninguno.
     */
    @Query("""
            select u from User u
            where (lower(u.username) like lower(concat('%', :text, '%'))
                   or lower(u.displayName) like lower(concat('%', :text, '%')))
              and (u.club is null or (:onlyWithoutClub = false and u.club.id <> :clubId))
            order by lower(u.displayName)
            """)
    List<User> searchClubCandidates(String text, Long clubId, boolean onlyWithoutClub, Pageable page);

    /** Usuarios activos con alguno de esos roles cuyo usuario o nombre visible contiene el texto. */
    @Query("""
            select u from User u
            where u.active = true and u.role in :roles
              and (lower(u.username) like lower(concat('%', :text, '%'))
                   or lower(u.displayName) like lower(concat('%', :text, '%')))
            order by lower(u.displayName)
            """)
    List<User> searchActiveByRoles(String text, Collection<UserRole> roles, Pageable page);
}
