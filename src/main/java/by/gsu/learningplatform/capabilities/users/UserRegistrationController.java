package by.gsu.learningplatform.capabilities.users;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserRegistrationController {

    private final UserRegistrationService userRegistrationService;

    public UserRegistrationController(final UserRegistrationService userRegistrationService) {
        this.userRegistrationService = userRegistrationService;
    }

    @PostMapping("/user-registrations")
    @ResponseStatus(HttpStatus.CREATED)
    public UserRegistrationResponse register( @Valid @RequestBody final UserRegistrationRequest request) {
        return userRegistrationService.register(request);
    }
}
