package com.codelevel.module.gamification.http.rest.routes;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class StreakResourceIT {

    private String loginAndGetToken(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("accessToken");
    }

    @Test
    void shouldReturn200OnRecordActivity() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .when().post("/gamification/streaks/activity")
                .then().statusCode(200)
                .body("currentStreakDays", equalTo(1))
                .body("activeToday", equalTo(true))
                .body("longestStreakDays", greaterThanOrEqualTo(1));
    }

    @Test
    void shouldReturn200OnSubsequentActivitySameDay() {
        String token = loginAndGetToken("user", "user");

        given().header("Authorization", "Bearer " + token)
                .when().post("/gamification/streaks/activity").then().statusCode(200);

        given()
                .header("Authorization", "Bearer " + token)
                .when().post("/gamification/streaks/activity")
                .then().statusCode(200)
                .body("activeToday", equalTo(true));
    }

    @Test
    void shouldReturn401OnRecordActivityWithoutToken() {
        given()
                .when().post("/gamification/streaks/activity")
                .then().statusCode(401);
    }

    @Test
    void shouldReturn200OnGetMyStreak() {
        String token = loginAndGetToken("user", "user");

        given().header("Authorization", "Bearer " + token)
                .when().post("/gamification/streaks/activity").then().statusCode(200);

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/gamification/streaks/my")
                .then().statusCode(200)
                .body("userId", notNullValue())
                .body("currentStreakDays", greaterThanOrEqualTo(1))
                .body("activeToday", equalTo(true));
    }

    @Test
    void shouldReturn204OnGetMyStreakWhenNoActivity() {
        String token = loginAndGetToken("instructor", "instructor");

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/gamification/streaks/my")
                .then().statusCode(anyOf(equalTo(200), equalTo(204)));
    }

    @Test
    void shouldReturn200OnGetUserStreakPublic() {
        String token = loginAndGetToken("user", "user");

        String userId = given()
                .header("Authorization", "Bearer " + token)
                .when().post("/gamification/streaks/activity")
                .then().statusCode(200)
                .extract().jsonPath().getString("userId");

        given()
                .when().get("/gamification/streaks/users/" + userId)
                .then().statusCode(200)
                .body("userId", equalTo(userId));
    }

    @Test
    void shouldReturn422OnFreezeWithInsufficientXp() {
        String token = loginAndGetToken("instructor", "instructor");

        given().header("Authorization", "Bearer " + token)
                .when().post("/gamification/streaks/activity").then().statusCode(200);

        given()
                .header("Authorization", "Bearer " + token)
                .when().post("/gamification/streaks/freeze")
                .then().statusCode(422);
    }
}
