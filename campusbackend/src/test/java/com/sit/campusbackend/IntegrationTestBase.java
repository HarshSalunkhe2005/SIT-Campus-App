package com.sit.campusbackend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sit.campusbackend.auth.entity.Admin;
import com.sit.campusbackend.auth.entity.Student;
import com.sit.campusbackend.auth.repository.AdminRepository;
import com.sit.campusbackend.auth.repository.StudentRepository;
import com.sit.campusbackend.complaint.entity.Department;
import com.sit.campusbackend.complaint.repository.ComplaintRepository;
import com.sit.campusbackend.complaint.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestBase.TestClockConfig.class)
public abstract class IntegrationTestBase {

    /** A clock the tests can move forward, to exercise expiry, cooldowns and rate-limit windows without sleeping. */
    public static class MutableClock extends Clock {
        private volatile Instant now = Instant.parse("2026-01-01T10:00:00Z");

        public void advance(Duration d) {
            now = now.plus(d);
        }

        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @TestConfiguration
    public static class TestClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }

    protected static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 0, 0};
    protected static final String STRONG_PASSWORD = "Str0ng!Pass";

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected StudentRepository students;
    @Autowired protected AdminRepository admins;
    @Autowired protected DepartmentRepository departments;
    @Autowired protected ComplaintRepository complaints;
    @Autowired protected BCryptPasswordEncoder encoder;
    @Autowired protected MutableClock clock;

    @MockitoBean protected JavaMailSender mailSender;

    @BeforeEach
    void resetData() {
        Mockito.reset(mailSender);
        complaints.deleteAll();
        students.deleteAll();
        departments.deleteAll();
        admins.deleteAll();
    }

    // ── seed helpers ─────────────────────────────────────────────────────────────────────────────

    protected String uniqueEmail() {
        return "stu" + UUID.randomUUID().toString().substring(0, 8) + ".test.2024@sitpune.edu.in";
    }

    protected Student student(String email, String password, boolean verified) {
        Student s = new Student();
        s.setEmail(email);
        s.setPrn("24070126001");
        s.setFirstName("Test");
        s.setLastName("Student");
        s.setBatchYear("2024");
        s.setPasswordHash(password == null ? null : encoder.encode(password));
        s.setIsVerified(verified);
        return students.save(s);
    }

    protected Department dept(String type, String password) {
        Department d = new Department();
        d.setName(type + " Department");
        d.setType(type);
        d.setEmail(type.toLowerCase() + "@sitpune.edu.in");
        d.setPasswordHash(encoder.encode(password));
        return departments.save(d);
    }

    protected Admin admin(String email, String password) {
        Admin a = new Admin();
        a.setEmail(email);
        a.setFirstName("System");
        a.setLastName("Administrator");
        a.setPasswordHash(encoder.encode(password));
        return admins.save(a);
    }

    // ── request helpers ──────────────────────────────────────────────────────────────────────────

    protected ResultActions postJson(String path, Object body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    protected ResultActions postJson(String path, Object body, String token) throws Exception {
        return mvc.perform(post(path).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    protected String login(String email, String password) throws Exception {
        String body = postJson("/auth/login", java.util.Map.of("email", email, "password", password))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(body);
        if (!node.has("token")) throw new AssertionError("login failed: " + body);
        return node.get("token").asText();
    }

    protected RequestBuilder report(String token, String location, String description, String category,
                                                   MockMultipartFile image) throws Exception {
        String payload = json.writeValueAsString(java.util.Map.of(
                "location", location, "description", description, "category", category == null ? "" : category));
        MockMultipartHttpServletRequestBuilder b = multipart("/student/report")
                .file(new MockMultipartFile("complaint", "", "application/json", payload.getBytes()));
        if (image != null) b.file(image);
        return b.header("Authorization", "Bearer " + token);
    }

    protected MockMultipartFile png() {
        return new MockMultipartFile("image", "photo.png", "image/png", PNG);
    }

    /** The 6-digit code from the most recent OTP email. */
    protected String lastOtp() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        Mockito.verify(mailSender, Mockito.atLeastOnce()).send(captor.capture());
        List<SimpleMailMessage> sent = captor.getAllValues();
        Matcher m = Pattern.compile("\\b(\\d{6})\\b").matcher(sent.get(sent.size() - 1).getText());
        if (!m.find()) throw new AssertionError("no OTP in mail");
        return m.group(1);
    }
}
