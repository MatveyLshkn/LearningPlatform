package by.gsu.learningplatform.capabilities.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform-statistics")
public class AdminController {

    private final AdminStatisticsService adminStatisticsService;

    public AdminController(AdminStatisticsService adminStatisticsService) {
        this.adminStatisticsService = adminStatisticsService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PlatformStatisticsResponse statistics() {
        return adminStatisticsService.getPlatformStatistics();
    }
}
