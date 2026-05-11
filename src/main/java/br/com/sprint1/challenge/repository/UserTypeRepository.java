package br.com.sprint1.challenge.repository;

import br.com.sprint1.challenge.entity.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserTypeRepository extends JpaRepository<UserType, String> {
    Optional<UserType> findByType(String type);
}