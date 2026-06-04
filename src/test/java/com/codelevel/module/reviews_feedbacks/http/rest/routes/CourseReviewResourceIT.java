package com.codelevel.module.reviews_feedbacks.http.rest.routes;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class CourseReviewResourceIT {

    private String loginAndGetToken(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().jsonPath().getString("accessToken");
    }

    private Long createCourse() {
        String token = loginAndGetToken("instructor", "instructor");
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "Course for Review Test",
                        "description", "A course used in review integration tests",
                        "thumbnailUrl", "https://img.example.com/review-test.jpg",
                        "difficultyLevel", "BEGINNER"
                ))
                .when().post("/course")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    private Map<String, Object> validReviewBody(Long courseId) {
        return Map.of(
                "courseId", courseId,
                "rating", 4,
                "isPositive", true,
                "comment", "Ótimo curso, aprendi muito!"
        );
    }

    @Test
    void shouldReturn201OnCreateReview() {
        Long courseId = createCourse();
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validReviewBody(courseId))
                .when().post("/course-review")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("courseId", equalTo(courseId.intValue()))
                .body("rating", equalTo(4))
                .body("isPositive", equalTo(true))
                .body("comment", equalTo("Ótimo curso, aprendi muito!"))
                .body("createdAt", notNullValue());
    }

    @Test
    void shouldReturn401OnCreateReviewWithoutToken() {
        Long courseId = createCourse();

        given()
                .contentType(ContentType.JSON)
                .body(validReviewBody(courseId))
                .when().post("/course-review")
                .then().statusCode(401);
    }

    @Test
    void shouldReturn422OnCreateReviewWithInvalidRating() {
        Long courseId = createCourse();
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", courseId, "rating", 6, "isPositive", true))
                .when().post("/course-review")
                .then().statusCode(422);
    }

    @Test
    void shouldReturn422OnCreateReviewWithRatingZero() {
        Long courseId = createCourse();
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", courseId, "rating", 0, "isPositive", false))
                .when().post("/course-review")
                .then().statusCode(422);
    }

    @Test
    void shouldReturn404OnCreateReviewWithNonexistentCourse() {
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("courseId", 999999, "rating", 4, "isPositive", true))
                .when().post("/course-review")
                .then().statusCode(404);
    }

    @Test
    void shouldReturn409OnDuplicateReview() {
        Long courseId = createCourse();
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validReviewBody(courseId))
                .when().post("/course-review")
                .then().statusCode(201);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validReviewBody(courseId))
                .when().post("/course-review")
                .then().statusCode(409);
    }

    @Test
    void shouldReturn200OnGetReviewById() {
        Long courseId = createCourse();
        String token = loginAndGetToken("user", "user");

        Long reviewId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validReviewBody(courseId))
                .when().post("/course-review")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .when().get("/course-review/" + reviewId)
                .then().statusCode(200)
                .body("id", equalTo(reviewId.intValue()));
    }

    @Test
    void shouldReturn404OnGetNonexistentReview() {
        given()
                .when().get("/course-review/999999")
                .then().statusCode(404);
    }

    @Test
    void shouldReturn200OnGetReviewsByCourse() {
        Long courseId = createCourse();
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validReviewBody(courseId))
                .when().post("/course-review")
                .then().statusCode(201);

        given()
                .when().get("/course-review/by-course/" + courseId)
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class))
                .body("size()", greaterThan(0));
    }

    @Test
    void shouldReturn200OnUpdateOwnReview() {
        Long courseId = createCourse();
        String token = loginAndGetToken("user", "user");

        Long reviewId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validReviewBody(courseId))
                .when().post("/course-review")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("rating", 5, "isPositive", true, "comment", "Melhor curso que já fiz!"))
                .when().put("/course-review/" + reviewId)
                .then().statusCode(200)
                .body("rating", equalTo(5))
                .body("comment", equalTo("Melhor curso que já fiz!"));
    }

    @Test
    void shouldReturn422OnUpdateAnotherUsersReview() {
        Long courseId = createCourse();
        String userToken = loginAndGetToken("user", "user");

        Long reviewId = given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(validReviewBody(courseId))
                .when().post("/course-review")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        String instructorToken = loginAndGetToken("instructor", "instructor");
        given()
                .header("Authorization", "Bearer " + instructorToken)
                .contentType(ContentType.JSON)
                .body(Map.of("rating", 1, "isPositive", false, "comment", "Tentando editar review alheio"))
                .when().put("/course-review/" + reviewId)
                .then().statusCode(422);
    }

    @Test
    void shouldReturn204OnDeleteOwnReview() {
        Long courseId = createCourse();
        String token = loginAndGetToken("user", "user");

        Long reviewId = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validReviewBody(courseId))
                .when().post("/course-review")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        given()
                .header("Authorization", "Bearer " + token)
                .when().delete("/course-review/" + reviewId)
                .then().statusCode(204);
    }

    @Test
    void shouldReturn422OnDeleteAnotherUsersReview() {
        Long courseId = createCourse();
        String userToken = loginAndGetToken("user", "user");

        Long reviewId = given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(validReviewBody(courseId))
                .when().post("/course-review")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        String instructorToken = loginAndGetToken("instructor", "instructor");
        given()
                .header("Authorization", "Bearer " + instructorToken)
                .when().delete("/course-review/" + reviewId)
                .then().statusCode(422);
    }

    @Test
    void shouldReturn204OnAdminDeleteAnyReview() {
        Long courseId = createCourse();
        String userToken = loginAndGetToken("user", "user");

        Long reviewId = given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body(validReviewBody(courseId))
                .when().post("/course-review")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        String adminToken = loginAndGetToken("admin", "admin");
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when().delete("/course-review/" + reviewId)
                .then().statusCode(204);
    }

    @Test
    void shouldReturn200OnGetByUser() {
        Long courseId = createCourse();
        String token = loginAndGetToken("user", "user");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(validReviewBody(courseId))
                .when().post("/course-review")
                .then().statusCode(201);

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/course-review/by-user")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }
}
