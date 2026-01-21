package com.enterprise.testgen.template;

/**
 * Rest Assured code templates for API test automation.
 * Uses Rest Assured 5.4.0 (LTE/stable version).
 */
public class RestAssuredTemplates {

    public static final String RESTASSURED_VERSION = "5.4.0";

    /**
     * Base API Test class template
     */
    public static final String BASE_API_TEST_TEMPLATE = """
            package %s.tests;

            import io.restassured.RestAssured;
            import io.restassured.http.ContentType;
            import io.restassured.response.Response;
            import io.restassured.specification.RequestSpecification;
            import org.junit.jupiter.api.BeforeAll;
            import org.junit.jupiter.api.BeforeEach;
            import static io.restassured.RestAssured.*;
            import static org.hamcrest.Matchers.*;

            /**
             * Base API Test class providing Rest Assured configuration.
             * All API test classes should extend this class.
             */
            public abstract class BaseApiTest {

                protected RequestSpecification requestSpec;
                protected static final String BASE_URI = "%s";

                @BeforeAll
                public static void globalSetup() {
                    RestAssured.baseURI = BASE_URI;
                    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
                }

                @BeforeEach
                public void setUp() {
                    requestSpec = given()
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .log().all();
                }

                protected Response sendGet(String endpoint) {
                    return requestSpec
                        .when()
                        .get(endpoint)
                        .then()
                        .log().all()
                        .extract().response();
                }

                protected Response sendPost(String endpoint, Object body) {
                    return requestSpec
                        .body(body)
                        .when()
                        .post(endpoint)
                        .then()
                        .log().all()
                        .extract().response();
                }

                protected Response sendPut(String endpoint, Object body) {
                    return requestSpec
                        .body(body)
                        .when()
                        .put(endpoint)
                        .then()
                        .log().all()
                        .extract().response();
                }

                protected Response sendDelete(String endpoint) {
                    return requestSpec
                        .when()
                        .delete(endpoint)
                        .then()
                        .log().all()
                        .extract().response();
                }

                protected Response sendPatch(String endpoint, Object body) {
                    return requestSpec
                        .body(body)
                        .when()
                        .patch(endpoint)
                        .then()
                        .log().all()
                        .extract().response();
                }

                protected RequestSpecification withAuth(String token) {
                    return requestSpec.header("Authorization", "Bearer " + token);
                }

                protected RequestSpecification withQueryParam(String key, Object value) {
                    return requestSpec.queryParam(key, value);
                }
            }
            """;

    /**
     * API Test class template
     */
    public static final String API_TEST_TEMPLATE = """
            package %s.tests;

            import io.restassured.response.Response;
            import org.junit.jupiter.api.Test;
            import org.junit.jupiter.api.DisplayName;
            import static io.restassured.RestAssured.*;
            import static org.hamcrest.Matchers.*;
            import static org.junit.jupiter.api.Assertions.*;

            /**
             * Test Class: %s
             * Test Type: API Test - Rest Assured
             *
             * Scenario: %s
             *
             * Preconditions:
             * %s
             */
            public class %s extends BaseApiTest {

            %s
            }
            """;

    /**
     * GET request test method template
     */
    public static final String GET_TEST_TEMPLATE = """
                /**
                 * Test: %s
                 *
                 * Test Steps:
                 * %s
                 *
                 * Expected Results:
                 * %s
                 */
                @Test
                @DisplayName("%s")
                public void %s() {
                    // Arrange
                    String endpoint = "%s";

                    // Act
                    Response response = given()
                        .spec(requestSpec)
                        %s
                        .when()
                        .get(endpoint);

                    // Assert
                    response.then()
                        .statusCode(%d)
                        %s;
                }
            """;

    /**
     * POST request test method template
     */
    public static final String POST_TEST_TEMPLATE = """
                /**
                 * Test: %s
                 *
                 * Test Steps:
                 * %s
                 *
                 * Expected Results:
                 * %s
                 */
                @Test
                @DisplayName("%s")
                public void %s() {
                    // Arrange
                    String endpoint = "%s";
                    String requestBody = \"\"\"
                        %s
                        \"\"\";

                    // Act
                    Response response = given()
                        .spec(requestSpec)
                        .body(requestBody)
                        .when()
                        .post(endpoint);

                    // Assert
                    response.then()
                        .statusCode(%d)
                        %s;
                }
            """;

    /**
     * PUT request test method template
     */
    public static final String PUT_TEST_TEMPLATE = """
                /**
                 * Test: %s
                 *
                 * Test Steps:
                 * %s
                 *
                 * Expected Results:
                 * %s
                 */
                @Test
                @DisplayName("%s")
                public void %s() {
                    // Arrange
                    String endpoint = "%s";
                    String requestBody = \"\"\"
                        %s
                        \"\"\";

                    // Act
                    Response response = given()
                        .spec(requestSpec)
                        .body(requestBody)
                        .when()
                        .put(endpoint);

                    // Assert
                    response.then()
                        .statusCode(%d)
                        %s;
                }
            """;

    /**
     * DELETE request test method template
     */
    public static final String DELETE_TEST_TEMPLATE = """
                /**
                 * Test: %s
                 *
                 * Test Steps:
                 * %s
                 *
                 * Expected Results:
                 * %s
                 */
                @Test
                @DisplayName("%s")
                public void %s() {
                    // Arrange
                    String endpoint = "%s";

                    // Act
                    Response response = given()
                        .spec(requestSpec)
                        .when()
                        .delete(endpoint);

                    // Assert
                    response.then()
                        .statusCode(%d)
                        %s;
                }
            """;

    /**
     * POJO model class template for request/response bodies
     */
    public static final String POJO_TEMPLATE = """
            package %s.models;

            import com.fasterxml.jackson.annotation.JsonProperty;
            import lombok.Data;
            import lombok.Builder;
            import lombok.NoArgsConstructor;
            import lombok.AllArgsConstructor;

            /**
             * Model class for %s.
             */
            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public class %s {

            %s
            }
            """;

    private RestAssuredTemplates() {
        // Utility class
    }
}
