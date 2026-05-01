package by.gsu.learningplatform.capabilities.users;

import by.gsu.learningplatform.core.web.CursorPageResponse;
import by.gsu.learningplatform.core.web.PaginationUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final PaginationUtils paginationUtils;

    public UserController(UserService userService, PaginationUtils paginationUtils) {
        this.userService = userService;
        this.paginationUtils = paginationUtils;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CursorPageResponse<UserResponse> listUsers(@RequestParam(required = false) Integer limit,
                                                      @RequestParam(required = false) String cursor,
                                                      @RequestParam(required = false) UserRole role) {
        final var pageable = paginationUtils.toPageable(limit, cursor);
        final var page = userService.listUsers(role, pageable);
        return new CursorPageResponse<>(
                page.getContent(),
                new CursorPageResponse.PageMetadata(pageable.getPageSize(), page.getNumberOfElements(), paginationUtils.nextCursor(page)),
                Map.of("self", "/users?limit=" + pageable.getPageSize() + "&cursor=" + page.getNumber() + (role == null ? "" : "&role=" + role.name())));
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser() {
        return userService.getCurrentUserResponse();
    }

    @GetMapping("/{userId}")
    public UserResponse getUser(@PathVariable UUID userId) {
        return userService.getVisibleUserResponse(userId);
    }

    @GetMapping("/{userId}/details")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDetailsResponse getUserDetails(@PathVariable UUID userId) {
        return userService.getUserDetails(userId);
    }
}
