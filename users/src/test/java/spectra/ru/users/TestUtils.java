package spectra.ru.users;

import spectra.ru.users.api.dto.user.UserCreateDto;

public class TestUtils {

    public static final String DEFAULT_NAME = "John";

    public static final String DEFAULT_EMAIL = "John123@mail.ru";

    public static final UserCreateDto defaultUserCreateDto = new UserCreateDto(DEFAULT_NAME, DEFAULT_EMAIL, "pass");


}
