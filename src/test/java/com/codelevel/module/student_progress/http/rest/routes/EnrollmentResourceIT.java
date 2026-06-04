package com.codelevel.module.student_progress.http.rest.routes;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class EnrollmentResourceIT {

    private String loginAndGetToken(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("accessToken");
    }

    @Test
    void shouldReturn201OnEnroll() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", 100))
                .when().post("/enrollments")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("courseId", equalTo(100))
                .body("progressPercentage", equalTo(0.0f))
                .body("lessonsCompleted", equalTo(0))
                .body("enrolledAt", notNullValue());
    }

    @Test
    void shouldReturn409OnDuplicateEnrollment() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", 200))
                .when().post("/enrollments")
                .then().statusCode(201);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", 200))
                .when().post("/enrollments")
                .then().statusCode(409);
    }

    @Test
    void shouldReturn401OnEnrollWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", 300))
                .when().post("/enrollments")
                .then().statusCode(401);
    }

    @Test
    void shouldReturn200OnListMy() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", 400))
                .when().post("/enrollments")
                .then().statusCode(201);

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/enrollments/my")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void shouldReturn200OnGetById() {
        String token = loginAndGetToken("user", "user");

        Long id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", 500))
                .when().post("/enrollments")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/enrollments/" + id)
                .then().statusCode(200)
                .body("id", equalTo(id.intValue()));
    }

    @Test
    void shouldReturn404OnGetNonexistentEnrollment() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/enrollments/999999")
                .then().statusCode(404);
    }

    @Test
    void shouldReturn200OnListByCourse() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", 600))
                .when().post("/enrollments")
                .then().statusCode(201);

        given()
                .when().get("/enrollments/by-course/600")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class))
                .body("size()", greaterThan(0));
    }
}
