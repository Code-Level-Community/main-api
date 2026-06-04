package com.codelevel.module.student_progress.http.rest.routes;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class LessonProgressResourceIT {

    private String loginAndGetToken(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("accessToken");
    }

    @Test
    void shouldReturn200OnTrackProgressBelow90Percent() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("lessonId", 1001, "watchTimeSeconds", 80, "videoDurationSeconds", 100))
                .when().post("/lesson-progress/track")
                .then().statusCode(200)
                .body("completed", equalTo(false))
                .body("completionPercentage", equalTo(80.0f));
    }

    @Test
    void shouldReturn200AndAutoCompleteAt90Percent() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("lessonId", 1002, "watchTimeSeconds", 90, "videoDurationSeconds", 100))
                .when().post("/lesson-progress/track")
                .then().statusCode(200)
                .body("completed", equalTo(true))
                .body("completedAt", notNullValue());
    }

    @Test
    void shouldReturn200OnMarkComplete() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when().post("/lesson-progress/1003/complete")
                .then().statusCode(200)
                .body("completed", equalTo(true))
                .body("completionPercentage", equalTo(100.0f));
    }

    @Test
    void shouldReturn401OnTrackWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("lessonId", 1004, "watchTimeSeconds", 50, "videoDurationSeconds", 100))
                .when().post("/lesson-progress/track")
                .then().statusCode(401);
    }

    @Test
    void shouldReturn200OnGetByLesson() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("lessonId", 1005, "watchTimeSeconds", 40, "videoDurationSeconds", 100))
                .when().post("/lesson-progress/track")
                .then().statusCode(200);

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/lesson-progress/1005")
                .then().statusCode(200)
                .body("lessonId", equalTo(1005));
    }

    @Test
    void shouldReturn404OnGetUntrackedLesson() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/lesson-progress/999999")
                .then().statusCode(404);
    }

    @Test
    void shouldReturn200OnListMyProgress() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("lessonId", 1006, "watchTimeSeconds", 30, "videoDurationSeconds", 100))
                .when().post("/lesson-progress/track")
                .then().statusCode(200);

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/lesson-progress/my")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }
}
