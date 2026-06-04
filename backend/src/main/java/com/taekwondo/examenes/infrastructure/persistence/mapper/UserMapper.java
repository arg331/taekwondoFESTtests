package com.taekwondo.examenes.infrastructure.persistence.mapper;

import com.taekwondo.examenes.domain.model.User;
import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.UserJpaEntity;

/**
 * Conversor entre User (dominio) y UserJpaEntity (persistencia).
 *
 * Más simple que QuestionMapper porque User no tiene relaciones con
 * otras entidades.
 */
public final class UserMapper {

    private UserMapper() {}

    public static UserJpaEntity toJpa(User user) {
        return new UserJpaEntity(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getDisplayName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }

    public static User toDomain(UserJpaEntity entity) {
        return User.reconstitute(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getDisplayName(),
                entity.getRole(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }
}
