package com.codelevel.module.gamification.http.rest.routes;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class XpResourceIT {

    private static final String UID_1 = "00000000-0000-0000-0000-000000000001";
    private static final String UID_2 = "00000000-0000-0000-0000-000000000002";
    private static final String UID_3 = "00000000-0000-0000-0000-000000000003";

    private String loginAndGetToken(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("accessToken");
    }

    @Test
    void shouldReturn201OnAwardXp() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("userId", UID_1, "amount", 50, "source", "LESSON_COMPLETED",
                        "sourceId", 5001, "description", "Aula de teste"))
                .when().post("/gamification/xp/award")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("xpAmount", equalTo(50))
                .body("source", equalTo("LESSON_COMPLETED"))
                .body("leveledUp", notNullValue());
    }

    @Test
    void shouldReturn409OnDuplicateAward() {
        String token = loginAndGetToken("user", "user");

        Map<String, Object> body = Map.of("userId", UID_2, "amount", 30, "source", "EXERCISE_COMPLETED",
                "sourceId", 5002, "description", "Exercício");

        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
                .body(body).when().post("/gamification/xp/award").then().statusCode(201);

        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
                .body(body).when().post("/gamification/xp/award").then().statusCode(409);
    }

    @Test
    void shouldReturn401OnAwardWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("userId", UID_1, "amount", 50, "source", "LESSON_COMPLETED", "sourceId", 9999))
                .when().post("/gamification/xp/award")
                .then().statusCode(401);
    }

    @Test
    void shouldReturn200OnGetXpSummary() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/gamification/xp/users/" + UID_1)
                .then().statusCode(200)
                .body("userId", equalTo(UID_1))
                .body("totalXp", notNullValue());
    }

    @Test
    void shouldReturn200OnListTransactions() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("userId", UID_3, "amount", 25, "source", "LESSON_COMPLETED",
                        "sourceId", 5003, "description", "Aula"))
                .when().post("/gamification/xp/award").then().statusCode(201);

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/gamification/xp/users/" + UID_3 + "/transactions")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class))
                .body("size()", greaterThan(0));
    }

    @Test
    void shouldReturn200OnLeaderboard() {
        given()
                .when().get("/gamification/xp/leaderboard?limit=5")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void shouldReturn500OnInvalidSource() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("userId", UID_1, "amount", 50, "source", "INVALID_SOURCE", "sourceId", 1))
                .when().post("/gamification/xp/award")
                .then().statusCode(500);
    }
}
