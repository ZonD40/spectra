package spectra.ru.users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;


@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

//    @Test
//    void registerTest() throws Exception {
//        UserCreateDto userCreateDto = new UserCreateDto("asd", "qwe@mail.ru","5rrt");
//        when(userService.register(userCreateDto)).thenReturn(AnswerDto.makeDefault(true));
//
//        mockMvc.perform(post("/api/users")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(userCreateDto)))
//                        .andExpect(status().isOk());
//
//    }

}
