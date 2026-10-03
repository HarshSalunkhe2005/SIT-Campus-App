package com.sit.campusbackend.config;

import com.sit.campusbackend.auth.entity.Admin;
import com.sit.campusbackend.auth.entity.Student;
import com.sit.campusbackend.auth.repository.AdminRepository;
import com.sit.campusbackend.auth.repository.StudentRepository;
import com.sit.campusbackend.complaint.entity.Department;
import com.sit.campusbackend.complaint.repository.DepartmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

/**
 * First-run setup, driven entirely by configuration (no credentials live in the code or the repo):
 * creates the admin when BOOTSTRAP_ADMIN_EMAIL / BOOTSTRAP_ADMIN_PASSWORD are set, and the default departments
 * when BOOTSTRAP_DEPT_PASSWORD is set, and a ready-to-use student when BOOTSTRAP_DEMO_STUDENT_EMAIL / _PASSWORD are set
 * (for demo deployments where emails cannot be sent). Existing accounts are never touched, so it is safe to leave configured.
 */
@Component
@Profile("!test")
public class BootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapRunner.class);

    /** category (type) -> department name */
    private static final Map<String, String> DEFAULT_DEPARTMENTS = Map.of(
            "Electrical", "Electrical Department",
            "IT", "IT Department",
            "Cleaning", "Cleaning Department",
            "Hostel", "Hostel Department",
            "Plumbing", "Plumbing Department",
            "Civil", "Civil Department",
            "Furniture", "Furniture Department",
            "General", "General Department");

    private final AdminRepository admins;
    private final StudentRepository students;
    private final DepartmentRepository departments;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String deptPassword;
    private final String demoStudentEmail;
    private final String demoStudentPassword;

    public BootstrapRunner(AdminRepository admins, StudentRepository students, DepartmentRepository departments, BCryptPasswordEncoder passwordEncoder,
                           @Value("${app.bootstrap.admin-email:}") String adminEmail,
                           @Value("${app.bootstrap.admin-password:}") String adminPassword,
                           @Value("${app.bootstrap.dept-password:}") String deptPassword,
                           @Value("${app.bootstrap.demo-student-email:}") String demoStudentEmail,
                           @Value("${app.bootstrap.demo-student-password:}") String demoStudentPassword) {
        this.admins = admins;
        this.students = students;
        this.departments = departments;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
        this.adminPassword = adminPassword;
        this.deptPassword = deptPassword;
        this.demoStudentEmail = demoStudentEmail.trim().toLowerCase(Locale.ROOT);
        this.demoStudentPassword = demoStudentPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!adminEmail.isEmpty() && !adminPassword.isEmpty() && !admins.existsByEmailIgnoreCase(adminEmail)) {
            Admin admin = new Admin();
            admin.setEmail(adminEmail);
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setCreatedAt(LocalDateTime.now());
            admins.save(admin);
            log.info("Bootstrap: created admin {}", adminEmail);
        }

        if (!demoStudentEmail.isEmpty() && !demoStudentPassword.isEmpty() && !students.existsById(demoStudentEmail)) {
            Student student = new Student();
            student.setEmail(demoStudentEmail);
            student.setPrn("24070126000");
            student.setFirstName("Demo");
            student.setLastName("Student");
            student.setBatchYear("2024");
            student.setPasswordHash(passwordEncoder.encode(demoStudentPassword));
            student.setIsVerified(true);
            students.save(student);
            log.info("Bootstrap: created demo student {}", demoStudentEmail);
        }

        if (!deptPassword.isEmpty()) {
            String hash = passwordEncoder.encode(deptPassword);
            DEFAULT_DEPARTMENTS.forEach((type, name) -> {
                String email = type.toLowerCase(Locale.ROOT) + "@sitpune.edu.in";
                if (departments.findByType(type).isEmpty() && !departments.existsByEmailIgnoreCase(email)) {
                    Department d = new Department();
                    d.setName(name);
                    d.setType(type);
                    d.setEmail(email);
                    d.setPasswordHash(hash);
                    departments.save(d);
                    log.info("Bootstrap: created department {}", name);
                }
            });
        }
    }
}
