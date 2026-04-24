package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WarehouseSearchResourceTest {

  static final String SEED_AMS = "SRCH-SEED-AMS";

  @Inject EntityManager em;

  @BeforeEach
  @Transactional
  void seedAmsterdamWarehouseForSearchTests() {
    em.createQuery("DELETE FROM DbWarehouse w WHERE w.location = :loc")
        .setParameter("loc", "AMSTERDAM-001")
        .executeUpdate();

    DbWarehouse row = new DbWarehouse();
    row.businessUnitCode = SEED_AMS;
    row.location = "AMSTERDAM-001";
    row.capacity = 50;
    row.stock = 5;
    row.createdAt = LocalDateTime.now().minusDays(1);
    row.archivedAt = null;
    em.persist(row);
    em.flush();
  }

  @Test
  void searchByLocationReturnsSeededWarehouse() {
    given()
        .queryParam("location", "AMSTERDAM-001")
        .when()
        .get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("totalCount", equalTo(1))
        .body("items[0].businessUnitCode", equalTo(SEED_AMS));
  }

  @Test
  void invalidSortByReturns400() {
    given()
        .queryParam("sortBy", "weight")
        .when()
        .get("/warehouse/search")
        .then()
        .statusCode(400);
  }

  @Test
  void pageSizeIsCappedAtOneHundred() {
    given()
        .queryParam("location", "ZWOLLE-001")
        .queryParam("pageSize", 999)
        .when()
        .get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("pageSize", equalTo(100));
  }

  @Test
  void minCapacityFilterNarrowsResults() {
    given()
        .queryParam("location", "AMSTERDAM-001")
        .queryParam("minCapacity", 60)
        .when()
        .get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("totalCount", equalTo(0));
  }
}
