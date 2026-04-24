package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class WarehouseRepositoryTest {

  @Inject WarehouseRepository warehouseRepository;

  @Inject EntityManager em;

  @BeforeEach
  @Transactional
  void removeScratchRows() {
    em.createQuery("DELETE FROM DbWarehouse w WHERE w.businessUnitCode LIKE 'REPO-%'")
        .executeUpdate();
  }

  @Test
  @Transactional
  void searchExcludesArchivedWarehouses() {
    persistWarehouse("REPO-ACTIVE", "AMSTERDAM-001", 40, 10, null);
    persistWarehouse("REPO-ARCH", "AMSTERDAM-001", 40, 10, LocalDateTime.now().minusDays(1));

    WarehouseSearchQueryResult result =
        warehouseRepository.searchActiveWarehouses(
            "AMSTERDAM-001", null, null, "createdAt", false, 0, 10);

    assertTrue(result.items().stream().anyMatch(w -> "REPO-ACTIVE".equals(w.businessUnitCode)));
    assertTrue(result.items().stream().noneMatch(w -> "REPO-ARCH".equals(w.businessUnitCode)));
  }

  @Test
  @Transactional
  void searchCombinesFiltersWithAndLogic() {
    persistWarehouse("REPO-A", "TILBURG-001", 30, 5, null);
    persistWarehouse("REPO-B", "TILBURG-001", 60, 5, null);

    WarehouseSearchQueryResult result =
        warehouseRepository.searchActiveWarehouses(
            "TILBURG-001", 50, null, "capacity", false, 0, 10);

    assertEquals(1, result.items().size());
    assertEquals("REPO-B", result.items().get(0).businessUnitCode);
  }

  @Test
  @Transactional
  void searchWithoutOptionalFiltersUsesOnlyActivePredicate() {
    persistWarehouse("REPO-ANY", "VETSBY-001", 10, 1, null);

    WarehouseSearchQueryResult result =
        warehouseRepository.searchActiveWarehouses(
            null, null, null, "createdAt", true, 0, 5);

    assertTrue(result.totalCount() >= 1);
    assertTrue(result.items().stream().anyMatch(w -> "REPO-ANY".equals(w.businessUnitCode)));
  }

  @Test
  @Transactional
  void updateThrowsWhenWarehouseDoesNotExist() {
    Warehouse missing = new Warehouse();
    missing.businessUnitCode = "REPO-MISSING";
    missing.location = "ZWOLLE-001";
    missing.capacity = 10;
    missing.stock = 1;

    assertThrows(IllegalArgumentException.class, () -> warehouseRepository.update(missing));
  }

  @Test
  void removeIsNotSupported() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> warehouseRepository.remove(new Warehouse()));
  }

  private void persistWarehouse(
      String code, String location, int capacity, int stock, LocalDateTime archivedAt) {
    DbWarehouse row = new DbWarehouse();
    row.businessUnitCode = code;
    row.location = location;
    row.capacity = capacity;
    row.stock = stock;
    row.createdAt = LocalDateTime.now().minusHours(1);
    row.archivedAt = archivedAt;
    em.persist(row);
    em.flush();
  }
}
