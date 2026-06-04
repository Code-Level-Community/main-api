package com.codelevel.module.integration.http.rest.routes;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class SocialMediaPostResourceIT {

    private String loginAsAdmin() {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "admin", "password", "admin"))
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("accessToken");
    }

    private String loginAsInstructor() {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "instructor", "password", "instructor"))
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("accessToken");
    }

    private String loginAsUser() {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "user", "password", "user"))
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("accessToken");
    }

    private Map<String, Object> validBody() {
        return Map.of(
                "platform", "INSTAGRAM",
                "postType", "NEW_COURSE",
                "content", "Novo curso de Quarkus com GraalVM disponível! Aprenda a construir aplicações nativas de alta performance."
        );
    }

    @Test
    void shouldReturn201OnCreateAsAdmin() {
        String token = loginAsAdmin();

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/integration/posts")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("platform", equalTo("INSTAGRAM"))
                .body("postType", equalTo("NEW_COURSE"))
                .body("status", equalTo("PENDING"));
    }

    @Test
    void shouldReturn403OnCreateAsInstructor() {
        String token = loginAsInstructor();

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/integration/posts")
                .then().statusCode(403);
    }

    @Test
    void shouldReturn403OnCreateAsUser() {
        String token = loginAsUser();

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/integration/posts")
                .then().statusCode(403);
    }

    @Test
    void shouldReturn401OnCreateWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/integration/posts")
                .then().statusCode(401);
    }

    @Test
    void shouldReturn422OnCreateWithBlankContent() {
        String token = loginAsAdmin();

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("platform", "INSTAGRAM", "postType", "NEW_COURSE", "content", ""))
                .when().post("/integration/posts")
                .then().statusCode(422);
    }

    @Test
    void shouldReturn200OnListAllAsAdmin() {
        String token = loginAsAdmin();

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/integration/posts")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void shouldReturn200OnListAllAsInstructor() {
        String token = loginAsInstructor();

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/integration/posts")
                .then().statusCode(200);
    }

    @Test
    void shouldReturn403OnListAllAsUser() {
        String token = loginAsUser();

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/integration/posts")
                .then().statusCode(403);
    }

    @Test
    void shouldReturn200OnGetByIdAsAdmin() {
        String token = loginAsAdmin();

        Long id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/integration/posts")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/integration/posts/" + id)
                .then().statusCode(200)
                .body("id", equalTo(id.intValue()));
    }

    @Test
    void shouldReturn404OnGetNonexistentPost() {
        String token = loginAsAdmin();

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/integration/posts/999999")
                .then().statusCode(404);
    }

    @Test
    void shouldReturn200OnPublishAsAdmin() {
        String token = loginAsAdmin();

        Long id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/integration/posts")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .when().post("/integration/posts/" + id + "/publish")
                .then().statusCode(200)
                .body("status", equalTo("POSTED"))
                .body("postedAt", notNullValue());
    }

    @Test
    void shouldReturn422OnPublishAlreadyPostedPost() {
        String token = loginAsAdmin();

        Long id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/integration/posts")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .when().post("/integration/posts/" + id + "/publish")
                .then().statusCode(200);

        given()
                .header("Authorization", "Bearer " + token)
                .when().post("/integration/posts/" + id + "/publish")
                .then().statusCode(422);
    }

    @Test
    void shouldReturn422OnRetryNonFailedPost() {
        String token = loginAsAdmin();

        Long id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/integration/posts")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .when().post("/integration/posts/" + id + "/retry")
                .then().statusCode(422);
    }

    @Test
    void shouldReturn204OnCancelPendingPost() {
        String token = loginAsAdmin();

        Long id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/integration/posts")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .when().delete("/integration/posts/" + id)
                .then().statusCode(204);
    }

    @Test
    void shouldReturn422OnCancelPublishedPost() {
        String token = loginAsAdmin();

        Long id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validBody())
                .when().post("/integration/posts")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .when().post("/integration/posts/" + id + "/publish")
                .then().statusCode(200);

        given()
                .header("Authorization", "Bearer " + token)
                .when().delete("/integration/posts/" + id)
                .then().statusCode(422);
    }

    @Test
    void shouldReturn200OnListByPlatformFilter() {
        String token = loginAsAdmin();

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("platform", "INSTAGRAM")
                .when().get("/integration/posts")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void shouldReturn200OnListByStatusFilter() {
        String token = loginAsAdmin();

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("status", "PENDING")
                .when().get("/integration/posts")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }
}