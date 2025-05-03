package spectra.ru.users.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @Mock
    private JwtService jwtService;

    private final Random random = new Random();

    @InjectMocks
    private UserService userService;

    private static final String DEFAULT_NAME = "John";

    private static final String DEFAULT_EMAIL = "John123@mail.ru";

    private static UserEntity defaultUserEntity;

    private static UserResponseDto defaultUserResponseDto;

    private static UserCreateDto defaultUserCreateDto;

    private static UserAuthenticateDto defaultUserAuthenticateDto;

    private static JwtAuthenticationDto defaultJwtAuthenticationDto;

    private static Stream<Arguments> updateExceptionProviderFactory() {
        return Stream.of(
                Arguments.of(new UserCreateDto("John", "John123@mail.ru", ""), "password"),
                Arguments.of(new UserCreateDto("John", "", "pass"), "email"),
                Arguments.of(new UserCreateDto("", "John123@mail.ru", "pass"), "name")
                );
    }

    private static Stream<UserCreateDto> updateProviderFactory() {
        String newName = "Petr";
        String newEmail = "Petr321@gmail.com";
        String newPassword = "newPass";

        return Stream.of(
                new UserCreateDto(newName, null, null),
                new UserCreateDto(null, newEmail, null),
                new UserCreateDto(null, null, newPassword),
                new UserCreateDto(newName, newEmail, null),
                new UserCreateDto(newName, null, newPassword),
                new UserCreateDto(null, newEmail, newPassword),
                new UserCreateDto(newName, newEmail, newPassword)
                );
    }

    @BeforeAll()
    static void initAll() {
        defaultUserEntity = UserEntity
                .builder()
                .id(1L)
                .createAt(Instant.ofEpochSecond(1))
                .password("encoded")
                .email(DEFAULT_EMAIL)
                .name(DEFAULT_NAME)
                .build();

        defaultUserResponseDto = UserResponseDto
                .builder()
                .id(1L)
                .createAt(Instant.ofEpochSecond(1))
                .email(DEFAULT_EMAIL)
                .name(DEFAULT_NAME)
                .build();

        defaultUserCreateDto = new UserCreateDto(DEFAULT_NAME, DEFAULT_EMAIL, "pass");

        defaultUserAuthenticateDto = new UserAuthenticateDto(DEFAULT_EMAIL, "pass");

        defaultJwtAuthenticationDto = JwtAuthenticationDto
                .builder()
                .token("token")
                .refreshToken("refresh token")
                .build();
    }

    @BeforeEach
    void setUp() {
        // Ручное создание UserService с реальным Random
        userService = new UserService(
                userRepository,
                userDtoFactory,
                passwordEncoder,
                jwtService,
                deleteKafkaTemplate,
                registrationKafkaTemplate,
                codeRedisTemplate,
                userRedisTemplate,
                random
        );
    }

    @Test
    void registerExceptionTest() {
        when(userRepository.findByEmail(defaultUserCreateDto.getEmail().trim())).thenReturn(Optional.of(defaultUserEntity));

        Exception e = assertThrows(BadRequestException.class, () -> userService.register(defaultUserCreateDto));
        assertEquals(String.format("User with email \"%s\" is already exist!", DEFAULT_EMAIL), e.getMessage());
    }

    @Test
    void registerTest() {
        when(userRepository.findByEmail(defaultUserCreateDto.getEmail().trim())).thenReturn(Optional.empty());

        when(userRedisTemplate.opsForValue()).thenReturn(userValueOperations);
        when(codeRedisTemplate.opsForValue()).thenReturn(codeValueOperations);
        AnswerDto result = userService.register(defaultUserCreateDto);

        assertTrue(result.getSuccess());
        verify(codeValueOperations).set(eq(String.format("verify:email:%s", DEFAULT_EMAIL)), anyInt(), eq(Duration.ofMinutes(5)));
        verify(userValueOperations).set(String.format("pending:user:%s", DEFAULT_EMAIL), defaultUserCreateDto, Duration.ofMinutes(5));
        verify(registrationKafkaTemplate).send(eq("registration-code"), any(RegistrationCodeEvent.class));
    }

    @Test
    void confirmRegistrationNoSavedCodeTest() {
        Integer code = 123456;

        when(codeRedisTemplate.opsForValue()).thenReturn(codeValueOperations);
        when(codeValueOperations.get(String.format("verify:email:%s", DEFAULT_EMAIL))).thenReturn(null);

        Exception e = assertThrows(BadRequestException.class, () -> userService.confirmRegistration(DEFAULT_EMAIL, code));
        assertEquals("Invalid code", e.getMessage());
    }

    @Test
    void confirmRegistrationInvalidCodeTest() {
        Integer code = 123456;
        Integer savedCode = 654321;

        when(codeRedisTemplate.opsForValue()).thenReturn(codeValueOperations);
        when(codeValueOperations.get(String.format("verify:email:%s", DEFAULT_EMAIL))).thenReturn(savedCode);

        Exception e = assertThrows(BadRequestException.class, () -> userService.confirmRegistration(DEFAULT_EMAIL, code));
        assertEquals("Invalid code", e.getMessage());
    }

    @Test
    void confirmRegistrationNoSavedDtoTest() {
        Integer code = 123456;

        when(userRedisTemplate.opsForValue()).thenReturn(userValueOperations);
        when(codeRedisTemplate.opsForValue()).thenReturn(codeValueOperations);
        when(codeValueOperations.get(String.format("verify:email:%s", DEFAULT_EMAIL))).thenReturn(code);
        when(userValueOperations.get(String.format("pending:user:%s", DEFAULT_EMAIL))).thenReturn(null);

        Exception e = assertThrows(BadRequestException.class, () -> userService.confirmRegistration(DEFAULT_EMAIL, code));
        assertEquals("User registration data is missing", e.getMessage());
    }

    @Test
    void confirmRegistrationTest() {
        Integer code = 123456;

        when(userRedisTemplate.opsForValue()).thenReturn(userValueOperations);
        when(codeRedisTemplate.opsForValue()).thenReturn(codeValueOperations);
        when(codeValueOperations.get(String.format("verify:email:%s", DEFAULT_EMAIL))).thenReturn(code);
        when(userValueOperations.get(String.format("pending:user:%s", DEFAULT_EMAIL))).thenReturn(defaultUserCreateDto);
        when(passwordEncoder.encode(defaultUserCreateDto.getPassword().trim())).thenReturn("encoded");
        when(userRepository.save(any(UserEntity.class))).thenReturn(defaultUserEntity);
        when(userDtoFactory.makeUserResponseDto(defaultUserEntity)).thenReturn(defaultUserResponseDto);

        UserResponseDto result = userService.confirmRegistration(DEFAULT_EMAIL, code);

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
        when(userRepository.findByEmail(defaultUserAuthenticateDto.getEmail().trim())).thenReturn(Optional.of(defaultUserEntity));
        when(passwordEncoder.matches(defaultUserAuthenticateDto.getPassword().trim(), defaultUserEntity.getPassword().trim())).thenReturn(true);
        when(jwtService.generateAuthToken(defaultUserEntity.getEmail(), defaultUserEntity.getId())).thenReturn(defaultJwtAuthenticationDto);

        JwtAuthenticationDto result = userService.authenticate(defaultUserAuthenticateDto);

        assertEquals(defaultJwtAuthenticationDto, result);
    }

    @Test
    void refreshBlacklistExceptionTest() {
        String refreshToken = "token";

        when(jwtService.isTokenInBlacklist(refreshToken)).thenReturn(true);

        Exception e = assertThrows(BadRequestException.class, () -> userService.refresh(refreshToken));
        assertEquals("Token is in blacklist", e.getMessage());
    }

    @Test
    void refreshInvalidExceptionTest() {
        String refreshToken = "token";
        Claims claims = mock(Claims.class);

        when(jwtService.getClaims(refreshToken)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(DEFAULT_EMAIL);
        when(userRepository.existsByEmail(DEFAULT_EMAIL)).thenReturn(false);
        when(jwtService.isTokenInBlacklist(refreshToken)).thenReturn(false);

        Exception e = assertThrows(BadRequestException.class, () -> userService.refresh(refreshToken));
        assertEquals("Invalid refresh token", e.getMessage());
    }

    @Test
    void refreshTest() {
        String refreshToken = "token";
        Claims claims = mock(Claims.class);

        when(jwtService.getClaims(refreshToken)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(DEFAULT_EMAIL);
        when(claims.get("id", Long.class)).thenReturn(1L);
        when(userRepository.existsByEmail(DEFAULT_EMAIL)).thenReturn(true);
        when(jwtService.generateAuthToken(DEFAULT_EMAIL, 1L)).thenReturn(defaultJwtAuthenticationDto);

        JwtAuthenticationDto result = userService.refresh(refreshToken);

        assertEquals(defaultJwtAuthenticationDto, result);
        verify(jwtService).addTokenToBlacklist(refreshToken);
    }

    @Test
    void findByIdExceptionTest() {
        Long id = 1L;

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        Exception e = assertThrows(NotFoundExeption.class, () -> userService.find(id));
        assertEquals("User with id \"1\" doesn't exist!", e.getMessage());
    }

    @Test
    void findByIdTest() {
        Long id = 1L;

        when(userRepository.findById(id)).thenReturn(Optional.of(defaultUserEntity));
        when(userDtoFactory.makeUserResponseDto(defaultUserEntity)).thenReturn(defaultUserResponseDto);

        UserResponseDto result = userService.find(id);

        assertEquals(defaultUserResponseDto, result);
    }

    @ParameterizedTest
    @CsvSource({DEFAULT_NAME + ", " + DEFAULT_EMAIL,
            ", " + DEFAULT_EMAIL,
            DEFAULT_NAME+ ", ",
            ", "
    })
    void findByNameOrEmailTest(String name, String email) {
        lenient().when(userRepository.streamAllBy()).thenReturn(Stream.of(defaultUserEntity));
        lenient().when(userRepository.streamAllByNameOrEmail(DEFAULT_NAME, null)).thenReturn(Stream.of(defaultUserEntity));
        lenient().when(userRepository.streamAllByNameOrEmail(null, DEFAULT_EMAIL)).thenReturn(Stream.of(defaultUserEntity));
        lenient().when(userRepository.streamAllByNameOrEmail(DEFAULT_NAME, DEFAULT_EMAIL)).thenReturn(Stream.of(defaultUserEntity));
        when(userDtoFactory.makeUserResponseDto(defaultUserEntity)).thenReturn(defaultUserResponseDto);

        List<UserResponseDto> result = userService.find(name, email);

        assertEquals(List.of(defaultUserResponseDto), result);
    }

    @Test
    void deleteExceptionTest() {
        Long id = 1L;

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        Exception e = assertThrows(NotFoundExeption.class, () -> userService.delete(id));
        assertEquals("User with id \"1\" doesn't exist!", e.getMessage());
    }

    @Test
    void deleteTest() {
        Long id = 1L;

        when(userRepository.findById(id)).thenReturn(Optional.of(defaultUserEntity));

        AnswerDto result = userService.delete(id);

        assertTrue(result.getSuccess());
        verify(userRepository).deleteById(id);
        verify(deleteKafkaTemplate).send("user-delete", id);
    }

    @ParameterizedTest
    @MethodSource("updateExceptionProviderFactory")
    void updateEmptyExceptionTest(UserCreateDto userCreateDto, String propertyName) {
        Long id = 1L;

        Exception e = assertThrows(BadRequestException.class, () -> userService.update(id, userCreateDto));
        assertEquals(String.format("Property \"%s\" cannot be empty string", propertyName), e.getMessage());
    }

    @Test
    void updateNullExceptionTest() {
        Long id = 1L;
        UserCreateDto userCreateDto = new UserCreateDto();

        Exception e = assertThrows(BadRequestException.class, () -> userService.update(id, userCreateDto));
        assertEquals("At least one property must be specified", e.getMessage());
    }

    @Test
    void updateNotExistExceptionTest() {
        Long id = 1L;

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        Exception e = assertThrows(NotFoundExeption.class, () -> userService.update(id, defaultUserCreateDto));
        assertEquals("User with id \"1\" doesn't exist", e.getMessage());
    }

    @Test
    void updateAlreadyExistExceptionTest() {
        Long id = 1L;

        when(userRepository.findById(id)).thenReturn(Optional.of(defaultUserEntity));
        when(userRepository.findByEmail(defaultUserCreateDto.getEmail().trim())).thenReturn(Optional.of(defaultUserEntity));

        Exception e = assertThrows(BadRequestException.class, () -> userService.update(id, defaultUserCreateDto));
        assertEquals(String.format("User with email \"%s\" is already exist!", DEFAULT_EMAIL), e.getMessage());
    }

    @ParameterizedTest
    @MethodSource("updateProviderFactory")
    void updatePartialFieldsTest(UserCreateDto userCreateDto) {
        Long id = 1L;
        Instant now = Instant.now();
        UserEntity userEntity = new UserEntity(id, now, DEFAULT_NAME, DEFAULT_EMAIL, "encoded");
        String name = userCreateDto.getName() == null ? DEFAULT_NAME : userCreateDto.getName();
        String email = userCreateDto.getEmail() == null ? DEFAULT_EMAIL : userCreateDto.getEmail();
        UserResponseDto userResponseDto = new UserResponseDto(id, now, name, email);

        when(userRepository.findById(id)).thenReturn(Optional.of(userEntity));
        when(userDtoFactory.makeUserResponseDto(userEntity)).thenReturn(userResponseDto);
        if (userCreateDto.getPassword() != null) {
            when(passwordEncoder.encode(userCreateDto.getPassword().trim())).thenReturn("encoded2");
        }

        UserResponseDto result = userService.update(id, userCreateDto);

        assertEquals(userResponseDto, result);
        verify(userRepository).save(userEntity);

        String nameForTest = userCreateDto.getName() == null ? DEFAULT_NAME : name;
        String emailForTest = userCreateDto.getEmail() == null ? DEFAULT_EMAIL : email;
        String passwordForTest = userCreateDto.getPassword() == null ? "encoded" : "encoded2";

        assertEquals(nameForTest, userEntity.getName());
        assertEquals(emailForTest, userEntity.getEmail());
        assertEquals(passwordForTest, userEntity.getPassword());
    }

    @Test
    void batchEmptyListTest() {
        List<Long> idList = List.of();

        when(userRepository.streamAllByIdIn(idList)).thenReturn(Stream.empty());

        List<UserInternalResponseDto> result = userService.batch(idList);

        assertTrue(result.isEmpty());
    }

    @Test
    void batchNonExistingIdsTest() {
        List<Long> idList = List.of(999L);
        when(userRepository.streamAllByIdIn(idList)).thenReturn(Stream.empty());

        List<UserInternalResponseDto> result = userService.batch(idList);

        assertTrue(result.isEmpty());
    }

    @Test
    void batchTest() {
        List<Long> idList = List.of(1L);
        UserInternalResponseDto userInternalResponseDto = new UserInternalResponseDto(1L, DEFAULT_NAME);

        when(userRepository.streamAllByIdIn(idList)).thenReturn(Stream.of(defaultUserEntity));
        when(userDtoFactory.makeUserInternalResponseDto(defaultUserEntity)).thenReturn(userInternalResponseDto);

        List<UserInternalResponseDto> result = userService.batch(idList);

        assertEquals(List.of(userInternalResponseDto), result);
    }

    @Test
    void batchEmailEmptyListTest() {
        List<Long> idList = List.of();
        when(userRepository.streamAllByIdIn(idList)).thenReturn(Stream.empty());

        List<String> result = userService.batchEmail(idList);

        assertTrue(result.isEmpty());
    }

    @Test
    void batchEmailTest() {
        List<Long> idList = List.of(1L);

        when(userRepository.streamAllByIdIn(idList)).thenReturn(Stream.of(defaultUserEntity));

        List<String> result = userService.batchEmail(idList);

        assertEquals(List.of(DEFAULT_EMAIL), result);
    }

    @Test
    void loadUserByUsernameExceptionTest() {
        String email = DEFAULT_EMAIL;

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        Exception e = assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername(email));
        assertEquals(String.format("User with email \"%s\" doesn't exist", DEFAULT_EMAIL), e.getMessage());
    }

    @Test
    void loadUserByUsernameTest() {
        String email = DEFAULT_EMAIL;

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(defaultUserEntity));

        UserEntity result = userService.loadUserByUsername(email);
        assertEquals(defaultUserEntity, result);
    }
}
