package com.sit.campusbackend.complaint.service;

import com.sit.campusbackend.auth.entity.Student;
import com.sit.campusbackend.auth.repository.StudentRepository;
import com.sit.campusbackend.complaint.dto.*;
import com.sit.campusbackend.complaint.entity.*;
import com.sit.campusbackend.complaint.exception.ResourceNotFoundException;
import com.sit.campusbackend.complaint.repository.ComplaintRepository;
import com.sit.campusbackend.complaint.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock ComplaintRepository   complaintRepository;
    @Mock DepartmentRepository  departmentRepository;
    @Mock StudentRepository     studentRepository;
    @Mock JavaMailSender        mailSender;
    @Mock BCryptPasswordEncoder passwordEncoder;

    @InjectMocks ComplaintService service;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private Student    student;
    private Department itDept;
    private Complaint  savedComplaint;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setEmail("john.doe.2023@sitpune.edu.in");
        student.setFirstName("john");
        student.setLastName("doe");
        student.setIsVerified(true);

        itDept = new Department();
        itDept.setId(2L);
        itDept.setName("IT Department");
        itDept.setType("IT");
        itDept.setEmail("it@sitpune.edu.in");
        // BCrypt hash of "dept@123"
        itDept.setPasswordHash("$2a$10$/oXEBwDz2br4VJJmQkQdQOPQLmlANa3SnCUkzantg4hwr8ILRgcva");

        savedComplaint = new Complaint();
        savedComplaint.setId(1L);
        savedComplaint.setTitle("WiFi down");
        savedComplaint.setDescription("The wifi is not working in the lab.");
        savedComplaint.setCategory("IT");
        savedComplaint.setStatus(ComplaintStatus.ASSIGNED);
        savedComplaint.setPriority(ComplaintPriority.MEDIUM);
        savedComplaint.setStudent(student);
        savedComplaint.setDepartment(itDept);
        savedComplaint.setCreatedAt(LocalDateTime.now());
        savedComplaint.setUpdatedAt(LocalDateTime.now());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // detectCategory() tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("detectCategory: wifi → IT")
    void detectCategory_wifi_returnsIT() {
        assertThat(service.detectCategory("The wifi is not working")).isEqualTo("IT");
    }

    @Test @DisplayName("detectCategory: fan → Electrical")
    void detectCategory_fan_returnsElectrical() {
        assertThat(service.detectCategory("The fan in room 204 is broken")).isEqualTo("Electrical");
    }

    @Test @DisplayName("detectCategory: garbage → Cleaning")
    void detectCategory_garbage_returnsCleaning() {
        assertThat(service.detectCategory("There is garbage near the entrance")).isEqualTo("Cleaning");
    }

    @Test @DisplayName("detectCategory: hostel → Hostel")
    void detectCategory_hostel_returnsHostel() {
        assertThat(service.detectCategory("Hostel room beds are damaged")).isEqualTo("Hostel");
    }

    @Test @DisplayName("detectCategory: pipe leak → Plumbing")
    void detectCategory_pipe_returnsPlumbing() {
        assertThat(service.detectCategory("There is a pipe leak in the washroom")).isEqualTo("Plumbing");
    }

    @Test @DisplayName("detectCategory: unknown text → General")
    void detectCategory_unknown_returnsGeneral() {
        assertThat(service.detectCategory("Something random is wrong")).isEqualTo("General");
    }

    @Test @DisplayName("detectCategory: null description → General")
    void detectCategory_null_returnsGeneral() {
        assertThat(service.detectCategory(null)).isEqualTo("General");
    }

    @Test @DisplayName("detectCategory: blank description → General")
    void detectCategory_blank_returnsGeneral() {
        assertThat(service.detectCategory("   ")).isEqualTo("General");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // createComplaint() tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("createComplaint: valid request saves complaint with ASSIGNED status")
    void createComplaint_validRequest_savesAndReturnsResponse() {
        ComplaintRequest req = new ComplaintRequest();
        req.setEmail("john.doe.2023@sitpune.edu.in");
        req.setTitle("WiFi down");
        req.setDescription("The wifi is not working in the lab.");
        req.setImageUrl("https://img.example.com/1.jpg");

        when(studentRepository.findById(req.getEmail())).thenReturn(Optional.of(student));
        when(departmentRepository.findByType("IT")).thenReturn(Optional.of(itDept));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(savedComplaint);

        ComplaintResponse response = service.createComplaint(req);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ComplaintStatus.ASSIGNED);
        assertThat(response.getCategory()).isEqualTo("IT");
        assertThat(response.getDepartmentName()).isEqualTo("IT Department");

        verify(complaintRepository, times(1)).save(any(Complaint.class));
    }

    @Test @DisplayName("createComplaint: priority HIGH is saved correctly")
    void createComplaint_withHighPriority_savedAsHigh() {
        ComplaintRequest req = new ComplaintRequest();
        req.setEmail(student.getEmail());
        req.setTitle("Critical wifi issue");
        req.setDescription("The wifi is not working in the lab.");
        req.setPriority(ComplaintPriority.HIGH);

        // savedComplaint has MEDIUM by default; override for this test
        Complaint highPriorityComplaint = new Complaint();
        highPriorityComplaint.setId(2L);
        highPriorityComplaint.setTitle("Critical wifi issue");
        highPriorityComplaint.setDescription("The wifi is not working in the lab.");
        highPriorityComplaint.setCategory("IT");
        highPriorityComplaint.setStatus(ComplaintStatus.ASSIGNED);
        highPriorityComplaint.setPriority(ComplaintPriority.HIGH);
        highPriorityComplaint.setStudent(student);
        highPriorityComplaint.setDepartment(itDept);
        highPriorityComplaint.setCreatedAt(LocalDateTime.now());
        highPriorityComplaint.setUpdatedAt(LocalDateTime.now());

        when(studentRepository.findById(req.getEmail())).thenReturn(Optional.of(student));
        when(departmentRepository.findByType("IT")).thenReturn(Optional.of(itDept));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(highPriorityComplaint);

        ComplaintResponse response = service.createComplaint(req);

        assertThat(response.getPriority()).isEqualTo(ComplaintPriority.HIGH);
    }

    @Test @DisplayName("createComplaint: null priority defaults to MEDIUM")
    void createComplaint_nullPriority_defaultsToMedium() {
        ComplaintRequest req = new ComplaintRequest();
        req.setEmail(student.getEmail());
        req.setTitle("WiFi down");
        req.setDescription("The wifi is not working in the lab.");
        req.setPriority(null); // not provided

        when(studentRepository.findById(any())).thenReturn(Optional.of(student));
        when(departmentRepository.findByType("IT")).thenReturn(Optional.of(itDept));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> {
            Complaint c = inv.getArgument(0);
            assertThat(c.getPriority()).isEqualTo(ComplaintPriority.MEDIUM);
            return savedComplaint;
        });

        service.createComplaint(req);
        verify(complaintRepository).save(any(Complaint.class));
    }

    @Test @DisplayName("createComplaint: student not found → ResourceNotFoundException")
    void createComplaint_studentNotFound_throwsException() {
        ComplaintRequest req = new ComplaintRequest();
        req.setEmail("ghost@sitpune.edu.in");
        req.setTitle("Some issue");
        req.setDescription("Some description about the issue.");

        when(studentRepository.findById(req.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createComplaint(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Student not found");
    }

    @Test @DisplayName("createComplaint: null email → IllegalArgumentException")
    void createComplaint_nullEmail_throwsIllegalArgument() {
        ComplaintRequest req = new ComplaintRequest();
        req.setTitle("Title");
        req.setDescription("Description here.");

        assertThatThrownBy(() -> service.createComplaint(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test @DisplayName("createComplaint: falls back to General if category dept missing")
    void createComplaint_categoryDeptMissing_usesGeneral() {
        Department generalDept = new Department();
        generalDept.setId(6L);
        generalDept.setName("General Department");
        generalDept.setType("General");

        Complaint generalSaved = new Complaint();
        generalSaved.setId(5L);
        generalSaved.setTitle("Misc");
        generalSaved.setDescription("Something random.");
        generalSaved.setCategory("General");
        generalSaved.setStatus(ComplaintStatus.ASSIGNED);
        generalSaved.setPriority(ComplaintPriority.MEDIUM);
        generalSaved.setStudent(student);
        generalSaved.setDepartment(generalDept);
        generalSaved.setCreatedAt(LocalDateTime.now());
        generalSaved.setUpdatedAt(LocalDateTime.now());

        ComplaintRequest req = new ComplaintRequest();
        req.setEmail(student.getEmail());
        req.setTitle("Misc");
        req.setDescription("Something random is broken here.");

        when(studentRepository.findById(req.getEmail())).thenReturn(Optional.of(student));
        when(departmentRepository.findByType("General")).thenReturn(Optional.of(generalDept));
        when(complaintRepository.save(any())).thenReturn(generalSaved);

        ComplaintResponse resp = service.createComplaint(req);
        assertThat(resp.getCategory()).isEqualTo("General");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // updateStatus() tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("updateStatus: valid id updates status correctly")
    void updateStatus_validId_updatesStatus() {
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(savedComplaint));
        when(complaintRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ComplaintResponse resp = service.updateStatus(1L, ComplaintStatus.IN_PROGRESS);

        assertThat(resp.getStatus()).isEqualTo(ComplaintStatus.IN_PROGRESS);
    }

    @Test @DisplayName("updateStatus: invalid id → ResourceNotFoundException")
    void updateStatus_invalidId_throwsException() {
        when(complaintRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(99L, ComplaintStatus.CLOSED))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Complaint not found");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // getAllComplaints (paginated) tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("getAllComplaints(Pageable): returns mapped Page")
    void getAllComplaints_pageable_returnsMappedPage() {
        PageRequest pageable = PageRequest.of(0, 5);
        Page<Complaint> complaintPage = new PageImpl<>(List.of(savedComplaint), pageable, 1);

        when(complaintRepository.findAll(pageable)).thenReturn(complaintPage);

        Page<ComplaintResponse> result = service.getAllComplaints(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("WiFi down");
    }

    @Test @DisplayName("getAllComplaints(Pageable): empty page returns empty content")
    void getAllComplaints_emptyPage_returnsEmpty() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Complaint> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(complaintRepository.findAll(pageable)).thenReturn(emptyPage);

        Page<ComplaintResponse> result = service.getAllComplaints(pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // getDashboardStats() tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("getDashboardStats: returns correct counts from repository")
    void getDashboardStats_returnsCorrectCounts() {
        when(complaintRepository.count()).thenReturn(10L);
        when(complaintRepository.countByStatus(ComplaintStatus.PENDING)).thenReturn(2L);
        when(complaintRepository.countByStatus(ComplaintStatus.ASSIGNED)).thenReturn(3L);
        when(complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS)).thenReturn(2L);
        when(complaintRepository.countByStatus(ComplaintStatus.RESOLVED)).thenReturn(2L);
        when(complaintRepository.countByStatus(ComplaintStatus.CLOSED)).thenReturn(1L);

        DashboardStatsResponse stats = service.getDashboardStats();

        assertThat(stats.getTotal()).isEqualTo(10);
        assertThat(stats.getPending()).isEqualTo(2);
        assertThat(stats.getAssigned()).isEqualTo(3);
        assertThat(stats.getInProgress()).isEqualTo(2);
        assertThat(stats.getResolved()).isEqualTo(2);
        assertThat(stats.getClosed()).isEqualTo(1);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // departmentLogin() tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("departmentLogin: correct password returns LoginResponse")
    void departmentLogin_correctPassword_returnsResponse() {
        when(departmentRepository.findByEmail("it@sitpune.edu.in"))
                .thenReturn(Optional.of(itDept));
        when(passwordEncoder.matches("dept@123", itDept.getPasswordHash()))
                .thenReturn(true);

        LoginResponse resp = service.departmentLogin("it@sitpune.edu.in", "dept@123");

        assertThat(resp.getRole()).isEqualTo("DEPARTMENT");
        assertThat(resp.getDepartmentId()).isEqualTo(2L);
        assertThat(resp.getDepartmentName()).isEqualTo("IT Department");
    }

    @Test @DisplayName("departmentLogin: wrong password → IllegalArgumentException")
    void departmentLogin_wrongPassword_throwsException() {
        when(departmentRepository.findByEmail("it@sitpune.edu.in"))
                .thenReturn(Optional.of(itDept));
        when(passwordEncoder.matches("wrong", itDept.getPasswordHash()))
                .thenReturn(false);

        assertThatThrownBy(() -> service.departmentLogin("it@sitpune.edu.in", "wrong"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test @DisplayName("departmentLogin: unknown email → ResourceNotFoundException")
    void departmentLogin_unknownEmail_throwsException() {
        when(departmentRepository.findByEmail("unknown@sitpune.edu.in"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.departmentLogin("unknown@sitpune.edu.in", "pass"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No department found");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BCrypt encoder integration test (real encoder, no mock)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("BCrypt: encoded password matches raw password")
    void bcrypt_encodedPassword_matchesRaw() {
        BCryptPasswordEncoder realEncoder = new BCryptPasswordEncoder();
        String raw  = "dept@123";
        String hash = realEncoder.encode(raw);

        assertThat(realEncoder.matches(raw, hash)).isTrue();
        assertThat(realEncoder.matches("wrong", hash)).isFalse();
        // Different hashes for same input (salt randomness)
        assertThat(hash).isNotEqualTo(realEncoder.encode(raw));
    }

    @Test @DisplayName("BCrypt: stored hash in seed SQL is valid for dept@123")
    void bcrypt_seedHash_validForDeptPassword() {
        BCryptPasswordEncoder realEncoder = new BCryptPasswordEncoder();
        // Hash from seed_departments.sql for IT Department
        String storedHash = "$2a$10$/oXEBwDz2br4VJJmQkQdQOPQLmlANa3SnCUkzantg4hwr8ILRgcva";
        assertThat(realEncoder.matches("dept@123", storedHash)).isTrue();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // resolveComplaint() tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("resolveComplaint: sets RESOLVED status and sends email")
    void resolveComplaint_validId_resolvesAndEmailsSent() {
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(savedComplaint));
        when(complaintRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        ComplaintResponse resp = service.resolveComplaint(1L, "https://img.example.com/proof.jpg");

        assertThat(resp.getStatus()).isEqualTo(ComplaintStatus.RESOLVED);
        assertThat(resp.getResolvedImageUrl()).isEqualTo("https://img.example.com/proof.jpg");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage sentMsg = captor.getValue();
        assertThat(sentMsg.getTo()).contains(student.getEmail());
        assertThat(sentMsg.getSubject()).contains("Resolved");
    }

    @Test @DisplayName("resolveComplaint: complaint not found → ResourceNotFoundException")
    void resolveComplaint_notFound_throwsException() {
        when(complaintRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveComplaint(404L, "url"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // getStudentComplaints() and getDepartmentComplaints()
    // ═══════════════════════════════════════════════════════════════════════════

    @Test @DisplayName("getStudentComplaints: returns mapped list")
    void getStudentComplaints_returnsList() {
        when(complaintRepository.findByStudentEmail(student.getEmail()))
                .thenReturn(List.of(savedComplaint));

        List<ComplaintResponse> result = service.getStudentComplaints(student.getEmail());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStudentEmail()).isEqualTo(student.getEmail());
        assertThat(result.get(0).getPriority()).isEqualTo(ComplaintPriority.MEDIUM);
    }

    @Test @DisplayName("getDepartmentComplaints: returns complaints for department")
    void getDepartmentComplaints_returnsList() {
        when(complaintRepository.findByDepartmentId(2L)).thenReturn(List.of(savedComplaint));

        List<ComplaintResponse> result = service.getDepartmentComplaints(2L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDepartmentName()).isEqualTo("IT Department");
    }
}
