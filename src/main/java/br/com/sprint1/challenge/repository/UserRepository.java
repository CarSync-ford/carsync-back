package br.com.sprint1.challenge.repository;

import br.com.sprint1.challenge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByCpf(String cpf);
}