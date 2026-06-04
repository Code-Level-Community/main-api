package com.codelevel.module.course.http.rest.routes;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class ModuleResourceIT {

    private String loginAndGetToken(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("accessToken");
    }

    private Long createCourse(String token) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Test Course", "description", "Test course description here", "thumbnailUrl", "http://img.com/t.jpg", "difficultyLevel", "BEGINNER"))
                .when().post("/course")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    @Test
    void shouldReturn201OnCreateModule() {
        String token = loginAndGetToken("instructor", "instructor");
        Long courseId = createCourse(token);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Module 1", "description", "First module", "orderPosition", 1))
                .when().post("/course/" + courseId + "/module")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("title", equalTo("Module 1"))
                .body("courseId", equalTo(courseId.intValue()));
    }

    @Test
    void shouldReturn401OnCreateModuleWithoutToken() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Module", "description", "Desc", "orderPosition", 1))
                .when().post("/course/1/module")
                .then().statusCode(401);
    }

    @Test
    void shouldReturn200OnGetModulesByCourse() {
        String token = loginAndGetToken("instructor", "instructor");
        Long courseId = createCourse(token);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Module A", "description", "Desc", "orderPosition", 1))
                .when().post("/course/" + courseId + "/module")
                .then().statusCode(201);

        given()
                .when().get("/course/" + courseId + "/module")
                .then().statusCode(200)
                .body("size()", greaterThan(0));
    }

    @Test
    void shouldReturn200OnGetModuleById() {
        String token = loginAndGetToken("instructor", "instructor");
        Long courseId = createCourse(token);

        Long moduleId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Module X", "description", "Desc", "orderPosition", 1))
                .when().post("/course/" + courseId + "/module")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .when().get("/course/" + courseId + "/module/" + moduleId)
                .then().statusCode(200)
                .body("id", equalTo(moduleId.intValue()));
    }

    @Test
    void shouldReturn404OnGetNonexistentModule() {
        given()
                .when().get("/course/1/module/999999")
                .then().statusCode(404);
    }

    @Test
    void shouldReturn200OnUpdateModule() {
        String token = loginAndGetToken("instructor", "instructor");
        Long courseId = createCourse(token);

        Long moduleId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "Old Title", "description", "Old desc", "orderPosition", 1))
                .when().post("/course/" + courseId + "/module")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "New Title", "description", "New desc", "orderPosition", 2))
                .when().put("/course/" + courseId + "/module/" + moduleId)
                .then().statusCode(200)
                .body("title", equalTo("New Title"));
    }

    @Test
    void shouldReturn204OnDeleteModule() {
        String token = loginAndGetToken("admin", "admin");
        Long courseId = createCourse(token);

        Long moduleId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("title", "To Delete", "description", "Desc", "orderPosition", 1))
                .when().post("/course/" + courseId + "/module")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .when().delete("/course/" + courseId + "/module/" + moduleId)
                .then().statusCode(204);
    }
}
