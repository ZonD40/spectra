package spectra.ru.community.store.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import spectra.ru.community.store.entities.SpectorEntity;
import spectra.ru.community.store.entities.SpectorSubscriberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpectorSubscriberRepository  extends JpaRepository<SpectorSubscriberEntity, Long> {

    Optional<SpectorSubscriberEntity> findBySpectorAndUserId(SpectorEntity spectorEntity, Long userId);

    @Modifying
    @Transactional
    @Query(nativeQuery = true,
            value = """
            DELETE
            FROM spector_subscribers
            WHERE
                spector_subscribers.user_id = :userId
            """)
    void removeSubscriberFromAll(Long userId);

}
