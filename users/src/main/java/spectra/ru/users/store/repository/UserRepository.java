package spectra.ru.users.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spectra.ru.users.store.entities.UserEntity;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    Stream<UserEntity> streamAllByNameOrEmail(String name, String email);

    Stream<UserEntity> streamAllBy();

    Stream<UserEntity> streamAllById(List<Long> ids);

}
