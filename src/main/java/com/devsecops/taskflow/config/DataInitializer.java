package com.devsecops.taskflow.config;

import com.devsecops.taskflow.domain.Role;
import com.devsecops.taskflow.domain.Task;
import com.devsecops.taskflow.domain.TaskPriority;
import com.devsecops.taskflow.domain.TaskStatus;
import com.devsecops.taskflow.domain.User;
import com.devsecops.taskflow.repository.TaskRepository;
import com.devsecops.taskflow.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Configuration
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository users;
    private final TaskRepository tasks;
    private final PasswordEncoder encoder;
    private final AppProperties properties;

    public DataInitializer(UserRepository users, TaskRepository tasks,
                           PasswordEncoder encoder, AppProperties properties) {
        this.users = users;
        this.tasks = tasks;
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.Seed seed = properties.seed();
        if (!seed.enabled()) {
            log.info("Seed disabled, skipping initial data creation");
            return;
        }

        String adminUsername = seed.admin().username();
        String adminPassword = seed.admin().password();

        if (adminUsername == null || adminUsername.isBlank() ||
                adminPassword == null || adminPassword.length() < 12) {
            throw new IllegalStateException(
                    "SEED_ENABLED=true but SEED_ADMIN_USERNAME or SEED_ADMIN_PASSWORD are missing/too weak. " +
                            "Admin password must be at least 12 characters.");
        }

        if (users.findByUsername(adminUsername).isEmpty()) {
            User admin = User.builder()
                    .username(adminUsername)
                    .email(adminUsername.toLowerCase() + "@taskflow.local")
                    .password(encoder.encode(adminPassword))
                    .fullName("System Administrator")
                    .role(Role.ROLE_ADMIN)
                    .enabled(true)
                    .build();
            users.save(admin);
            log.warn("Created initial admin user '{}' with password '{}'. Change it immediately.", adminUsername, adminPassword);
        }

        if (seed.demoEnabled() && tasks.count() == 0) {
            createDemoData();
        }
    }

    private void createDemoData() {
        log.info("Seeding demo data");

        User demo = users.findByUsername("demo").orElseGet(() ->
                users.save(User.builder()
                        .username("demo")
                        .email("demo@taskflow.local")
                        .password(encoder.encode("DemoUser@1234"))
                        .fullName("Demo User")
                        .role(Role.ROLE_USER)
                        .enabled(true)
                        .build())
        );

        User admin = users.findByUsername(properties.seed().admin().username()).orElseThrow();

        List<Task> demoTasks = List.of(
                Task.builder().title("Set up CI/CD pipeline").description("Create GitHub Actions workflows for build, test, and deploy").status(TaskStatus.TODO).priority(TaskPriority.HIGH).createdBy(admin).assignee(demo).build(),
                Task.builder().title("Write API documentation").description("Add OpenAPI specs and usage examples for all endpoints").status(TaskStatus.IN_PROGRESS).priority(TaskPriority.MEDIUM).createdBy(admin).build(),
                Task.builder().title("Perform security audit").description("Run OWASP dependency-check and Trivy scans").status(TaskStatus.TODO).priority(TaskPriority.URGENT).createdBy(demo).build(),
                Task.builder().title("Implement rate limiting").description("Add bucket4j-based rate limiting on public endpoints").status(TaskStatus.TODO).priority(TaskPriority.LOW).createdBy(admin).assignee(demo).build()
        );
        tasks.saveAll(demoTasks);
    }
}