package com.codelevel.module.community.http.rest.routes;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class AnswerResourceIT {

    private String loginAndGetToken(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("accessToken");
    }

    private Long createQuestion(String token) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "courseId", 1,
                        "title", "Como configurar o Spring Boot com Docker?",
                        "content", "Estou tentando configurar minha aplicação Spring Boot para rodar em container Docker."
                ))
                .when().post("/community/questions")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    @Test
    void shouldReturn201OnCreateAsUser() {
        String token = loginAndGetToken("user", "user");
        Long questionId = createQuestion(token);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("questionId", questionId, "content", "Você precisa configurar o host do banco para o nome do serviço Docker no compose."))
                .when().post("/community/answers")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("questionId", equalTo(questionId.intValue()))
                .body("accepted", equalTo(false))
                .body("upVotes", equalTo(0));
    }

    @Test
    void shouldReturn401OnCreateWithoutToken() {
        String token = loginAndGetToken("user", "user");
        Long questionId = createQuestion(token);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("questionId", questionId, "content", "Resposta sem autenticação."))
                .when().post("/community/answers")
                .then().statusCode(401);
    }

    @Test
    void shouldReturn422OnCreateWithShortContent() {
        String token = loginAndGetToken("user", "user");
        Long questionId = createQuestion(token);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("questionId", questionId, "content", "Curto"))
                .when().post("/community/answers")
                .then().statusCode(422);
    }

    @Test
    void shouldReturn200OnListByQuestion() {
        String token = loginAndGetToken("user", "user");
        Long questionId = createQuestion(token);

        given()
                .when().get("/community/answers/by-question/" + questionId)
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void shouldReturn200OnGetById() {
        String token = loginAndGetToken("user", "user");
        Long questionId = createQuestion(token);

        Long answerId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("questionId", questionId, "content", "Você precisa configurar o host do banco para o serviço Docker no compose."))
                .when().post("/community/answers")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .when().get("/community/answers/" + answerId)
                .then().statusCode(200)
                .body("id", equalTo(answerId.intValue()));
    }

    @Test
    void shouldReturn404OnGetNonexistent() {
        given()
                .when().get("/community/answers/999999")
                .then().statusCode(404);
    }

    @Test
    void shouldReturn200OnUpvoteAnswer() {
        String token = loginAndGetToken("user", "user");
        Long questionId = createQuestion(token);

        Long answerId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("questionId", questionId, "content", "Você precisa configurar o host do banco para o serviço Docker no compose."))
                .when().post("/community/answers")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        String voterToken = loginAndGetToken("instructor", "instructor");

        given()
                .header("Authorization", "Bearer " + voterToken)
                .contentType(ContentType.JSON)
                .body(Map.of("voteType", "UPVOTE"))
                .when().post("/community/answers/" + answerId + "/vote")
                .then().statusCode(200)
                .body("upVotes", equalTo(1));
    }

    @Test
    void shouldReturn200OnAcceptAnswerByQuestionOwner() {
        String token = loginAndGetToken("user", "user");
        Long questionId = createQuestion(token);

        String instructorToken = loginAndGetToken("instructor", "instructor");

        Long answerId = given()
                .header("Authorization", "Bearer " + instructorToken)
                .contentType(ContentType.JSON)
                .body(Map.of("questionId", questionId, "content", "Você precisa configurar o host do banco para o serviço Docker no compose."))
                .when().post("/community/answers")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when().post("/community/answers/" + answerId + "/accept")
                .then().statusCode(200)
                .body("accepted", equalTo(true));
    }

    @Test
    void shouldReturn422OnAcceptAnswerByNonOwner() {
        String token = loginAndGetToken("user", "user");
        Long questionId = createQuestion(token);

        String instructorToken = loginAndGetToken("instructor", "instructor");

        Long answerId = given()
                .header("Authorization", "Bearer " + instructorToken)
                .contentType(ContentType.JSON)
                .body(Map.of("questionId", questionId, "content", "Você precisa configurar o host do banco para o serviço Docker no compose."))
                .when().post("/community/answers")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + instructorToken)
                .contentType(ContentType.JSON)
                .when().post("/community/answers/" + answerId + "/accept")
                .then().statusCode(422);
    }

    @Test
    void shouldReturn204OnDeleteByOwner() {
        String token = loginAndGetToken("user", "user");
        Long questionId = createQuestion(token);

        Long answerId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("questionId", questionId, "content", "Você precisa configurar o host do banco para o serviço Docker no compose."))
                .when().post("/community/answers")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .when().delete("/community/answers/" + answerId)
                .then().statusCode(204);
    }

    @Test
    void shouldReturn422OnDeleteByNonOwner() {
        String token = loginAndGetToken("user", "user");
        Long questionId = createQuestion(token);

        Long answerId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("questionId", questionId, "content", "Você precisa configurar o host do banco para o serviço Docker no compose."))
                .when().post("/community/answers")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        String otherToken = loginAndGetToken("instructor", "instructor");

        given()
                .header("Authorization", "Bearer " + otherToken)
                .when().delete("/community/answers/" + answerId)
                .then().statusCode(422);
    }
}
