package spectra.ru.users.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import spectra.ru.event.RegistrationCodeEvent;
import spectra.ru.users.api.dto.AnswerDto;
import spectra.ru.users.api.dto.user.UserAuthenticateDto;
import spectra.ru.users.api.dto.user.UserCreateDto;
import spectra.ru.users.api.dto.user.UserResponseDto;
import spectra.ru.users.api.factories.UserDtoFactory;
import spectra.ru.users.exceptions.BadRequestException;
import spectra.ru.users.exceptions.NotFoundExeption;
import spectra.ru.users.store.entities.UserEntity;
import spectra.ru.users.store.repository.UserRepository;
import spectra.ru.users.TestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestPropertySource("application.properties")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private KafkaTemplate<String, Long> deleteKafkaTemplate;

    @Mock
    private KafkaTemplate<String, RegistrationCodeEvent> registrationKafkaTemplate;

    @Mock
    private RedisTemplate<String, Integer> codeRedisTemplate;

    @Mock
    private RedisTemplate<String, UserCreateDto> userRedisTemplate;

    @Mock
    private ValueOperations<String, Integer> codeValueOperations;

    @Mock
    private ValueOperations<String, UserCreateDto> userValueOperations;

    @Mock
    private UserDtoFactory userDtoFactory;

    @InjectMocks
    private UserService userService;

    private static UserEntity defaultUserEntity;

    private static UserResponseDto defaultUserResponseDto;

    private static UserCreateDto defaultUserCreateDto;

    private static UserAuthenticateDto defaultUserAuthenticateDto;

    @BeforeAll()
    static void initAll() {
        defaultUserEntity = UserEntity
                .builder()
                .id(1L)
                .createAt(Instant.ofEpochSecond(1))
                .password("encoded")
                .email("John123@mail.ru")
                .name("John")
                .build();

        defaultUserResponseDto = UserResponseDto
                .builder()
                .id(1L)
                .createAt(Instant.ofEpochSecond(1))
                .email("John123@mail.ru")
                .name("John")
                .build();

        defaultUserCreateDto = new UserCreateDto("John", "John123@mail.ru", "pass");

        defaultUserAuthenticateDto = new UserAuthenticateDto("John123@mail.ru", "pass");
    }

    @BeforeEach
    void setUp() {
        lenient().when(codeRedisTemplate.opsForValue()).thenReturn(codeValueOperations);
        lenient().when(userRedisTemplate.opsForValue()).thenReturn(userValueOperations);
        userService = new UserService(userRepository, userDtoFactory, passwordEncoder,
                deleteKafkaTemplate, registrationKafkaTemplate, codeRedisTemplate, userRedisTemplate, new Random());

        String jwtSecret = "veeeeeeeeeeeeeeeeeeryLongSecretKey";
        TestUtils.setField(userService, "jwtSecret", jwtSecret);
        TestUtils.setField(userService, "jwtExpiration", 3600000L);
    }

    @Test
    void registerExceptionTest() {
        when(userRepository.findByEmail(defaultUserCreateDto.getEmail().trim())).thenReturn(Optional.of(new UserEntity()));

        assertThrows(BadRequestException.class, () -> userService.register(defaultUserCreateDto));
    }

    @Test
    void registerTest() {
        when(userRepository.findByEmail(defaultUserCreateDto.getEmail().trim())).thenReturn(Optional.empty());

        AnswerDto result = userService.register(defaultUserCreateDto);

        assertTrue(result.getSuccess());
        verify(codeValueOperations).set(eq("verify:email:John123@mail.ru"), anyInt(), eq(Duration.ofMinutes(5)));
        verify(userValueOperations).set("pending:user:John123@mail.ru", defaultUserCreateDto, Duration.ofMinutes(5));
        verify(registrationKafkaTemplate).send(eq("registration-code"), any(RegistrationCodeEvent.class));
    }

    @Test
    void confirmRegistrationNoSavedCodeTest() {
        String email = "John123@mail.ru ";

        when(codeValueOperations.get("verify:email:John123@mail.ru")).thenReturn(null);

        Exception e = assertThrows(BadRequestException.class, () -> userService.confirmRegistration(email, null));
        assertEquals("Invalid code", e.getMessage());
    }

    @Test
    void confirmRegistrationInvalidCodeTest() {
        String email = "John123@mail.ru ";
        Integer code = 123456;
        Integer savedCode = 654321;

        when(codeValueOperations.get("verify:email:John123@mail.ru")).thenReturn(savedCode);

        Exception e = assertThrows(BadRequestException.class, () -> userService.confirmRegistration(email, code));
        assertEquals("Invalid code", e.getMessage());
    }

    @Test
    void confirmRegistrationNoSavedDtoTest() {
        String email = "John123@mail.ru ";
        Integer code = 123456;

        when(codeValueOperations.get("verify:email:John123@mail.ru")).thenReturn(code);
        when(userValueOperations.get("pending:user:John123@mail.ru")).thenReturn(null);

        Exception e = assertThrows(BadRequestException.class, () -> userService.confirmRegistration(email, code));
        assertEquals("Invalid code", e.getMessage());
    }

    @Test
    void confirmRegistrationTest() {
        String email = "John123@mail.ru ";
        Integer code = 123456;

        when(codeValueOperations.get("verify:email:John123@mail.ru")).thenReturn(code);
        when(userValueOperations.get("pending:user:John123@mail.ru")).thenReturn(defaultUserCreateDto);
        when(passwordEncoder.encode(defaultUserCreateDto.getPassword().trim())).thenReturn("encoded");
        when(userRepository.save(any(UserEntity.class))).thenReturn(defaultUserEntity);
        when(userDtoFactory.makeUserResponseDto(defaultUserEntity)).thenReturn(defaultUserResponseDto);

        UserResponseDto result = userService.confirmRegistration(email, code);

        assertEquals(defaultUserResponseDto, result);
    }

    @Test
    void authenticateEmptyUserTest() {
        when(userRepository.findByEmail(defaultUserAuthenticateDto.getEmail().trim())).thenReturn(Optional.empty());

        Exception e = assertThrows(NotFoundExeption.class, () -> userService.authenticate(defaultUserAuthenticateDto));
        assertEquals("invalid credentials", e.getMessage());
    }


    @Test
    void authenticateInvalidPasswordTest() {
        when(userRepository.findByEmail(defaultUserAuthenticateDto.getEmail().trim())).thenReturn(Optional.of(defaultUserEntity));
        when(passwordEncoder.matches(defaultUserAuthenticateDto.getPassword().trim(), defaultUserEntity.getPassword().trim())).thenReturn(false);

        Exception e = assertThrows(NotFoundExeption.class, () -> userService.authenticate(defaultUserAuthenticateDto));
        assertEquals("invalid credentials", e.getMessage());
    }

    @Test
    void authenticateTest() {
        String jwtSecret = "veeeeeeeeeeeeeeeeeeryLongSecretKey";

        when(userRepository.findByEmail(defaultUserAuthenticateDto.getEmail().trim())).thenReturn(Optional.of(defaultUserEntity));
        when(passwordEncoder.matches(defaultUserAuthenticateDto.getPassword().trim(), defaultUserEntity.getPassword().trim())).thenReturn(true);

        String result = userService.authenticate(defaultUserAuthenticateDto);

        assertEquals("John123@mail.ru", Jwts
                .parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .build()
                .parseSignedClaims(result)
                .getPayload()
                .getSubject()
        );
    }

    @Test
    void findExceptionTest() {
        Long id = 1L;

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        Exception e = assertThrows(NotFoundExeption.class, () -> userService.find(id));
        assertEquals("User with id \"1\" doesn't exist!", e.getMessage());
    }

    @Test
    void findTest() {
        Long id = 1L;

        when(userRepository.findById(id)).thenReturn(Optional.of(defaultUserEntity));
        when(userDtoFactory.makeUserResponseDto(defaultUserEntity)).thenReturn(defaultUserResponseDto);

        UserResponseDto result = userService.find(id);

        assertEquals(defaultUserResponseDto, result);
    }
}
