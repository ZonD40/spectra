package spectra.ru.community.service;

import org.springframework.kafka.core.KafkaTemplate;
import spectra.ru.community.api.dto.AnswerDto;
import spectra.ru.community.api.dto.reaction.ReactionResponseDto;
import spectra.ru.community.api.exceptions.BadRequestException;
import spectra.ru.community.api.exceptions.NotFoundExeption;
import spectra.ru.community.api.factories.ReactionDtoFactory;
import spectra.ru.community.enums.ReactionType;
import spectra.ru.community.store.entities.MessageEntity;
import spectra.ru.community.store.entities.ReactionEntity;
import spectra.ru.community.store.repository.MessageRepository;
import spectra.ru.community.store.repository.ReactionRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import spectra.ru.event.NewReactionEvent;

import java.util.List;
import java.util.stream.Stream;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Transactional
public class ReactionService {

    ReactionRepository reactionRepository;

    MessageRepository messageRepository;

    ReactionDtoFactory reactionDtoFactory;

    KafkaTemplate<String, NewReactionEvent> kafkaNewReactionTemplate;

    public ReactionResponseDto create(Long messageId, ReactionType reactionType, Long userId) {

        MessageEntity messageEntity = messageRepository.findById(messageId).orElseThrow(() ->
                new BadRequestException(String.format("Message with id \"%s\" doesn't exist", messageId))
        );

        ReactionEntity reactionEntity = reactionRepository.save(ReactionEntity
                .builder()
                .type(reactionType)
                .message(messageEntity)
                .userId(userId)
                .build());

        NewReactionEvent newReactionEvent = NewReactionEvent.builder()
                .reactionType(reactionType.toString())
                .userId(messageEntity.getUserId())
                .messageLink(String.format("http://localhost:8080/api/messages/%s", messageId))
                .reactionUserId(userId)
                .build();

        kafkaNewReactionTemplate.send("new-reaction", newReactionEvent);

        return reactionDtoFactory.makeReactionDto(reactionEntity);
    }

    public List<ReactionResponseDto> find(Long messageId) {

        MessageEntity messageEntity = messageRepository.findById(messageId).orElseThrow(() ->
                new NotFoundExeption(
                        String.format("Message with id \"%s\" doesn't exist", messageId)
                )
        );

        Stream<ReactionEntity> reactionEntityStream = reactionRepository.streamAllByMessage(messageEntity);

        return reactionEntityStream.map(reactionDtoFactory::makeReactionDto).toList();
    }

    public AnswerDto delete(Long messageId, Long id, Long userId) {

        if (!messageRepository.existsById(messageId)) {
            throw new NotFoundExeption(
                    String.format("Message with id \"%s\" doesn't exist", messageId)
            );
        }

        ReactionEntity reactionEntity = reactionRepository
                .findById(id)
                .orElseThrow(() ->
                        new NotFoundExeption(
                                String.format("Reaction with id \"%s\" doesn't exist", id)
                        )
                );

        if (!reactionEntity.getUserId().equals(userId)) {
            throw new BadRequestException("Only creator can delete a reaction");
        }

        reactionRepository.deleteById(id);

        return AnswerDto.makeDefault(true);
    }


}
