package com.codelevel.module.student_progress.http.rest.routes;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class ExerciseAttemptResourceIT {

    private String loginAndGetToken(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("accessToken");
    }

    private Map<String, Object> correctAttemptBody(long exerciseId) {
        return Map.of(
                "exerciseId", exerciseId,
                "submittedAnswer", "A resposta correta",
                "correct", true,
                "baseXpReward", 100,
                "maxAttempts", 0,
                "feedback", "Parabéns!"
        );
    }

    @Test
    void shouldReturn201OnSubmitAttempt() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(correctAttemptBody(2001L))
                .when().post("/exercise-attempts")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("attemptNumber", equalTo(1))
                .body("correct", equalTo(true))
                .body("xpEarned", equalTo(100))
                .body("submittedAt", notNullValue());
    }

    @Test
    void shouldReturn201WithReducedXpAfterFailures() {
        String token = loginAndGetToken("instructor", "instructor");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("exerciseId", 2002L, "submittedAnswer", "errada", "correct", false, "baseXpReward", 100, "maxAttempts", 0, "feedback", "Tente novamente"))
                .when().post("/exercise-attempts")
                .then().statusCode(201);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("exerciseId", 2002L, "submittedAnswer", "certa", "correct", true, "baseXpReward", 100, "maxAttempts", 0, "feedback", "Correto!"))
                .when().post("/exercise-attempts")
                .then().statusCode(201)
                .body("xpEarned", equalTo(90));
    }

    @Test
    void shouldReturn422WhenMaxAttemptsExceeded() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("exerciseId", 2003L, "submittedAnswer", "a1", "correct", false, "baseXpReward", 50, "maxAttempts", 1, "feedback", ""))
                .when().post("/exercise-attempts")
                .then().statusCode(201);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("exerciseId", 2003L, "submittedAnswer", "a2", "correct", true, "baseXpReward", 50, "maxAttempts", 1, "feedback", ""))
                .when().post("/exercise-attempts")
                .then().statusCode(422);
    }

    @Test
    void shouldReturn401OnSubmitWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .body(correctAttemptBody(2004L))
                .when().post("/exercise-attempts")
                .then().statusCode(401);
    }

    @Test
    void shouldReturn200OnListByExercise() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("exerciseId", 2005L, "submittedAnswer", "resp", "correct", false, "baseXpReward", 30, "maxAttempts", 0, "feedback", ""))
                .when().post("/exercise-attempts")
                .then().statusCode(201);

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/exercise-attempts/by-exercise/2005")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class))
                .body("size()", greaterThan(0));
    }
}
