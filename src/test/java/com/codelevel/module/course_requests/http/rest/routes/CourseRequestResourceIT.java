package com.codelevel.module.course_requests.http.rest.routes;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class CourseRequestResourceIT {

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
                "title", "Curso de Kotlin do zero ao avançado",
                "description", "Gostaria de ver um curso completo de Kotlin com foco em backend.",
                "categoryId", 1
        );
    }

    @Test
    void shouldReturn201OnCreateAsUser() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/course-request")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("status", equalTo("PENDING"))
                .body("upvotes", equalTo(0))
                .body("downvotes", equalTo(0));
    }

    @Test
    void shouldReturn201OnCreateAsInstructor() {
        String token = loginAndGetToken("instructor", "instructor");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/course-request")
                .then().statusCode(201);
    }

    @Test
    void shouldReturn401OnCreateWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/course-request")
                .then().statusCode(401);
    }

    @Test
    void shouldReturn422OnCreateWithBlankTitle() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "", "description", "Descrição válida do curso solicitado.", "categoryId", 1))
                .when().post("/course-request")
                .then().statusCode(422);
    }

    @Test
    void shouldReturn200OnListAll() {
        given()
                .when().get("/course-request")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void shouldReturn200OnListByStatus() {
        given()
                .queryParam("status", "PENDING")
                .when().get("/course-request")
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
                .when().post("/course-request")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .when().get("/course-request/" + id)
                .then().statusCode(200)
                .body("id", equalTo(id.intValue()));
    }

    @Test
    void shouldReturn404OnGetNonexistent() {
        given()
                .when().get("/course-request/999999")
                .then().statusCode(404);
    }

    @Test
    void shouldReturn200OnVoteAsUser() {
        String token = loginAndGetToken("user", "user");

        Long id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/course-request")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        String voterToken = loginAndGetToken("instructor", "instructor");

        given()
                .header("Authorization", "Bearer " + voterToken)
                .contentType(ContentType.JSON)
                .body(Map.of("voteType", "UPVOTE"))
                .when().post("/course-request/" + id + "/vote")
                .then().statusCode(200)
                .body("upvotes", equalTo(1));
    }

    @Test
    void shouldReturn401OnVoteWithoutToken() {
        String token = loginAndGetToken("user", "user");

        Long id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/course-request")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("voteType", "UPVOTE"))
                .when().post("/course-request/" + id + "/vote")
                .then().statusCode(401);
    }

    @Test
    void shouldReturn200OnRejectAsAdmin() {
        String token = loginAndGetToken("user", "user");

        Long id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/course-request")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        String adminToken = loginAndGetToken("admin", "admin");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .when().post("/course-request/" + id + "/reject")
                .then().statusCode(200)
                .body("status", equalTo("REJECTED"));
    }

    @Test
    void shouldReturn403OnRejectAsUser() {
        String token = loginAndGetToken("user", "user");

        Long id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/course-request")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when().post("/course-request/" + id + "/reject")
                .then().statusCode(403);
    }

    @Test
    void shouldReturn422OnAcceptNonApprovedRequest() {
        String token = loginAndGetToken("user", "user");

        Long id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/course-request")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        String instructorToken = loginAndGetToken("instructor", "instructor");

        given()
                .header("Authorization", "Bearer " + instructorToken)
                .contentType(ContentType.JSON)
                .when().post("/course-request/" + id + "/accept")
                .then().statusCode(422);
    }

    @Test
    void shouldReturn403OnAcceptAsUser() {
        String token = loginAndGetToken("user", "user");

        Long id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/course-request")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when().post("/course-request/" + id + "/accept")
                .then().statusCode(403);
    }
}