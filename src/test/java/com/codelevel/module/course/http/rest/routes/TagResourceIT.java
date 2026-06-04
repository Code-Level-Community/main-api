package com.codelevel.module.course.http.rest.routes;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class TagResourceIT {

    private String loginAndGetToken(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("accessToken");
    }

    @Test
    void shouldReturn201OnCreateTagAsAdmin() {
        String token = loginAndGetToken("admin", "admin");
        String slug = "java-" + UUID.randomUUID().toString().substring(0, 8);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Java", "slug", slug))
                .when().post("/tag")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Java"))
                .body("slug", equalTo(slug));
    }

    @Test
    void shouldReturn401OnCreateTagWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Test", "slug", "test"))
                .when().post("/tag")
                .then().statusCode(401);
    }

    @Test
    void shouldReturn403OnCreateTagAsUser() {
        String token = loginAndGetToken("user", "user");
        String slug = "test-" + UUID.randomUUID().toString().substring(0, 8);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Test", "slug", slug))
                .when().post("/tag")
                .then().statusCode(403);
    }

    @Test
    void shouldReturn200OnGetAllTags() {
        given()
                .when().get("/tag")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void shouldReturn200OnGetTagById() {
        String token = loginAndGetToken("admin", "admin");
        String slug = "spring-" + UUID.randomUUID().toString().substring(0, 8);

        Long id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Spring", "slug", slug))
                .when().post("/tag")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .when().get("/tag/" + id)
                .then().statusCode(200)
                .body("id", equalTo(id.intValue()))
                .body("name", equalTo("Spring"));
    }

    @Test
    void shouldReturn404OnGetNonexistentTag() {
        given()
                .when().get("/tag/999999")
                .then().statusCode(404);
    }

    @Test
    void shouldReturn409OnDuplicateTagSlug() {
        String token = loginAndGetToken("admin", "admin");
        String slug = "kotlin-" + UUID.randomUUID().toString().substring(0, 8);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Kotlin", "slug", slug))
                .when().post("/tag")
                .then().statusCode(201);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Kotlin Lang", "slug", slug))
                .when().post("/tag")
                .then().statusCode(409);
    }
}
