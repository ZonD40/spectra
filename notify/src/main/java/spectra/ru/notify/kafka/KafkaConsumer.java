package spectra.ru.notify.kafka;

import lombok.AllArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import spectra.ru.event.NewCommentEvent;
import spectra.ru.event.NewPostEvent;
import spectra.ru.event.NewReactionEvent;
import spectra.ru.event.RegistrationCodeEvent;
import spectra.ru.notify.dto.UserResponseDto;
import spectra.ru.notify.service.EmailService;
import spectra.ru.notify.service.UserService;

import java.util.List;

@Service
@AllArgsConstructor
public class KafkaConsumer {

    EmailService emailService;

    UserService userService;

    @KafkaListener(topics = "new-post", groupId = "spectra-group")
    public void listenToNewPost(NewPostEvent newPostEvent) {

        Mono<List<String>> monoEmailList = userService.getUserEmailListByIdListAsync(newPostEvent.getSubscribersIdList());

        List<String> emailList = monoEmailList.block();

        if (emailList == null) {
            return;
        }

        String subject = String.format("Новый пост в спектре %s", newPostEvent.getSpectorName());
        String body = String.format("Посмотрите новый пост тут: %s", newPostEvent.getPostLink());

        for (String email : emailList) {
            emailService.sendEmail(email, subject, body);
        }

    }

    @KafkaListener(topics = "new-comment", groupId = "spectra-group")
    public void listenToNewComment(NewCommentEvent newCommentEvent) {

        Mono<List<String>> monoEmailList = userService.getUserEmailListByIdListAsync(List.of(newCommentEvent.getUserId()));

        List<String> emailList = monoEmailList.block();

        if (emailList == null) {
            return;
        }

        String subject = String.format("Новый комментарий к вашему сообщению в спектре %s", newCommentEvent.getSpectorName());
        String body = String.format("К вашему сообщению: %s %nнаписали новый комментарий: %s",
                newCommentEvent.getMessageLink(),
                newCommentEvent.getCommentLink()
        );

        for (String email : emailList) {
            emailService.sendEmail(email, subject, body);
        }

    }

    @KafkaListener(topics = "new-reaction", groupId = "spectra-group")
    public void listenToNewReaction(NewReactionEvent newReactionEvent) {

        Mono<List<String>> monoEmailList = userService.getUserEmailListByIdListAsync(List.of(newReactionEvent.getUserId()));

        UserResponseDto userResponseDto = userService.getUserNameByIdAsync(newReactionEvent.getReactionUserId()).block();

        List<String> emailList = monoEmailList.block();

        if (emailList == null) {
            return;
        }

        String subject = "Новая реакция на ваше сообщение";
        String body = String.format("%s поставил  %s на ваше сообщение %s",
                userResponseDto.getName(),
                newReactionEvent.getReactionType(),
                newReactionEvent.getMessageLink()
        );

        for (String email : emailList) {
            emailService.sendEmail(email, subject, body);
        }

    }

    @KafkaListener(topics = "registration-code", groupId = "spectra-group")
    public void listenToRegistrationCode(RegistrationCodeEvent registrationCodeEvent) {

        String subject = "Код для регистрации в spectra";
        String body = String.format("Ваш код для регистрации: %s", registrationCodeEvent.getCode());


        emailService.sendEmail(registrationCodeEvent.getEmail(), subject, body);

    }

}