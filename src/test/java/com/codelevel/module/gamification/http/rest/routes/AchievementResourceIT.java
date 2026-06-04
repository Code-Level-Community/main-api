package com.codelevel.module.gamification.http.rest.routes;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class AchievementResourceIT {

    private String loginAndGetToken(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("accessToken");
    }

    @Test
    void shouldReturn201OnCreateByAdmin() {
        String token = loginAndGetToken("admin", "admin");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "Primeira Aula",
                        "slug", "primeira-aula-it",
                        "triggerType", "LESSONS_COMPLETED",
                        "triggerCriteria", "1",
                        "xpReward", 50
                ))
                .when().post("/gamification/achievements")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("slug", equalTo("primeira-aula-it"))
                .body("xpReward", equalTo(50));
    }

    @Test
    void shouldReturn403OnCreateByUser() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Negado", "slug", "negado-slug",
                        "triggerType", "LESSONS_COMPLETED", "triggerCriteria", "1", "xpReward", 0))
                .when().post("/gamification/achievements")
                .then().statusCode(403);
    }

    @Test
    void shouldReturn200OnListAchievements() {
        given()
                .when().get("/gamification/achievements")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void shouldReturn200OnGetUserAchievements() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/gamification/achievements/users/00000000-0000-0000-0000-000000000001")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void shouldReturn200OnCheckAndUnlock() {
        String adminToken = loginAndGetToken("admin", "admin");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "Check Test Achievement",
                        "slug", "check-test-achv",
                        "triggerType", "LESSONS_COMPLETED",
                        "triggerCriteria", "1",
                        "xpReward", 0
                ))
                .when().post("/gamification/achievements").then().statusCode(201);

        String userToken = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(Map.of("userId", "00000000-0000-0000-0000-000000000001", "triggerType", "LESSONS_COMPLETED", "currentValue", 1))
                .when().post("/gamification/achievements/check")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void shouldReturn409OnDuplicateSlug() {
        String token = loginAndGetToken("admin", "admin");

        Map<String, Object> body = Map.of(
                "name", "Dup Slug", "slug", "dup-slug-achv",
                "triggerType", "LESSONS_COMPLETED", "triggerCriteria", "1", "xpReward", 0
        );

        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
                .body(body).when().post("/gamification/achievements").then().statusCode(201);

        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
                .body(body).when().post("/gamification/achievements").then().statusCode(409);
    }
}
