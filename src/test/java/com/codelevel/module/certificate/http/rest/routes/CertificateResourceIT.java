package com.codelevel.module.certificate.http.rest.routes;

import com.codelevel.module.certificate.domain.Certificate;
import com.codelevel.module.certificate.persistence.entity.CertificateEntity;
import com.codelevel.module.certificate.persistence.resource.CertificateProcessor;
import com.codelevel.module.certificate.persistence.resource.CertificateService;
import com.codelevel.module.student_progress.persistence.entity.CourseEnrollmentEntity;
import com.codelevel.module.student_progress.persistence.resource.EnrollmentService;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class CertificateResourceIT {

    // courseIds únicos por cenário para evitar conflitos entre testes
    private static final long COMPLETED_COURSE    = 11001L;
    private static final long INCOMPLETE_COURSE   = 11002L;
    private static final long DUPLICATE_COURSE    = 11003L;
    private static final long MY_LIST_COURSE      = 11004L;
    private static final long VERIFY_COURSE       = 11005L;
    private static final long NOT_ENROLLED_COURSE = 99998L;

    @Inject
    EnrollmentService enrollmentService;

    @Inject
    CertificateService certificateService;

    @Inject
    CertificateProcessor processor;

    @Inject
    MockMailbox mailbox;

    private static UUID uid(long n) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", n));
    }

    @BeforeEach
    void clearMailbox() {
        mailbox.clear();
    }

    // =====================================================================
    // POST /certificates/request
    // =====================================================================

    @Test
    void shouldReturn202OnCertificateRequestWhenCourseIsCompleted() {
        String token = loginAndGetToken("user", "user");
        UUID userId = extractUserIdFromToken(token);

        CourseEnrollmentEntity enrollment = enrollmentService.enroll(COMPLETED_COURSE, userId);
        enrollmentService.updateProgress(enrollment.getId(), 1L, 1L, 0L);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", COMPLETED_COURSE))
                .when()
                .post("/certificates/request")
                .then()
                .statusCode(202)
                .body("id", notNullValue())
                .body("courseId", equalTo((int) COMPLETED_COURSE))
                .body("status", equalTo("PENDING"));
    }

    @Test
    void shouldReturn401OnCertificateRequestWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", 10L))
                .when()
                .post("/certificates/request")
                .then()
                .statusCode(401);
    }

    @Test
    void shouldReturn422OnCertificateRequestWhenEnrollmentNotCompleted() {
        String token = loginAndGetToken("user", "user");
        UUID userId = extractUserIdFromToken(token);

        enrollmentService.enroll(INCOMPLETE_COURSE, userId);
        // não chama updateProgress: completedAt permanece null

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", INCOMPLETE_COURSE))
                .when()
                .post("/certificates/request")
                .then()
                .statusCode(422);
    }

    @Test
    void shouldReturn404OnCertificateRequestWhenNotEnrolled() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", NOT_ENROLLED_COURSE))
                .when()
                .post("/certificates/request")
                .then()
                .statusCode(404);
    }

    @Test
    void shouldReturn409OnDuplicateCertificateRequest() {
        String token = loginAndGetToken("user", "user");
        UUID userId = extractUserIdFromToken(token);

        CourseEnrollmentEntity enrollment = enrollmentService.enroll(DUPLICATE_COURSE, userId);
        enrollmentService.updateProgress(enrollment.getId(), 1L, 1L, 0L);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", DUPLICATE_COURSE))
                .when()
                .post("/certificates/request")
                .then()
                .statusCode(202);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", DUPLICATE_COURSE))
                .when()
                .post("/certificates/request")
                .then()
                .statusCode(409);
    }

    // =====================================================================
    // GET /certificates/my
    // =====================================================================

    @Test
    void shouldReturn200WithCertificateListForAuthenticatedUser() {
        String token = loginAndGetToken("user", "user");
        UUID userId = extractUserIdFromToken(token);

        CourseEnrollmentEntity enrollment = enrollmentService.enroll(MY_LIST_COURSE, userId);
        enrollmentService.updateProgress(enrollment.getId(), 1L, 1L, 0L);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", MY_LIST_COURSE))
                .when()
                .post("/certificates/request")
                .then()
                .statusCode(202);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/certificates/my")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
                .body("courseId", hasItem((int) MY_LIST_COURSE))
                .body("status", everyItem(notNullValue()));
    }

    @Test
    void shouldReturn401OnListMyCertificatesWithoutToken() {
        given()
                .when()
                .get("/certificates/my")
                .then()
                .statusCode(401);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoCertificates() {
        String token = loginAndGetToken("admin", "admin");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/certificates/my")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    // =====================================================================
    // GET /certificates/verify/{code}
    // =====================================================================

    @Test
    void shouldReturn200OnVerifyValidCertificate() {
        CertificateEntity entity = certificateService.persistPending(
                uid(77), VERIFY_COURSE, "verify@resource.com", "Verify User");
        processor.process(entity.id);

        Certificate cert = certificateService.findById(entity.id);

        given()
                .when()
                .get("/certificates/verify/" + cert.certificateCode())
                .then()
                .statusCode(200)
                .body("certificateCode", notNullValue())
                .body("status", equalTo("SENT"))
                .body("verificationUrl", notNullValue());
    }

    @Test
    void shouldReturn200OnVerifyWithoutAuthenticationPublicEndpoint() {
        CertificateEntity entity = certificateService.persistPending(
                uid(77), VERIFY_COURSE + 1, "public@resource.com", "Public User");
        processor.process(entity.id);

        Certificate cert = certificateService.findById(entity.id);

        // sem token — endpoint é público (@PermitAll)
        given()
                .when()
                .get("/certificates/verify/" + cert.certificateCode())
                .then()
                .statusCode(200);
    }

    @Test
    void shouldReturn404OnVerifyNonExistentCertificate() {
        given()
                .when()
                .get("/certificates/verify/" + java.util.UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    void shouldReturnCorrectCertificateDataOnVerify() {
        CertificateEntity entity = certificateService.persistPending(
                uid(77), VERIFY_COURSE + 2, "data@resource.com", "Data User");
        processor.process(entity.id);

        Certificate cert = certificateService.findById(entity.id);

        given()
                .when()
                .get("/certificates/verify/" + cert.certificateCode())
                .then()
                .statusCode(200)
                .body("id", equalTo(entity.id.intValue()))
                .body("courseId", equalTo((int) (VERIFY_COURSE + 2)));
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private String loginAndGetToken(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("accessToken");
    }

    private UUID extractUserIdFromToken(String token) {
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        int start = payload.indexOf("\"sub\":\"") + 7;
        int end = payload.indexOf("\"", start);
        return UUID.fromString(payload.substring(start, end));
    }
}
