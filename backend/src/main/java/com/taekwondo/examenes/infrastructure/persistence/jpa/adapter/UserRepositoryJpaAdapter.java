package com.taekwondo.examenes.infrastructure.persistence.jpa.adapter;

import com.taekwondo.examenes.domain.model.User;
import com.taekwondo.examenes.domain.model.UserRole;
import com.taekwondo.examenes.domain.port.UserRepository;
import com.taekwondo.examenes.infrastructure.persistence.jpa.repository.UserSpringDataRepository;
import com.taekwondo.examenes.infrastructure.persistence.mapper.UserMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepositoryJpaAdapter implements UserRepository {

    private final UserSpringDataRepository springDataRepository;

    public UserRepositoryJpaAdapter(UserSpringDataRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public User save(User user) {
        return UserMapper.toDomain(springDataRepository.save(UserMapper.toJpa(user)));
    }

    @Override
    public Optional<User> findById(Long id) {
        return springDataRepository.findById(id).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return springDataRepository.findByUsername(username).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springDataRepository.findByEmail(email).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return springDataRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByRole(UserRole role) {
        return springDataRepository.existsByRole(role);
    }

    @Override
    public List<User> findAll() {
        return springDataRepository.findAll().stream()
                .map(UserMapper::toDomain)
                .toList();
    }
}
