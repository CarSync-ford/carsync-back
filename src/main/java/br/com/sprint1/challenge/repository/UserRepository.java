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

    java.util.List<User> findAllByDeletedAtBefore(java.time.LocalDateTime cutoff);

    java.util.List<User> findAllByLastLoginBefore(java.time.LocalDateTime cutoff);

    @Modifying
    @Query("update User u set u.failedLoginAttempts = 0, u.lockedUntil = null where u.id = :id")
    void unlockUser(@Param("id") String id);

    @Modifying
    @Query("update User u set u.failedLoginAttempts = u.failedLoginAttempts + 1 where u.id = :id")
    void incrementFailedLoginAttempts(@Param("id") String id);

    @Modifying
    @Query("update User u set u.failedLoginAttempts = 0, u.lockedUntil = :lockedUntil where u.id = :id")
    void lockUser(@Param("id") String id, @Param("lockedUntil") java.time.LocalDateTime lockedUntil);

    @Modifying
    @Query("update User u set u.refreshToken = :refreshToken, u.refreshTokenExpiresAt = :expiresAt where u.id = :id")
    void updateRefreshToken(@Param("id") String id, @Param("refreshToken") String refreshToken, @Param("expiresAt") java.time.LocalDateTime expiresAt);

    @Modifying
    @Query("update User u set u.refreshToken = null, u.refreshTokenExpiresAt = null where u.id = :id")
    void revokeRefreshToken(@Param("id") String id);
}