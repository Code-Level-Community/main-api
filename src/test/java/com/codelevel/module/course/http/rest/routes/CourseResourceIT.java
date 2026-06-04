package com.codelevel.module.course.http.rest.routes;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class CourseResourceIT {

    private String loginAndGetToken(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("accessToken");
    }

    private Map<String, Object> validCourseBody() {
        return Map.of(
                "title", "Java Fundamentals",
                "description", "Learn Java from scratch with hands-on exercises",
                "thumbnailUrl", "https://img.example.com/java.jpg",
                "difficultyLevel", "BEGINNER"
        );
    }

    @Test
    void shouldReturn201OnCreateCourseAsInstructor() {
        String token = loginAndGetToken("instructor", "instructor");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validCourseBody())
                .when().post("/course")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("title", equalTo("Java Fundamentals"))
                .body("status", equalTo("DRAFT"));
    }

    @Test
    void shouldReturn201OnCreateCourseAsAdmin() {
        String token = loginAndGetToken("admin", "admin");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validCourseBody())
                .when().post("/course")
                .then().statusCode(201)
                .body("id", notNullValue());
    }

    @Test
    void shouldReturn401OnCreateCourseWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .body(validCourseBody())
                .when().post("/course")
                .then().statusCode(401);
    }

    @Test
    void shouldReturn403OnCreateCourseAsUser() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validCourseBody())
                .when().post("/course")
                .then().statusCode(403);
    }

    @Test
    void shouldReturn200OnGetAllCourses() {
        given()
                .when().get("/course")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void shouldReturn200OnGetCourseById() {
        String token = loginAndGetToken("instructor", "instructor");

        Long id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validCourseBody())
                .when().post("/course")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .when().get("/course/" + id)
                .then().statusCode(200)
                .body("id", equalTo(id.intValue()));
    }

    @Test
    void shouldReturn404OnGetNonexistentCourse() {
        given()
                .when().get("/course/999999")
                .then().statusCode(404);
    }

    @Test
    void shouldReturn200OnUpdateCourse() {
        String token = loginAndGetToken("instructor", "instructor");

        Long id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validCourseBody())
                .when().post("/course")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Updated Title", "description", "Updated description text here", "thumbnailUrl", "https://img.com/new.jpg", "difficultyLevel", "ADVANCED"))
                .when().put("/course/" + id)
                .then().statusCode(200)
                .body("title", equalTo("Updated Title"))
                .body("difficultyLevel", equalTo("ADVANCED"));
    }

    @Test
    void shouldReturn204OnDeleteCourseAsAdmin() {
        String token = loginAndGetToken("admin", "admin");

        Long id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validCourseBody())
                .when().post("/course")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .when().delete("/course/" + id)
                .then().statusCode(204);
    }

    @Test
    void shouldReturn200OnPublishCourse() {
        String token = loginAndGetToken("admin", "admin");

        Long courseId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validCourseBody())
                .when().post("/course")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        Long moduleId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Module 1", "description", "First module", "orderPosition", 1))
                .when().post("/course/" + courseId + "/module")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Lesson 1", "contentType", "TEXT", "orderPosition", 1, "xpReward", 50))
                .when().post("/module/" + moduleId + "/lesson")
                .then().statusCode(201);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when().post("/course/" + courseId + "/publish")
                .then().statusCode(200)
                .body("status", equalTo("EXPERT_APPROVED"))
                .body("publishedAt", notNullValue());
    }

    @Test
    void shouldReturn422OnPublishCourseWithoutContent() {
        String token = loginAndGetToken("admin", "admin");

        Long courseId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validCourseBody())
                .when().post("/course")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when().post("/course/" + courseId + "/publish")
                .then().statusCode(422);
    }

    @Test
    void shouldReturn422OnCreateCourseWithInvalidTitle() {
        String token = loginAndGetToken("instructor", "instructor");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "AB", "description", "desc", "thumbnailUrl", "http://img.com/t.jpg", "difficultyLevel", "BEGINNER"))
                .when().post("/course")
                .then().statusCode(422);
    }
}
