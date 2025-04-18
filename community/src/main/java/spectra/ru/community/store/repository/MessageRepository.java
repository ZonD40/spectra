package spectra.ru.community.store.repository;

import spectra.ru.community.store.entities.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.stream.Stream;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    @Query(nativeQuery = true,
            value = """
            SELECT
            *
            FROM message
            WHERE
            (:spectorId IS NULL OR message.spector_id = :spectorId)
            AND (:parentMessageId IS NULL OR message.parent_message_id = :parentMessageId)
            """)
    Stream<MessageEntity> streamAllBySpectorIdOrParentMessageId(Long spectorId, Long parentMessageId);

}
