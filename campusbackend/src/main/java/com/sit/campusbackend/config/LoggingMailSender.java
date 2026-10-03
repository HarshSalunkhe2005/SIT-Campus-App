package com.sit.campusbackend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Arrays;

/** Development mail sender: prints emails (including OTP codes) to the log instead of sending them. */
@Configuration
@ConditionalOnProperty(name = "app.mail.log-only", havingValue = "true")
public class LoggingMailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailSender.class);

    @Bean
    public JavaMailSender javaMailSender() {
        return new JavaMailSenderImpl() {
            @Override
            public void send(SimpleMailMessage... messages) {
                for (SimpleMailMessage m : messages) {
                    log.info("[dev mail] to={} subject=\"{}\"\n{}", Arrays.toString(m.getTo()), m.getSubject(), m.getText());
                }
            }
        };
    }
}
