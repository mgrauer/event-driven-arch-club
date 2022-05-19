package event.club.admin.services;

import event.club.admin.domain.Chair;
import event.club.admin.http.UpdateChairCommand;
import event.club.admin.repositories.JpaChairRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChairManagementService {

    private final static Logger log = LoggerFactory.getLogger(ChairManagementService.class);

    private final JpaChairRepository chairRepository;

    private final RestTemplate client = new RestTemplate();

    private final String chairfrontLocation;

    @Autowired
    public ChairManagementService(JpaChairRepository chairRepository, @Value("${chairfront.location}") String chairfront) {
        this.chairRepository = chairRepository;
        this.chairfrontLocation = "http://"+chairfront;
        log.info("Initialized the chair service");
        log.info("Chairfront is located at {}", chairfront);
    }

    public Optional<Chair> get(UUID chairId) {
        return this.chairRepository.findById(chairId);
    }

    public List<Chair> list() {
        List<Chair> chairs = new ArrayList<>();
        this.chairRepository.findAll().forEach(chairs::add);
        return chairs;
    }

    public Chair create(UpdateChairCommand command) {
        // should throw invalid exceptions or return a -Result class
        log.info("About to save with {}, {}, {}", command.getRequestedSku(), command.getRequestedName(), command.getRequestedDescription());
        Chair target = new Chair(
                1,
                command.getRequestedSku(),
                command.getRequestedName(),
                command.getRequestedDescription()
        );
        Chair saved = this.chairRepository.save(target);

        log.info("Chair {} successfully saved, updating downstream...", saved.getId());
        log.info("Calling chairfront at {}", chairfrontLocation);

        Observable.fromCallable(() -> {
                    ResponseEntity<Boolean> result = client.postForEntity(chairfrontLocation+"/register", target, Boolean.class);
                    log.info("Observable: did we save from chairfront? {}", result.getStatusCode());
                    return result.getStatusCode();
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe(System.out::println, Throwable::printStackTrace);






        return saved;

    }

}
