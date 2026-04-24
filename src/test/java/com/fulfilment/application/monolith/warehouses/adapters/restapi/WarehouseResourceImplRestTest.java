package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link WarehouseResourceImpl} via HTTP to improve coverage of REST wiring,
 * status mapping, and null-stock handling.
 */
@QuarkusTest
class WarehouseResourceImplRestTest {

  @Inject EntityManager em;

  @BeforeEach
  @Transactional
  void seedWarehousesForRestLayer() {
    em.createQuery("DELETE FROM DbWarehouse w WHERE w.businessUnitCode LIKE 'REST-%'").executeUpdate();

    insert("REST-LIST", "TILBURG-001", 25, 5);
    insert("REST-GET", "ZWOLLE-002", 20, 4);
    insert("REST-ARC", "HELMOND-001", 40, 10);
    insert("REST-REP", "EINDHOVEN-001", 50, 15);
    em.flush();
  }

  private void insert(String code, String location, int capacity, int stock) {
    DbWarehouse w = new DbWarehouse();
    w.businessUnitCode = code;
    w.location = location;
    w.capacity = capacity;
    w.stock = stock;
    w.createdAt = LocalDateTime.now().minusHours(2);
    w.archivedAt = null;
    em.persist(w);
  }

  @Test
  void listWarehousesIncludesSeededRows() {
    given().when().get("/warehouse").then().statusCode(200).body(containsString("REST-LIST"));
  }

  @Test
  void getWarehouseReturns200() {
    given()
        .when()
        .get("/warehouse/REST-GET")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("REST-GET"));
  }

  @Test
  void getWarehouseReturns404WhenMissing() {
    given().when().get("/warehouse/REST-NO-SUCH").then().statusCode(404);
  }

  @Test
  void createWarehouseReturns400WhenInvalid() {
    given()
        .contentType("application/json")
        .body(
            "{\"businessUnitCode\":\"REST-BAD\",\"location\":\"INVALID-LOC\",\"capacity\":10,\"stock\":1}")
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400);
  }

  @Test
  void createWarehouseDefaultsNullStockToZero() {
    given()
        .contentType("application/json")
        .body("{\"businessUnitCode\":\"REST-NOSTK\",\"location\":\"VETSBY-001\",\"capacity\":30}")
        .when()
        .post("/warehouse")
        .then()
        .statusCode(200)
        .body("stock", equalTo(0));
  }

  @Test
  void archiveMissingWarehouseReturns404() {
    given().when().delete("/warehouse/REST-MISSING").then().statusCode(404);
  }

  @Test
  void archiveWarehouseReturns204ThenReArchiveFailsWith400() {
    given().when().delete("/warehouse/REST-ARC").then().statusCode(204);
    given().when().delete("/warehouse/REST-ARC").then().statusCode(400);
  }

  @Test
  void replaceWarehouseReturns200() {
    given()
        .contentType("application/json")
        .body("{\"location\":\"ZWOLLE-001\",\"capacity\":30,\"stock\":10}")
        .when()
        .post("/warehouse/REST-REP/replacement")
        .then()
        .statusCode(200)
        .body("location", equalTo("ZWOLLE-001"))
        .body("capacity", equalTo(30));
  }

  @Test
  void replaceWarehouseReturns400WhenInvalid() {
    given()
        .contentType("application/json")
        .body("{\"location\":\"INVALID\",\"capacity\":10,\"stock\":1}")
        .when()
        .post("/warehouse/REST-REP/replacement")
        .then()
        .statusCode(400);
  }
}
