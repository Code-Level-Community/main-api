package com.codelevel.module.course.http.rest.routes;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class CategoryResourceIT {

    private String loginAndGetToken(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("accessToken");
    }

    @Test
    void shouldReturn201OnCreateCategoryAsAdmin() {
        String token = loginAndGetToken("admin", "admin");
        String slug = "backend-" + UUID.randomUUID().toString().substring(0, 8);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Backend", "slug", slug, "iconUrl", "https://icon.com/b.svg"))
                .when().post("/category")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Backend"))
                .body("slug", equalTo(slug));
    }

    @Test
    void shouldReturn401OnCreateCategoryWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Test", "slug", "test"))
                .when().post("/category")
                .then().statusCode(401);
    }

    @Test
    void shouldReturn403OnCreateCategoryAsUser() {
        String token = loginAndGetToken("user", "user");
        String slug = "test-" + UUID.randomUUID().toString().substring(0, 8);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Test", "slug", slug))
                .when().post("/category")
                .then().statusCode(403);
    }

    @Test
    void shouldReturn200OnGetAllCategories() {
        given()
                .when().get("/category")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void shouldReturn200OnGetCategoryById() {
        String token = loginAndGetToken("admin", "admin");
        String slug = "devops-" + UUID.randomUUID().toString().substring(0, 8);

        Long id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("name", "DevOps", "slug", slug))
                .when().post("/category")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .when().get("/category/" + id)
                .then().statusCode(200)
                .body("id", equalTo(id.intValue()))
                .body("name", equalTo("DevOps"));
    }

    @Test
    void shouldReturn404OnGetNonexistentCategory() {
        given()
                .when().get("/category/999999")
                .then().statusCode(404);
    }

    @Test
    void shouldReturn409OnDuplicateSlug() {
        String token = loginAndGetToken("admin", "admin");
        String slug = "ml-" + UUID.randomUUID().toString().substring(0, 8);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("name", "ML", "slug", slug))
                .when().post("/category")
                .then().statusCode(201);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Machine Learning", "slug", slug))
                .when().post("/category")
                .then().statusCode(409);
    }
}
