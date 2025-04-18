package spectra.ru.community.service;

import spectra.ru.community.api.dto.AnswerDto;
import spectra.ru.community.api.dto.internal.UserInternalResponseDto;
import spectra.ru.community.api.dto.spector.SpectorCreateUpdateDto;
import spectra.ru.community.api.dto.spector.SpectorResponseDto;
import spectra.ru.community.api.exceptions.BadRequestException;
import spectra.ru.community.api.exceptions.NotFoundExeption;
import spectra.ru.community.api.factories.SpectorDtoFactory;
import spectra.ru.community.enums.SubscriberRole;
import spectra.ru.community.store.entities.SpectorEntity;
import spectra.ru.community.store.entities.SpectorSubscriberEntity;
import spectra.ru.community.store.repository.SpectorRepository;
import spectra.ru.community.store.repository.SpectorSubscriberRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Transactional
public class SpectorService {

    SpectorRepository spectorRepository;

    SpectorSubscriberRepository spectorSubscriberRepository;

    SpectorDtoFactory spectorDtoFactory;

    UserService userService;

    public SpectorResponseDto create(SpectorCreateUpdateDto spectorCreateUpdateDto, Long userId) {

        if (spectorCreateUpdateDto.getName() == null || spectorCreateUpdateDto.getName().isBlank()) {
            throw new BadRequestException("Property \"name\" must be specified");
        }

        SpectorEntity spectorEntity = SpectorEntity
                .builder()
                .name(spectorCreateUpdateDto.getName())
                .description(spectorCreateUpdateDto.getDescription() != null ? spectorCreateUpdateDto.getDescription() : "")
                .build();

        spectorEntity.getSubscriberList().add(
                SpectorSubscriberEntity
                        .builder()
                        .spector(spectorEntity)
                        .userId(userId)
                        .subscriberRole(SubscriberRole.OWNER)
                        .build()
        );

        spectorRepository.save(spectorEntity);

        return spectorDtoFactory.makeSpectorDto(spectorEntity);

    }

    public List<SpectorResponseDto> find(Long id, String name) {

        Stream<SpectorEntity> spectorEntityStream = spectorRepository.streamAllByIdOrName(id, name);

        return spectorEntityStream.map(spectorDtoFactory::makeSpectorDto).toList();
    }

    public AnswerDto delete(Long id, Long userId) {

        SpectorEntity spectorEntity = getSpectorByIdOrThrowException(id);

        if (!userHaveRoles(spectorEntity, userId, SubscriberRole.OWNER)) {
            throw new BadRequestException(
                    String.format("User with id \"%s\" is not the owner of the spector with id \"%s\"", userId, id)
            );
        }

        spectorRepository.delete(spectorEntity);

        return AnswerDto.makeDefault(true);
    }

    public SpectorResponseDto update(Long id, Long userId, SpectorCreateUpdateDto spectorCreateUpdateDto) {

        SpectorEntity spectorEntity = getSpectorByIdOrThrowException(id);

        if (!userHaveRoles(spectorEntity, userId, SubscriberRole.OWNER)) {
            throw new BadRequestException(
                    String.format("User with id \"%s\" is not the owner of the spector with id \"%s\"", userId, id)
            );
        }

        if (spectorCreateUpdateDto.getName() != null && spectorCreateUpdateDto.getName().isBlank()) {
            throw new BadRequestException("Property \"name\" cannot be empty string");
        }
        if (spectorCreateUpdateDto.getName() == null && spectorCreateUpdateDto.getDescription() == null) {
            throw new BadRequestException("At least one property must be changed");
        }

        spectorEntity.setName(spectorCreateUpdateDto.getName() == null ? spectorEntity.getName() : spectorCreateUpdateDto.getName().trim());
        spectorEntity.setDescription(spectorCreateUpdateDto.getDescription() == null ? spectorEntity.getDescription() : spectorCreateUpdateDto.getDescription().trim());

        return spectorDtoFactory.makeSpectorDto(spectorEntity);
    }

    public AnswerDto addSubscriber(Long spectorId, Long userId) {

        SpectorEntity spectorEntity = getSpectorByIdOrThrowException(spectorId);

        List<SpectorSubscriberEntity> subscriberList = spectorEntity.getSubscriberList();

        boolean isSubscribe = subscriberList
                .stream()
                .map(SpectorSubscriberEntity::getUserId)
                .anyMatch(subscriberId -> subscriberId.equals(userId));

        if (isSubscribe) {
            return AnswerDto.makeDefault(true);
        }

        SpectorSubscriberEntity subscriber = SpectorSubscriberEntity.builder()
                .userId(userId)
                .spector(spectorEntity)
                .subscriberRole(SubscriberRole.SUBSCRIBER)
                .build();

        subscriberList.add(subscriber);

        spectorRepository.save(spectorEntity);

        return AnswerDto.makeDefault(true);
    }

    public Mono<Set<UserInternalResponseDto>> getSubscribers(Long spectorId) {

        SpectorEntity spectorEntity = getSpectorByIdOrThrowException(spectorId);

        List<SpectorSubscriberEntity> subscriberList = spectorEntity.getSubscriberList();

        List<Long> idList = subscriberList.stream().map(SpectorSubscriberEntity::getUserId).toList();

        return userService.getUserListByIdListAsync(idList);
    }

    public AnswerDto removeSubscriber(Long spectorId, Long userId) {

        SpectorEntity spectorEntity = getSpectorByIdOrThrowException(spectorId);

        spectorEntity.getSubscriberList().removeIf(subscriber -> subscriber.getUserId().equals(userId));

        spectorRepository.save(spectorEntity);

        return AnswerDto.makeDefault(true);
    }

    public void removeSubscriberFromAllSpectors(Long userId) {
        
        spectorSubscriberRepository.removeSubscriberFromAll(userId);
        
    }

    protected SpectorEntity getSpectorByIdOrThrowException(Long spectorId) {
        return spectorRepository.findById(spectorId).orElseThrow(() ->
                new NotFoundExeption(
                        String.format("Spector with id \"%s\" doesn't exist", spectorId)
                )
        );
    }

    protected boolean userHaveRoles(SpectorEntity spector, Long userId, SubscriberRole... subscriberRoleList) {

        Optional<SpectorSubscriberEntity> spectorSubscriberEntity = spectorSubscriberRepository.findBySpectorAndUserId(spector, userId);

        return spectorSubscriberEntity
                .map(subscriberEntity ->
                        Arrays.stream(subscriberRoleList).anyMatch(r -> r == subscriberEntity.getSubscriberRole())
                )
                .orElse(false);

    }

}
