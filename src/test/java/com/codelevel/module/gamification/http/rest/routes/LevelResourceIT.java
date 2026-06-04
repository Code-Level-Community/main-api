package com.codelevel.module.gamification.http.rest.routes;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class LevelResourceIT {

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
                .body(Map.of("name", "Aprendiz", "xpRequired", 100))
                .when().post("/gamification/levels")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Aprendiz"))
                .body("xpRequired", equalTo(100));
    }

    @Test
    void shouldReturn403OnCreateByUser() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Negado", "xpRequired", 999))
                .when().post("/gamification/levels")
                .then().statusCode(403);
    }

    @Test
    void shouldReturn401OnCreateWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Sem Token", "xpRequired", 50))
                .when().post("/gamification/levels")
                .then().statusCode(401);
    }

    @Test
    void shouldReturn200OnListLevels() {
        given()
                .when().get("/gamification/levels")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void shouldReturn422OnNegativeXpRequired() {
        String token = loginAndGetToken("admin", "admin");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Inválido", "xpRequired", -1))
                .when().post("/gamification/levels")
                .then().statusCode(422);
    }
}
