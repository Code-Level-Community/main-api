package com.codelevel.module.community.http.rest.routes;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class QuestionResourceIT {

    private String loginAndGetToken(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("accessToken");
    }

    private Map<String, Object> validBody() {
        return Map.of(
                "courseId", 1,
                "title", "Como configurar o Spring Boot com Docker?",
                "content", "Estou tentando configurar minha aplicação Spring Boot para rodar em um container Docker, mas não consigo conectar ao banco de dados."
        );
    }

    @Test
    void shouldReturn201OnCreateAsUser() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/community/questions")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("upVotes", equalTo(0))
                .body("downVotes", equalTo(0))
                .body("answersCount", equalTo(0));
    }

    @Test
    void shouldReturn401OnCreateWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/community/questions")
                .then().statusCode(401);
    }

    @Test
    void shouldReturn422OnCreateWithInvalidTitle() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", 1, "title", "Abc", "content", "Conteúdo da pergunta com mais de vinte caracteres."))
                .when().post("/community/questions")
                .then().statusCode(422);
    }

    @Test
    void shouldReturn422OnCreateWithShortContent() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", 1, "title", "Pergunta válida aqui?", "content", "Curto demais"))
                .when().post("/community/questions")
                .then().statusCode(422);
    }

    @Test
    void shouldReturn200OnListAll() {
        given()
                .when().get("/community/questions")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void shouldReturn200OnListByCourse() {
        given()
                .queryParam("courseId", 1)
                .when().get("/community/questions")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void shouldReturn200OnGetById() {
        String token = loginAndGetToken("user", "user");

        Long id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/community/questions")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .when().get("/community/questions/" + id)
                .then().statusCode(200)
                .body("id", equalTo(id.intValue()));
    }

    @Test
    void shouldReturn404OnGetNonexistent() {
        given()
                .when().get("/community/questions/999999")
                .then().statusCode(404);
    }

    @Test
    void shouldReturn200OnUpvote() {
        String token = loginAndGetToken("user", "user");

        Long id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/community/questions")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        String voterToken = loginAndGetToken("instructor", "instructor");

        given()
                .header("Authorization", "Bearer " + voterToken)
                .contentType(ContentType.JSON)
                .body(Map.of("voteType", "UPVOTE"))
                .when().post("/community/questions/" + id + "/vote")
                .then().statusCode(200)
                .body("upVotes", equalTo(1));
    }

    @Test
    void shouldReturn409OnDuplicateVote() {
        String token = loginAndGetToken("user", "user");

        Long id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/community/questions")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        String voterToken = loginAndGetToken("instructor", "instructor");

        given()
                .header("Authorization", "Bearer " + voterToken)
                .contentType(ContentType.JSON)
                .body(Map.of("voteType", "UPVOTE"))
                .when().post("/community/questions/" + id + "/vote")
                .then().statusCode(200);

        given()
                .header("Authorization", "Bearer " + voterToken)
                .contentType(ContentType.JSON)
                .body(Map.of("voteType", "UPVOTE"))
                .when().post("/community/questions/" + id + "/vote")
                .then().statusCode(409);
    }

    @Test
    void shouldReturn204OnDeleteByOwner() {
        String token = loginAndGetToken("user", "user");

        Long id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/community/questions")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .when().delete("/community/questions/" + id)
                .then().statusCode(204);
    }

    @Test
    void shouldReturn422OnDeleteByNonOwner() {
        String token = loginAndGetToken("user", "user");

        Long id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/community/questions")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        String otherToken = loginAndGetToken("instructor", "instructor");

        given()
                .header("Authorization", "Bearer " + otherToken)
                .when().delete("/community/questions/" + id)
                .then().statusCode(422);
    }
}
