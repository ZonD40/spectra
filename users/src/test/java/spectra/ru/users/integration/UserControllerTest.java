package spectra.ru.users.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import spectra.ru.event.RegistrationCodeEvent;
import spectra.ru.users.api.dto.AnswerDto;
import spectra.ru.users.api.dto.user.UserCreateDto;
import spectra.ru.users.api.dto.user.UserResponseDto;
import spectra.ru.users.config.TestRedisConfiguration;
import spectra.ru.users.exceptions.BadRequestException;
import spectra.ru.users.store.entities.UserEntity;
import spectra.ru.users.store.repository.UserRepository;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static spectra.ru.users.TestUtils.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = TestRedisConfiguration.class, properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}"})
@AutoConfigureMockMvc
@DirtiesContext
@EmbeddedKafka
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private Environment environment;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private KafkaMessageListenerContainer<String, RegistrationCodeEvent> registrationKafkaContainer;

    private BlockingQueue<ConsumerRecord<String, RegistrationCodeEvent>> registrationRecords;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    RedisTemplate<String, UserCreateDto> userRedisTemplate;

    @MockitoBean
    private Random random;

    @BeforeAll
    void setUp() {
        DefaultKafkaConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(getConsumerProperties());
        ContainerProperties registrationContainerProperties = new ContainerProperties(environment.getProperty("registration-code-topic-name"));

        registrationKafkaContainer = new KafkaMessageListenerContainer<>(consumerFactory, registrationContainerProperties);
        registrationRecords = new LinkedBlockingQueue<>();
        registrationKafkaContainer.setupMessageListener((MessageListener<String, RegistrationCodeEvent>) registrationRecords::add);
        registrationKafkaContainer.start();
        ContainerTestUtils.waitForAssignment(registrationKafkaContainer, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @Test
    @Sql(scripts = {"/data/cleanUp.sql", "/data/insertData.sql"})
    void registrationAlreadyExistExceptionTest() throws Exception {
        UserCreateDto userCreateDto = defaultUserCreateDto;

        String userJson = objectMapper.writeValueAsString(userCreateDto);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
                .andExpect(status().is4xxClientError())
                .andExpect(result -> assertInstanceOf(BadRequestException.class, result.getResolvedException()))
                .andExpect(result -> assertEquals(
                        String.format("User with email \"%s\" is already exist!", DEFAULT_EMAIL),
                        result.getResolvedException().getMessage()
                ));

    }

    @Test
    @Sql(scripts = {"/data/cleanUp.sql"})
    void registrationInvalidCodeExceptionTest() throws Exception {
        UserCreateDto userCreateDto = new UserCreateDto(DEFAULT_NAME, DEFAULT_EMAIL, "password");

        when(random.nextInt(100000, 1000000)).thenReturn(123456);

        String userJson = objectMapper.writeValueAsString(userCreateDto);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson));

        mockMvc.perform(post("/api/users/confirm")
                        .param("email", DEFAULT_EMAIL)
                        .param("code", String.valueOf(654321)))
                .andExpect(status().is4xxClientError())
                .andExpect(result -> assertInstanceOf(BadRequestException.class, result.getResolvedException()))
                .andExpect(result -> assertEquals(
                        "Invalid code",
                        result.getResolvedException().getMessage()
                ));

    }

    @Test
    @Sql(scripts = {"/data/cleanUp.sql"})
    void registrationMissingDataExceptionTest() throws Exception {
        UserCreateDto userCreateDto = new UserCreateDto(DEFAULT_NAME, DEFAULT_EMAIL, "password");

        when(random.nextInt(100000, 1000000)).thenReturn(123456);

        String userJson = objectMapper.writeValueAsString(userCreateDto);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson));

        userRedisTemplate.delete("pending:user:" + DEFAULT_EMAIL);

        mockMvc.perform(post("/api/users/confirm")
                        .param("email", DEFAULT_EMAIL)
                        .param("code", String.valueOf(123456)))
                .andExpect(status().is4xxClientError())
                .andExpect(result -> assertInstanceOf(BadRequestException.class, result.getResolvedException()))
                .andExpect(result -> assertEquals(
                        "User registration data is missing",
                        result.getResolvedException().getMessage()
                ));

    }

    @Test
    @Sql(scripts = {"/data/cleanUp.sql"})
    void registrationTest() throws Exception {
        UserCreateDto userCreateDto = new UserCreateDto(DEFAULT_NAME, DEFAULT_EMAIL, "password");

        when(random.nextInt(100000, 1000000)).thenReturn(123456);

        String userJson = objectMapper.writeValueAsString(userCreateDto);

        String answerDtoJson = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AnswerDto result = objectMapper.readValue(answerDtoJson, AnswerDto.class);

        assertTrue(result.getSuccess());

        ConsumerRecord<String, RegistrationCodeEvent> message = registrationRecords.poll(3000, TimeUnit.MILLISECONDS);

        assertNotNull(message);

        RegistrationCodeEvent registrationCodeEvent = message.value();
        String email = registrationCodeEvent.getEmail();
        int code = registrationCodeEvent.getCode();

        assertEquals(DEFAULT_EMAIL, email);
        assertEquals(123456, code);

        String userResponseDtoJson = mockMvc.perform(post("/api/users/confirm")
                        .param("email", email)
                        .param("code", String.valueOf(code)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserResponseDto userResponseDto = objectMapper.readValue(userResponseDtoJson, UserResponseDto.class);

        assertEquals(DEFAULT_EMAIL, userResponseDto.getEmail());
        assertEquals(DEFAULT_NAME, userResponseDto.getName());
        assertEquals(1L, userResponseDto.getId());
        assertNotNull(userResponseDto.getCreateAt());

        Optional<UserEntity> optionalUserEntity = userRepository.findById(1L);
        assertTrue(optionalUserEntity.isPresent());

        UserEntity userEntity = optionalUserEntity.get();
        assertEquals(DEFAULT_NAME, userEntity.getName());
        assertEquals(DEFAULT_EMAIL, userEntity.getEmail());
        assertEquals(1L, userEntity.getId());
        assertNotNull(userEntity.getPassword());
        assertNotNull(userEntity.getCreateAt());
    }

    private Map<String, Object> getConsumerProperties() {
        return Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString(),
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
                ConsumerConfig.GROUP_ID_CONFIG, environment.getProperty("spring.kafka.consumer.group-id"),
                JsonDeserializer.TRUSTED_PACKAGES, environment.getProperty("spring.kafka.consumer.properties.json.trusted.packages"),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, environment.getProperty("spring.kafka.consumer.auto-offset-reset")
        );
    }

    @AfterAll
    void tearDown() {
        registrationKafkaContainer.stop();
    }

}
