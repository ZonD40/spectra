package spectra.ru.users.api.factories;

import org.springframework.stereotype.Component;
import spectra.ru.users.api.dto.user.UserResponseDto;
import spectra.ru.users.api.dto.user.internal.UserInternalResponseDto;
import spectra.ru.users.store.entities.UserEntity;

@Component
public class UserDtoFactory {

    public UserResponseDto makeUserResponseDto(UserEntity entity) {
        return UserResponseDto.builder()
                .id(entity.getId())
                .createAt(entity.getCreateAt())
                .name(entity.getName())
                .email(entity.getEmail())
                .build();
    }

    public UserInternalResponseDto makeUserInternalResponseDto(UserEntity entity) {
        return UserInternalResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }

}
