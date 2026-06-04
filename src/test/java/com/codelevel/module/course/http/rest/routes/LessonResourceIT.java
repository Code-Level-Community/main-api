package com.codelevel.module.course.http.rest.routes;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class LessonResourceIT {

    private String loginAndGetToken(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("accessToken");
    }

    private Long createModule(String token) {
        Long courseId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Test Course", "description", "Test course desc", "thumbnailUrl", "http://img.com/t.jpg", "difficultyLevel", "BEGINNER"))
                .when().post("/course")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        return given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Test Module", "description", "Desc", "orderPosition", 1))
                .when().post("/course/" + courseId + "/module")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    @Test
    void shouldReturn201OnCreateLesson() {
        String token = loginAndGetToken("instructor", "instructor");
        Long moduleId = createModule(token);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Intro Lesson", "description", "First lesson", "contentType", "TEXT",
                        "orderPosition", 1, "xpReward", 50))
                .when().post("/module/" + moduleId + "/lesson")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("title", equalTo("Intro Lesson"))
                .body("xpReward", equalTo(50));
    }

    @Test
    void shouldReturn401OnCreateLessonWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Lesson", "orderPosition", 1))
                .when().post("/module/1/lesson")
                .then().statusCode(401);
    }

    @Test
    void shouldReturn200OnGetLessonsByModule() {
        String token = loginAndGetToken("instructor", "instructor");
        Long moduleId = createModule(token);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Lesson 1", "description", "Desc", "contentType", "TEXT", "orderPosition", 1, "xpReward", 10))
                .when().post("/module/" + moduleId + "/lesson")
                .then().statusCode(201);

        given()
                .when().get("/module/" + moduleId + "/lesson")
                .then().statusCode(200)
                .body("size()", greaterThan(0));
    }

    @Test
    void shouldReturn200OnGetLessonById() {
        String token = loginAndGetToken("instructor", "instructor");
        Long moduleId = createModule(token);

        Long lessonId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Lesson X", "description", "Desc", "contentType", "VIDEO",
                        "videoUrl", "https://video.com/x", "orderPosition", 1, "xpReward", 30))
                .when().post("/module/" + moduleId + "/lesson")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .when().get("/module/" + moduleId + "/lesson/" + lessonId)
                .then().statusCode(200)
                .body("id", equalTo(lessonId.intValue()));
    }

    @Test
    void shouldReturn404OnGetNonexistentLesson() {
        given()
                .when().get("/module/1/lesson/999999")
                .then().statusCode(404);
    }

    @Test
    void shouldReturn200OnUpdateLesson() {
        String token = loginAndGetToken("instructor", "instructor");
        Long moduleId = createModule(token);

        Long lessonId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Old Lesson", "contentType", "TEXT", "orderPosition", 1, "xpReward", 10))
                .when().post("/module/" + moduleId + "/lesson")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Updated Lesson", "contentType", "TEXT", "orderPosition", 2, "xpReward", 20))
                .when().put("/module/" + moduleId + "/lesson/" + lessonId)
                .then().statusCode(200)
                .body("title", equalTo("Updated Lesson"));
    }

    @Test
    void shouldReturn204OnDeleteLesson() {
        String token = loginAndGetToken("admin", "admin");
        Long moduleId = createModule(token);

        Long lessonId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "To Delete", "contentType", "TEXT", "orderPosition", 1, "xpReward", 5))
                .when().post("/module/" + moduleId + "/lesson")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .when().delete("/module/" + moduleId + "/lesson/" + lessonId)
                .then().statusCode(204);
    }
}
