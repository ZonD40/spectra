package spectra.ru.community.kafka;

import spectra.ru.community.service.SpectorService;
import lombok.AllArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class KafkaConsumer {

    SpectorService spectorService;

    @KafkaListener(topics = "user-delete", groupId = "spectra-group")
    public void listen(Long id) {

        spectorService.removeSubscriberFromAllSpectors(id);

    }

}
