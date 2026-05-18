package br.com.sprint1.challenge.repository;

import br.com.sprint1.challenge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByCpf(String cpf);
    Optional<User> findByEmail(String email);
    @Modifying
    @Query("update User u set u.lastLogin = CURRENT_TIMESTAMP where u.id = :id")
    void updateLastLoginById(@Param("id") String id);

    boolean existsByEmail(String email);

}