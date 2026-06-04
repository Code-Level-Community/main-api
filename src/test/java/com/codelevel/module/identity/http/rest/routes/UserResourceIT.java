package com.codelevel.module.identity.http.rest.routes;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class UserResourceIT {

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
    void shouldReturn201OnCreateUserWithValidData() {
        String email = "create_" + UUID.randomUUID() + "@test.com";

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("fullName", "Create Test", "email", email, "password", "Password1!"))
            .when()
            .post("/user")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("email", equalTo(email));
    }

    @Test
    void shouldReturn409OnCreateUserWithDuplicateEmail() {
        String email = "dupuser_" + UUID.randomUUID() + "@test.com";

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("fullName", "First", "email", email, "password", "Password1!"))
            .when()
            .post("/user")
            .then()
            .statusCode(201);

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("fullName", "Second", "email", email, "password", "Password2!"))
            .when()
            .post("/user")
            .then()
            .statusCode(409);
    }

    @Test
    void shouldReturn200OnGetUserWhenAuthenticatedWithRoleUser() {
        // Create a user via signup — automatically gets ROLE_USER via @PrePersist
        String email = "getme_" + UUID.randomUUID() + "@test.com";
        Response signupResponse = given()
            .contentType(ContentType.JSON)
            .body(Map.of("fullName", "Get Me User", "email", email, "password", "Password1!"))
            .when()
            .post("/auth/signup")
            .then()
            .statusCode(201)
            .extract()
            .response();

        String userId = signupResponse.jsonPath().getString("id");
        String username = signupResponse.jsonPath().getString("username");

        String token = loginAndGetToken(username, "Password1!");

        given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/user/" + userId)
            .then()
            .statusCode(200)
            .body("id", equalTo(userId))
            .body("username", equalTo(username));
    }

    @Test
    void shouldReturn404OnGetNonexistentUser() {
        String token = loginAndGetToken("user", "user");

        given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/user/" + UUID.randomUUID())
            .then()
            .statusCode(404);
    }

    @Test
    void shouldReturn401OnGetUserWithoutAuthentication() {
        given()
            .when()
            .get("/user/" + UUID.randomUUID())
            .then()
            .statusCode(401);
    }

    @Test
    void shouldReturn200OnGetAllUsersWithEnabledUsers() {
        given()
            .when()
            .get("/user")
            .then()
            .statusCode(200)
            .body("size()", greaterThan(0));
    }

    @Test
    void shouldReturn200OnUpdateUserWithValidData() {
        String email = "update_" + UUID.randomUUID() + "@test.com";
        Response signupResponse = given()
            .contentType(ContentType.JSON)
            .body(Map.of("fullName", "Update Me", "email", email, "password", "Password1!"))
            .when()
            .post("/auth/signup")
            .then()
            .statusCode(201)
            .extract()
            .response();

        String userId = signupResponse.jsonPath().getString("id");
        String username = signupResponse.jsonPath().getString("username");
        String token = loginAndGetToken(username, "Password1!");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("username", "updated_name", "email", "updated_" + UUID.randomUUID() + "@test.com", "password", "Newpass123!"))
            .when()
            .put("/user/" + userId)
            .then()
            .statusCode(200)
            .body("username", equalTo("updated_name"));
    }

    @Test
    void shouldReturn204OnDeleteExistingUser() {
        String email = "todelete_" + UUID.randomUUID() + "@test.com";
        String userId = given()
            .contentType(ContentType.JSON)
            .body(Map.of("fullName", "To Delete", "email", email, "password", "Password1!"))
            .when()
            .post("/user")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getString("id");

        given()
            .when()
            .delete("/user/" + userId)
            .then()
            .statusCode(204);
    }
}
