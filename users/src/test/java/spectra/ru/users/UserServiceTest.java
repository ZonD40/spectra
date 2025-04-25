package spectra.ru.users;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spectra.ru.users.api.dto.user.UserCreateDto;
import spectra.ru.users.service.UserService;
import spectra.ru.users.store.repository.UserRepository;

import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void register_Success() {
        UserCreateDto userCreateDto = new UserCreateDto("Jonh", "john@example.com", "password");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
    }

}
