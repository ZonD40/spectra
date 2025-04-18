package spectra.ru.users.api.controller;


import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import spectra.ru.users.api.dto.AnswerDto;
import spectra.ru.users.api.dto.user.UserAuthenticateDto;
import spectra.ru.users.api.dto.user.UserCreateDto;
import spectra.ru.users.api.dto.user.UserResponseDto;
import spectra.ru.users.api.dto.user.internal.UserInternalResponseDto;
import spectra.ru.users.service.UserService;
import spectra.ru.users.store.entities.UserEntity;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "api/users")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserService userService;

    @PostMapping
    public AnswerDto register(@RequestBody UserCreateDto userCreateDto) {
        return userService.register(userCreateDto);
    }

    @PostMapping("/confirm")
    public UserResponseDto confirmRegistration(
            @RequestParam(value = "code") Integer code,
            @RequestParam(value = "email") String email
    ) {
        return userService.confirmRegistration(email, code);
    }

    @PostMapping("/authenticate")
    public String authenticate(@RequestBody UserAuthenticateDto userAuthenticateDto) {
        return userService.authenticate(userAuthenticateDto);
    }

    @GetMapping()
    public List<UserResponseDto> get(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "email", required = false) String email
    ) {
        return userService.find(name, email);
    }

    @GetMapping("{id}")
    public UserResponseDto getById(@PathVariable(value = "id") Long id) {
        return userService.find(id);
    }

    @GetMapping("/me")
    public UserResponseDto getMe(@AuthenticationPrincipal UserEntity userEntity) {
        return userService.find(userEntity.getId());
    }

    @DeleteMapping()
    public AnswerDto delete(@AuthenticationPrincipal UserEntity userEntity) {
        return userService.delete(userEntity.getId());
    }

    @PatchMapping()
    public UserResponseDto patch(
            @AuthenticationPrincipal UserEntity userEntity,
            @RequestBody UserCreateDto userCreateDto
    ) {
        return userService.update(userEntity.getId(), userCreateDto);
    }

    @PostMapping("/internal/batch")
    public List<UserInternalResponseDto> batch(
            @RequestBody List<Long> userIdList
    ) {
        return userService.batch(userIdList);
    }

    @PostMapping("/internal/batchEmail")
    public List<String> batchEmail(
            @RequestBody List<Long> userIdList
    ) {
        return userService.batchEmail(userIdList);
    }

}
