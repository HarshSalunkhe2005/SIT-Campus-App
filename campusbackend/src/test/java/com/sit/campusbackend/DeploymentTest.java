package com.sit.campusbackend;

import com.sit.campusbackend.auth.repository.AdminRepository;
import com.sit.campusbackend.auth.repository.StudentRepository;
import com.sit.campusbackend.complaint.repository.DepartmentRepository;
import com.sit.campusbackend.config.BootstrapRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The single-deployment mode used by the Docker image: the backend serves the pages as well as the API. */
@TestPropertySource(properties = "app.frontend-dir=../src/main/resources")
class DeploymentTest extends IntegrationTestBase {

    @Autowired BCryptPasswordEncoder passwordEncoder;
    @Autowired StudentRepository studentRepo;
    @Autowired AdminRepository adminRepo;
    @Autowired DepartmentRepository departmentRepo;

    @Test
    void rootRedirectsToTheLoginPage() throws Exception {
        mvc.perform(get("/")).andExpect(status().isFound()).andExpect(header().string("Location", "/templates/auth/login.html"));
    }

    @Test
    void pagesAndAssetsAreServedWithoutLogin() throws Exception {
        mvc.perform(get("/templates/auth/login.html")).andExpect(status().isOk()).andExpect(content().string(containsString("login")));
        mvc.perform(get("/templates/student/dashboard.html")).andExpect(status().isOk());
        mvc.perform(get("/static/css/shared/base.css")).andExpect(status().isOk());
        mvc.perform(get("/static/js/shared/api.js")).andExpect(status().isOk()).andExpect(content().string(containsString("API_BASE_URL")));
    }

    @Test
    void theApiIsStillProtectedAndUnknownPathsStayClosed() throws Exception {
        mvc.perform(get("/student/my-reports")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/stats")).andExpect(status().isUnauthorized());
        mvc.perform(get("/campusbackend/pom.xml")).andExpect(status().isUnauthorized());
        mvc.perform(get("/templates/../../pom.xml")).andExpect(status().is4xxClientError());
        mvc.perform(get("/static/%2e%2e/%2e%2e/pom.xml")).andExpect(status().is4xxClientError());
    }

    @Test
    void bootstrapCreatesTheConfiguredAccountsOnceAndNeverOverwritesThem() throws Exception {
        BootstrapRunner runner = new BootstrapRunner(adminRepo, studentRepo, departmentRepo, passwordEncoder,
                "Owner@sitpune.edu.in", "Own3r#Pass", "Dept#Pass12", "demo@sitpune.edu.in", "Demo#Pass12");
        runner.run(null);
        runner.run(null); // idempotent

        assertEquals(8, departmentRepo.count());
        assertEquals(1, adminRepo.count());
        assertTrue(studentRepo.findById("demo@sitpune.edu.in").orElseThrow().getIsVerified());

        postJson("/auth/login", Map.of("email", "owner@sitpune.edu.in", "password", "Own3r#Pass")).andExpect(status().isOk());
        postJson("/auth/login", Map.of("email", "civil@sitpune.edu.in", "password", "Dept#Pass12")).andExpect(status().isOk());
        postJson("/auth/login", Map.of("email", "demo@sitpune.edu.in", "password", "Demo#Pass12")).andExpect(status().isOk());

        // a changed password survives a restart (the runner would otherwise reset it)
        var admin = adminRepo.findByEmailIgnoreCase("owner@sitpune.edu.in").orElseThrow();
        admin.setPasswordHash(passwordEncoder.encode("Chang3d#Pass"));
        adminRepo.save(admin);
        runner.run(null);
        postJson("/auth/login", Map.of("email", "owner@sitpune.edu.in", "password", "Chang3d#Pass")).andExpect(status().isOk());
    }
}
