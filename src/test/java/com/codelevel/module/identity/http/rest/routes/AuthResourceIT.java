package com.codelevel.module.identity.http.rest.routes;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class AuthResourceIT {

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

    @Test
    void shouldReturn201OnSignupWithValidData() {
        String uniqueEmail = "signup_" + UUID.randomUUID() + "@test.com";

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("fullName", "Test User", "email", uniqueEmail, "password", "Password1!"))
            .when()
            .post("/auth/signup")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("email", equalTo(uniqueEmail));
    }

    @Test
    void shouldReturn409OnDuplicateSignupEmail() {
        String uniqueEmail = "dup_" + UUID.randomUUID() + "@test.com";

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("fullName", "First User", "email", uniqueEmail, "password", "Password1!"))
            .when()
            .post("/auth/signup")
            .then()
            .statusCode(201);

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("fullName", "Second User", "email", uniqueEmail, "password", "Password2!"))
            .when()
            .post("/auth/signup")
            .then()
            .statusCode(409);
    }

    @Test
    void shouldReturn200OnLoginWithValidCredentials() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("username", "admin", "password", "admin"))
            .when()
            .post("/auth/login")
            .then()
            .statusCode(200)
            .body("accessToken", notNullValue())
            .body("refreshToken", notNullValue())
            .body("expiresIn", equalTo(900));
    }

    @Test
    void shouldReturn401OnWrongPassword() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("username", "admin", "password", "wrongpassword"))
            .when()
            .post("/auth/login")
            .then()
            .statusCode(401);
    }

    @Test
    void shouldReturn401WhenLoginUserDoesNotExist() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("username", "nonexistent_" + UUID.randomUUID(), "password", "anypassword"))
            .when()
            .post("/auth/login")
            .then()
            .statusCode(401);
    }

    @Test
    void shouldReturn200OnRefreshWithValidToken() {
        String refreshToken = given()
            .contentType(ContentType.JSON)
            .body(Map.of("username", "instructor", "password", "instructor"))
            .when()
            .post("/auth/login")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getString("refreshToken");

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("refreshToken", refreshToken))
            .when()
            .post("/auth/refresh")
            .then()
            .statusCode(200)
            .body("accessToken", notNullValue());
    }

    @Test
    void shouldReturn400OnRefreshWithInvalidToken() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("refreshToken", "completely-invalid-token-" + UUID.randomUUID()))
            .when()
            .post("/auth/refresh")
            .then()
            .statusCode(400);
    }

    @Test
    void shouldReturn200ForGetMeWhenAuthenticated() {
        String token = loginAndGetToken("admin", "admin");

        given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/auth/me")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("user.username", equalTo("admin"));
    }

    @Test
    void shouldReturn401ForGetMeWithoutAuthentication() {
        given()
            .when()
            .get("/auth/me")
            .then()
            .statusCode(401);
    }

    @Test
    void shouldReturn200OnLogoutWithValidToken() {
        String token = loginAndGetToken("user", "user");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .when()
            .post("/auth/logout")
            .then()
            .statusCode(200);
    }
}
