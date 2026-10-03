package com.sit.campusbackend.common;

import com.sit.campusbackend.complaint.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** The user is waiting for this email, so a failure is reported to them (503) rather than swallowed. */
    public void sendOtp(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your OTP Code — Campus Portal");
        message.setText("Your verification code is: " + otp + "\n\nIt expires in 10 minutes. If you did not request it, ignore this email.");
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Could not send OTP email", e);
            throw ApiException.unavailable("Could not send the verification email. Please try again later.");
        }
    }

    /** Best effort: the status change has already been saved, so a mail failure must not undo or fail it. */
    public void sendResolution(String to, String complaintTitle) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Complaint Resolved — " + complaintTitle);
        message.setText("Dear Student,\n\nYour complaint \"" + complaintTitle + "\" has been resolved.\n\n"
                + "If the issue persists, please raise a new complaint with updated details.\n\nRegards,\nCampus Management Team");
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("Could not send resolution email to {}: {}", to, e.getMessage());
        }
    }
}
