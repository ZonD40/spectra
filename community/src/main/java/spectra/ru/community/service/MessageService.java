package spectra.ru.community.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import spectra.ru.community.api.dto.AnswerDto;
import spectra.ru.community.api.dto.message.MessageCreateDto;
import spectra.ru.community.api.dto.message.MessageResponseDto;
import spectra.ru.community.api.dto.message.MessageUpdateDto;
import spectra.ru.community.api.exceptions.BadRequestException;
import spectra.ru.community.api.exceptions.NotFoundExeption;
import spectra.ru.community.api.factories.MessageDtoFactory;
import spectra.ru.community.enums.SubscriberRole;
import spectra.ru.community.store.entities.MessageEntity;
import spectra.ru.community.store.entities.SpectorEntity;
import spectra.ru.community.store.entities.SpectorSubscriberEntity;
import spectra.ru.community.store.repository.MessageRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import spectra.ru.event.NewCommentEvent;
import spectra.ru.event.NewPostEvent;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Transactional
public class MessageService {

    MessageRepository messageRepository;

    MessageDtoFactory messageDtoFactory;

    SpectorService spectorService;

    KafkaTemplate<String, NewPostEvent> kafkaNewPostTemplate;

    KafkaTemplate<String, NewCommentEvent> kafkaNewCommentTemplate;

    public MessageResponseDto create(MessageCreateDto messageCreateDto, Long userId) {

        SpectorEntity spectorEntity = spectorService.getSpectorByIdOrThrowException(messageCreateDto.getSpectorId());



        Long parentMessageId = messageCreateDto.getParentMessageId();

        MessageEntity parentMessageEntity = null;

        if (parentMessageId != null) {
            parentMessageEntity = messageRepository.findById(parentMessageId).orElseThrow(() ->
                    new NotFoundExeption(String.format("Parent message with id \"%s\" doesn't exist", parentMessageId))
            );

            if (!Objects.equals(parentMessageEntity.getSpector().getId(), messageCreateDto.getSpectorId())) {
                throw new BadRequestException("There is no parent message with this id in this spector");
            }
        } else if (!spectorService.userHaveRoles(spectorEntity, userId, SubscriberRole.OWNER)) {
            throw new BadRequestException(
                    String.format("User with id \"%s\" is not the owner of the spector with id \"%s\"", userId, spectorEntity.getId())
            );
        }


        MessageEntity messageEntity = messageRepository.save(MessageEntity
                .builder()
                .body(messageCreateDto.getBody())
                .spector(spectorEntity)
                .parentMessage(parentMessageEntity)
                .userId(userId)
                .build());

        String messageLink = "http://localhost:8080/api/messages/%s";

        if (parentMessageEntity == null) {
            List<Long> subscriberIdList = spectorEntity
                    .getSubscriberList()
                    .stream()
                    .map(SpectorSubscriberEntity::getUserId)
                    .toList();

            NewPostEvent newPostEvent = NewPostEvent.builder()
                    .subscribersIdList(subscriberIdList)
                    .spectorName(spectorEntity.getName())
                    .postLink(
                            String.format(messageLink, messageEntity.getId())
                    )
                    .build();

            kafkaNewPostTemplate.send("new-post", newPostEvent);
        } else if (parentMessageEntity.getUserId().equals(userId)) {
            NewCommentEvent newCommentEvent = NewCommentEvent.builder()
                    .messageLink(
                            String.format(messageLink, parentMessageEntity.getId())
                    )
                    .commentLink(
                            String.format(messageLink, messageEntity.getId())
                    )
                    .spectorName(spectorEntity.getName())
                    .userId(parentMessageEntity.getUserId())
                    .build();

            kafkaNewCommentTemplate.send("new-comment", newCommentEvent);
        }


        return messageDtoFactory.makeMessageDto(messageEntity);
    }

    public List<MessageResponseDto> find(Long spectorId, Long parentId) {

        Stream<MessageEntity> messageEntityStream = messageRepository.streamAllBySpectorIdOrParentMessageId(
                spectorId,
                parentId
            );

        return messageEntityStream.map(messageDtoFactory::makeMessageDto).toList();
    }

    @Cacheable(value = "messages", key = "#id")
    public MessageResponseDto findById(Long id) {
        MessageEntity messageEntity = messageRepository.findById(id).orElseThrow(() ->
                new BadRequestException(
                        String.format("Message with id \"%s\" doesn't exist", id)
                )
        );

        return messageDtoFactory.makeMessageDto(messageEntity);
    }

    @CacheEvict(value = "messages", key = "#id")
    public AnswerDto delete(Long id, Long userId) {

        MessageEntity messageEntity = messageRepository
                .findById(id)
                .orElseThrow(() ->
                        new NotFoundExeption(
                                String.format("Message with id \"%s\" doesn't exist", id)
                        )
                );

        if (messageEntity.getUserId().equals(userId)) {
            throw new BadRequestException("Only creator can delete a message");
        }

        messageRepository.delete(messageEntity);

        return AnswerDto.makeDefault(true);
    }

    @CachePut(value = "messages", key = "#id")
    public MessageResponseDto update(Long id, MessageUpdateDto messageUpdateDto, Long userId) {

        MessageEntity messageEntity = messageRepository.findById(id).orElseThrow(() ->
                new NotFoundExeption(
                        String.format("Message with id \"%s\" doesn't exist", id)
                )
        );

        if (messageEntity.getUserId().equals(userId)) {
            throw new BadRequestException("Only creator can edit a message");
        }

        messageEntity.setBody(messageUpdateDto.getBody());

        return messageDtoFactory.makeMessageDto(messageRepository.save(messageEntity));
    }

}
