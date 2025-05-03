package spectra.ru.users.service;

import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import spectra.ru.event.RegistrationCodeEvent;
import spectra.ru.users.api.dto.AnswerDto;
import spectra.ru.users.api.dto.user.JwtAuthenticationDto;
import spectra.ru.users.api.dto.user.UserAuthenticateDto;
import spectra.ru.users.api.dto.user.UserCreateDto;
import spectra.ru.users.api.dto.user.UserResponseDto;
import spectra.ru.users.api.dto.user.internal.UserInternalResponseDto;
import spectra.ru.users.api.factories.UserDtoFactory;
import spectra.ru.users.exceptions.BadRequestException;
import spectra.ru.users.exceptions.NotFoundExeption;
import spectra.ru.users.store.entities.UserEntity;
import spectra.ru.users.store.repository.UserRepository;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class UserService implements UserDetailsService {

    UserRepository userRepository;

    UserDtoFactory userDtoFactory;

    BCryptPasswordEncoder passwordEncoder;

    JwtService jwtService;

    KafkaTemplate<String, Long> deleteKafkaTemplate;

    KafkaTemplate<String, RegistrationCodeEvent> registrationKafkaTemplate;

    RedisTemplate<String, Integer> codeRedisTemplate;

    RedisTemplate<String, UserCreateDto> userRedisTemplate;

    Random random;

    static final String USER_KEY_PREFIX = "pending:user:";
    static final String VERIFICATION_KEY_PREFIX = "verify:email:";

    public AnswerDto register(UserCreateDto userCreateDto) {

        String email = userCreateDto.getEmail().trim();

        userRepository.findByEmail(email).ifPresent(userEntity -> {
            throw new BadRequestException(
                            String.format("User with email \"%s\" is already exist!", userEntity.getEmail())
                    );
        });

        String verificationKey = VERIFICATION_KEY_PREFIX + email;
        Integer code = random.nextInt(100000, 1000000);

        codeRedisTemplate.opsForValue().set(verificationKey, code, Duration.ofMinutes(5));

        String pendingKey = USER_KEY_PREFIX + email;

        userRedisTemplate.opsForValue().set(pendingKey, userCreateDto, Duration.ofMinutes(5));

        RegistrationCodeEvent registrationCodeEvent = RegistrationCodeEvent
                .builder()
                .email(email)
                .code(code)
                .build();

        registrationKafkaTemplate.send("registration-code", registrationCodeEvent);

        return AnswerDto.makeDefault(true);
    }

    public UserResponseDto confirmRegistration(String email, Integer code) {

        String verificationKey = VERIFICATION_KEY_PREFIX + email.trim();
        Integer savedCode = codeRedisTemplate.opsForValue().get(verificationKey);

        if (savedCode == null || !Objects.equals(savedCode, code)) {
            throw new BadRequestException("Invalid code");
        }

        String pendingKey = USER_KEY_PREFIX + email.trim();

        UserCreateDto userCreateDto = userRedisTemplate.opsForValue().get(pendingKey);

        if (userCreateDto == null) {
            throw new BadRequestException("User registration data is missing");
        }

        UserEntity userEntity = userRepository.save(UserEntity
                .builder()
                .name(userCreateDto.getName().trim())
                .email(userCreateDto.getEmail().trim())
                .password(passwordEncoder.encode(
                        userCreateDto.getPassword().trim()
                ))
                .build()
        );

        return userDtoFactory.makeUserResponseDto(userEntity);
    }

    public JwtAuthenticationDto authenticate(UserAuthenticateDto userAuthenticateDto) {
        Optional<UserEntity> optionalUserEntity = userRepository.findByEmail(userAuthenticateDto.getEmail().trim());

        if (optionalUserEntity.isEmpty()
                || !passwordEncoder.matches(userAuthenticateDto.getPassword().trim(), optionalUserEntity.get().getPassword().trim())) {
            throw new NotFoundExeption("invalid credentials");
        }

        UserEntity userEntity = optionalUserEntity.get();

        return jwtService.generateAuthToken(userEntity.getEmail(), userEntity.getId());
    }

    public JwtAuthenticationDto refresh(String token) {
        Claims claims = jwtService.getClaims(token);

        if (jwtService.isTokenInBlacklist(token)) {
            throw new BadRequestException("Token is in blacklist");
        }

        if (!userRepository.existsByEmail(claims.getSubject())) {
            throw new BadRequestException("Invalid refresh token");
        }

        jwtService.addTokenToBlacklist(token);

        return jwtService.generateAuthToken(claims.getSubject(), claims.get("id", Long.class));
    }

    public UserResponseDto find(Long id) {

        UserEntity userEntity = userRepository.findById(id).orElseThrow(() ->
                new NotFoundExeption(
                        String.format("User with id \"%s\" doesn't exist!", id)
                )
        );

        return userDtoFactory.makeUserResponseDto(userEntity);
    }

    @Transactional
    public List<UserResponseDto> find(String name, String email) {

        Stream<UserEntity> userEntityStream;

        if (name == null && email == null) {
            userEntityStream = userRepository.streamAllBy();
        } else {
            userEntityStream = userRepository.streamAllByNameOrEmail(name, email);
        }

        return userEntityStream.map(userDtoFactory::makeUserResponseDto).toList();
    }

    public AnswerDto delete(Long id) {

        userRepository
                .findById(id)
                .orElseThrow(() ->
                        new NotFoundExeption(
                                String.format("User with id \"%s\" doesn't exist!", id)
                        )
                );

        userRepository.deleteById(id);

        deleteKafkaTemplate.send("user-delete", id);

        return AnswerDto.makeDefault(true);
    }

    public UserResponseDto update(Long id, UserCreateDto userCreateDto) {

        String emptyErrorMessage = "Property \"%s\" cannot be empty string";

        if (userCreateDto.getName() != null && userCreateDto.getName().isBlank()) {
            throw new BadRequestException(String.format(emptyErrorMessage, "name"));
        } else if (userCreateDto.getEmail() != null && userCreateDto.getEmail().isBlank()) {
            throw new BadRequestException(String.format(emptyErrorMessage, "email"));
        } else if (userCreateDto.getPassword() != null && userCreateDto.getPassword().isBlank()) {
            throw new BadRequestException(String.format(emptyErrorMessage, "password"));
        } else if (userCreateDto.getPassword() == null && userCreateDto.getEmail() == null && userCreateDto.getName() == null) {
            throw new BadRequestException("At least one property must be specified");
        }

        Optional<UserEntity> optionalUserEntity = userRepository.findById(id);

        UserEntity userEntity = optionalUserEntity.orElseThrow(() ->
                new NotFoundExeption(
                        String.format("User with id \"%s\" doesn't exist", id)
                )
        );

        if (userCreateDto.getEmail() != null) {
            userRepository.findByEmail(userCreateDto.getEmail().trim()).ifPresent(foundUserEntity -> {
                throw new BadRequestException(
                        String.format("User with email \"%s\" is already exist!", foundUserEntity.getEmail())
                );
            });
        }

        userEntity.setName(userCreateDto.getName() == null ? userEntity.getName() : userCreateDto.getName().trim());
        userEntity.setEmail(userCreateDto.getEmail() == null ? userEntity.getEmail() : userCreateDto.getEmail().trim());
        userEntity.setPassword(userCreateDto.getPassword() == null
                ? userEntity.getPassword()
                : passwordEncoder.encode(userCreateDto.getPassword().trim())
        );

        userRepository.save(userEntity);

        return userDtoFactory.makeUserResponseDto(userEntity);
    }

    public List<UserInternalResponseDto> batch(List<Long> userIdList) {

        Stream<UserEntity> userEntityStream = userRepository.streamAllByIdIn(userIdList);

        return userEntityStream.map(userDtoFactory::makeUserInternalResponseDto).toList();
    }

    public List<String> batchEmail(List<Long> userIdList) {

        Stream<UserEntity> userEntityStream = userRepository.streamAllByIdIn(userIdList);

        return userEntityStream.map(UserEntity::getEmail).toList();
    }

    @Override
    public UserEntity loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<UserEntity> userEntity = userRepository.findByEmail(email);

        return userEntity.orElseThrow(() ->
                new UsernameNotFoundException(
                        String.format("User with email \"%s\" doesn't exist", email)
                )
        );
    }

}
