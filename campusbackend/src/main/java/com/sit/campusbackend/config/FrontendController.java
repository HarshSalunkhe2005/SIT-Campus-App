package com.sit.campusbackend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** When this app serves the frontend (FRONTEND_DIR set), the site root goes to the login page. */
@RestController
@ConditionalOnExpression("!'${app.frontend-dir:}'.isBlank()")
public class FrontendController {

    @GetMapping("/")
    public ResponseEntity<Void> root() {
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, "/templates/auth/login.html").build();
    }
}
