package com.codelevel.module.reviews_feedbacks.http.rest.routes;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class LessonFeedbackResourceIT {

    private String loginAndGetToken(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("accessToken");
    }

    @Test
    void shouldReturn201OnCreateFeedback() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("lessonId", 1001, "isHelpful", true, "comment", "Aula muito clara e objetiva!"))
                .when().post("/lesson-feedback")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("lessonId", equalTo(1001))
                .body("isHelpful", equalTo(true))
                .body("comment", equalTo("Aula muito clara e objetiva!"))
                .body("createdAt", notNullValue());
    }

    @Test
    void shouldReturn201OnCreateFeedbackWithoutComment() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("lessonId", 1002, "isHelpful", false))
                .when().post("/lesson-feedback")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("isHelpful", equalTo(false));
    }

    @Test
    void shouldReturn401OnCreateFeedbackWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("lessonId", 1099, "isHelpful", true))
                .when().post("/lesson-feedback")
                .then().statusCode(401);
    }

    @Test
    void shouldReturn422OnCreateFeedbackWithCommentTooLong() {
        String token = loginAndGetToken("user", "user");
        String longComment = "a".repeat(1001);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("lessonId", 1003, "isHelpful", true, "comment", longComment))
                .when().post("/lesson-feedback")
                .then().statusCode(422);
    }

    @Test
    void shouldReturn409OnDuplicateFeedback() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("lessonId", 1004, "isHelpful", true))
                .when().post("/lesson-feedback")
                .then().statusCode(201);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("lessonId", 1004, "isHelpful", false))
                .when().post("/lesson-feedback")
                .then().statusCode(409);
    }

    @Test
    void shouldReturn200OnGetFeedbackById() {
        String token = loginAndGetToken("user", "user");

        Long feedbackId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("lessonId", 1005, "isHelpful", true))
                .when().post("/lesson-feedback")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .when().get("/lesson-feedback/" + feedbackId)
                .then().statusCode(200)
                .body("id", equalTo(feedbackId.intValue()));
    }

    @Test
    void shouldReturn404OnGetNonexistentFeedback() {
        given()
                .when().get("/lesson-feedback/999999")
                .then().statusCode(404);
    }

    @Test
    void shouldReturn200OnGetFeedbacksByLesson() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("lessonId", 1006, "isHelpful", true, "comment", "Excelente!"))
                .when().post("/lesson-feedback")
                .then().statusCode(201);

        given()
                .when().get("/lesson-feedback/by-lesson/1006")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class))
                .body("size()", greaterThan(0));
    }

    @Test
    void shouldReturn200OnUpdateOwnFeedback() {
        String token = loginAndGetToken("user", "user");

        Long feedbackId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("lessonId", 1007, "isHelpful", true))
                .when().post("/lesson-feedback")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("isHelpful", false, "comment", "Mudei de ideia, não foi tão útil"))
                .when().put("/lesson-feedback/" + feedbackId)
                .then().statusCode(200)
                .body("isHelpful", equalTo(false))
                .body("comment", equalTo("Mudei de ideia, não foi tão útil"));
    }

    @Test
    void shouldReturn422OnUpdateAnotherUsersFeedback() {
        String userToken = loginAndGetToken("user", "user");

        Long feedbackId = given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(Map.of("lessonId", 1008, "isHelpful", true))
                .when().post("/lesson-feedback")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        String instructorToken = loginAndGetToken("instructor", "instructor");
        given()
                .header("Authorization", "Bearer " + instructorToken)
                .contentType(ContentType.JSON)
                .body(Map.of("isHelpful", false, "comment", "Tentando editar feedback alheio"))
                .when().put("/lesson-feedback/" + feedbackId)
                .then().statusCode(422);
    }

    @Test
    void shouldReturn204OnDeleteOwnFeedback() {
        String token = loginAndGetToken("user", "user");

        Long feedbackId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("lessonId", 1009, "isHelpful", true))
                .when().post("/lesson-feedback")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .when().delete("/lesson-feedback/" + feedbackId)
                .then().statusCode(204);
    }

    @Test
    void shouldReturn422OnDeleteAnotherUsersFeedback() {
        String userToken = loginAndGetToken("user", "user");

        Long feedbackId = given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(Map.of("lessonId", 1010, "isHelpful", true))
                .when().post("/lesson-feedback")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        String instructorToken = loginAndGetToken("instructor", "instructor");
        given()
                .header("Authorization", "Bearer " + instructorToken)
                .when().delete("/lesson-feedback/" + feedbackId)
                .then().statusCode(422);
    }

    @Test
    void shouldReturn204OnAdminDeleteAnyFeedback() {
        String userToken = loginAndGetToken("user", "user");

        Long feedbackId = given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(Map.of("lessonId", 1011, "isHelpful", true))
                .when().post("/lesson-feedback")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        String adminToken = loginAndGetToken("admin", "admin");
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when().delete("/lesson-feedback/" + feedbackId)
                .then().statusCode(204);
    }

    @Test
    void shouldReturn200OnGetByUser() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("lessonId", 1012, "isHelpful", true))
                .when().post("/lesson-feedback")
                .then().statusCode(201);

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/lesson-feedback/by-user")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }
}
