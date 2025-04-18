package spectra.ru.community.store.repository;

import spectra.ru.community.store.entities.SpectorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.stream.Stream;

public interface SpectorRepository extends JpaRepository<SpectorEntity, Long> {

    @Query(nativeQuery = true,
            value = """
            SELECT
               *
            FROM spector
            WHERE
               (:id IS NULL OR spector.id = :id)
               AND (:name IS NULL OR spector.name = :name)
            """)
    Stream<SpectorEntity> streamAllByIdOrName(Long id, String name);

}
