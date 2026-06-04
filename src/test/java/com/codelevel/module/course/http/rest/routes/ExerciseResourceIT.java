package com.codelevel.module.course.http.rest.routes;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class ExerciseResourceIT {

    private String loginAndGetToken(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("accessToken");
    }

    private Long createLesson(String token) {
        Long courseId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Test Course", "description", "Test desc", "thumbnailUrl", "http://img.com/t.jpg", "difficultyLevel", "BEGINNER"))
                .when().post("/course")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        Long moduleId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Module", "description", "Desc", "orderPosition", 1))
                .when().post("/course/" + courseId + "/module")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        return given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Lesson", "contentType", "TEXT", "orderPosition", 1, "xpReward", 10))
                .when().post("/module/" + moduleId + "/lesson")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    @Test
    void shouldReturn201OnCreateExercise() {
        String token = loginAndGetToken("instructor", "instructor");
        Long lessonId = createLesson(token);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("type", "QUIZ", "title", "Quiz 1", "description", "First quiz", "maxAttempts", 3, "xpReward", 30))
                .when().post("/lesson/" + lessonId + "/exercise")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("title", equalTo("Quiz 1"))
                .body("type", equalTo("QUIZ"))
                .body("maxAttempts", equalTo(3));
    }

    @Test
    void shouldReturn401OnCreateExerciseWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("type", "QUIZ", "title", "Quiz"))
                .when().post("/lesson/1/exercise")
                .then().statusCode(401);
    }

    @Test
    void shouldReturn200OnGetExercisesByLesson() {
        String token = loginAndGetToken("instructor", "instructor");
        Long lessonId = createLesson(token);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("type", "QUIZ", "title", "Exercise 1", "maxAttempts", 2, "xpReward", 20))
                .when().post("/lesson/" + lessonId + "/exercise")
                .then().statusCode(201);

        given()
                .when().get("/lesson/" + lessonId + "/exercise")
                .then().statusCode(200)
                .body("size()", greaterThan(0));
    }

    @Test
    void shouldReturn200OnGetExerciseById() {
        String token = loginAndGetToken("instructor", "instructor");
        Long lessonId = createLesson(token);

        Long exerciseId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("type", "CODE_CHALLENGE", "title", "Exercise X", "maxAttempts", 1, "xpReward", 10))
                .when().post("/lesson/" + lessonId + "/exercise")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .when().get("/lesson/" + lessonId + "/exercise/" + exerciseId)
                .then().statusCode(200)
                .body("id", equalTo(exerciseId.intValue()));
    }

    @Test
    void shouldReturn404OnGetNonexistentExercise() {
        given()
                .when().get("/lesson/1/exercise/999999")
                .then().statusCode(404);
    }

    @Test
    void shouldReturn200OnUpdateExercise() {
        String token = loginAndGetToken("instructor", "instructor");
        Long lessonId = createLesson(token);

        Long exerciseId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("type", "QUIZ", "title", "Old Exercise", "maxAttempts", 2, "xpReward", 10))
                .when().post("/lesson/" + lessonId + "/exercise")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("type", "QUIZ", "title", "Updated Exercise", "maxAttempts", 5, "xpReward", 50))
                .when().put("/lesson/" + lessonId + "/exercise/" + exerciseId)
                .then().statusCode(200)
                .body("title", equalTo("Updated Exercise"))
                .body("maxAttempts", equalTo(5));
    }

    @Test
    void shouldReturn204OnDeleteExercise() {
        String token = loginAndGetToken("admin", "admin");
        Long lessonId = createLesson(token);

        Long exerciseId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("type", "QUIZ", "title", "To Delete", "maxAttempts", 1, "xpReward", 5))
                .when().post("/lesson/" + lessonId + "/exercise")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .when().delete("/lesson/" + lessonId + "/exercise/" + exerciseId)
                .then().statusCode(204);
    }
}
