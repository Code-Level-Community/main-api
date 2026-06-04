package com.codelevel.module.student_progress.http.rest.routes;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class LearningPathResourceIT {

    private String loginAndGetToken(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("accessToken");
    }

    private Map<String, Object> validBody() {
        return Map.of(
                "title", "Trilha de Java Backend Completa",
                "description", "Do básico ao avançado com Spring e Quarkus",
                "difficultyLevel", "BEGINNER"
        );
    }

    private Long createPath(String token) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/learning-paths")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    @Test
    void shouldReturn201OnCreateAsInstructor() {
        String token = loginAndGetToken("instructor", "instructor");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/learning-paths")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("published", equalTo(false))
                .body("coursesCount", equalTo(0));
    }

    @Test
    void shouldReturn403OnCreateAsUser() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/learning-paths")
                .then().statusCode(403);
    }

    @Test
    void shouldReturn401OnCreateWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/learning-paths")
                .then().statusCode(401);
    }

    @Test
    void shouldReturn200OnListPublished() {
        given()
                .when().get("/learning-paths")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void shouldReturn200OnGetById() {
        String token = loginAndGetToken("instructor", "instructor");
        Long id = createPath(token);

        given()
                .when().get("/learning-paths/" + id)
                .then().statusCode(200)
                .body("id", equalTo(id.intValue()));
    }

    @Test
    void shouldReturn404OnGetNonexistentPath() {
        given()
                .when().get("/learning-paths/999999")
                .then().statusCode(404);
    }

    @Test
    void shouldReturn200OnPublishByCreator() {
        String token = loginAndGetToken("instructor", "instructor");
        Long id = createPath(token);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when().post("/learning-paths/" + id + "/publish")
                .then().statusCode(200)
                .body("published", equalTo(true));
    }

    @Test
    void shouldReturn200OnPublishByAdmin() {
        String creatorToken = loginAndGetToken("instructor", "instructor");
        Long id = createPath(creatorToken);

        String adminToken = loginAndGetToken("admin", "admin");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .when().post("/learning-paths/" + id + "/publish")
                .then().statusCode(200)
                .body("published", equalTo(true));
    }

    @Test
    void shouldReturn201OnAddCourseToPath() {
        String token = loginAndGetToken("instructor", "instructor");
        Long id = createPath(token);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", 9001, "orderPosition", 1))
                .when().post("/learning-paths/" + id + "/course")
                .then().statusCode(201)
                .body("courseId", equalTo(9001))
                .body("orderPosition", equalTo(1));

        given()
                .when().get("/learning-paths/" + id)
                .then().statusCode(200)
                .body("coursesCount", equalTo(1));
    }

    @Test
    void shouldReturn409OnAddDuplicateCourse() {
        String token = loginAndGetToken("instructor", "instructor");
        Long id = createPath(token);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", 9002, "orderPosition", 1))
                .when().post("/learning-paths/" + id + "/course")
                .then().statusCode(201);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", 9002, "orderPosition", 2))
                .when().post("/learning-paths/" + id + "/course")
                .then().statusCode(409);
    }

    @Test
    void shouldReturn204OnRemoveCourse() {
        String token = loginAndGetToken("instructor", "instructor");
        Long id = createPath(token);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", 9003, "orderPosition", 1))
                .when().post("/learning-paths/" + id + "/course")
                .then().statusCode(201);

        given()
                .header("Authorization", "Bearer " + token)
                .when().delete("/learning-paths/" + id + "/course/9003")
                .then().statusCode(204);

        given()
                .when().get("/learning-paths/" + id)
                .then().statusCode(200)
                .body("coursesCount", equalTo(0));
    }

    @Test
    void shouldReturn200OnListCoursesInPath() {
        String token = loginAndGetToken("instructor", "instructor");
        Long id = createPath(token);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", 9004, "orderPosition", 1))
                .when().post("/learning-paths/" + id + "/course")
                .then().statusCode(201);

        given()
                .when().get("/learning-paths/" + id + "/courses")
                .then().statusCode(200)
                .body("size()", greaterThan(0));
    }
}
