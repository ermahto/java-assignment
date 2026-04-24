package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.location.LocationGateway;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class CreateWarehouseUseCaseTest {

  @Inject WarehouseRepository warehouseRepository;

  @Inject LocationGateway locationGateway;

  @Inject EntityManager em;

  private CreateWarehouseUseCase useCase;

  @BeforeEach
  @Transactional
  void cleanScratchData() {
    em.createQuery("DELETE FROM DbWarehouse w WHERE w.businessUnitCode LIKE 'CWU-%'")
        .executeUpdate();
    useCase = new CreateWarehouseUseCase(warehouseRepository, locationGateway);
  }

  @Test
  @Transactional
  void createsWarehouseAndPersistsMetadata() {
    Warehouse input = newWarehouse("CWU-OK", "EINDHOVEN-001", 40, 10);

    useCase.create(input);

    Warehouse stored = warehouseRepository.findByBusinessUnitCode("CWU-OK");
    assertNotNull(stored);
    assertEquals("EINDHOVEN-001", stored.location);
    assertEquals(40, stored.capacity);
    assertEquals(10, stored.stock);
    assertNotNull(stored.createdAt, "createdAt should be set by the use case");
  }

  @Test
  @Transactional
  void rejectsDuplicateBusinessUnitCode() {
    useCase.create(newWarehouse("CWU-DUP", "ZWOLLE-001", 10, 5));

    Warehouse second = newWarehouse("CWU-DUP", "ZWOLLE-002", 20, 5);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> useCase.create(second));
    assertTrue(ex.getMessage().contains("already exists"));
  }

  @Test
  @Transactional
  void rejectsStockGreaterThanCapacity() {
    Warehouse invalid = newWarehouse("CWU-STK", "HELMOND-001", 30, 50);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> useCase.create(invalid));
    assertTrue(ex.getMessage().contains("exceeds warehouse capacity"));
  }

  @Test
  @Transactional
  void rejectsUnknownLocation() {
    Warehouse invalid = newWarehouse("CWU-LOC", "NO-SUCH-PLACE", 10, 5);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> useCase.create(invalid));
    assertTrue(ex.getMessage().contains("not valid"));
  }

  private static Warehouse newWarehouse(String code, String location, int capacity, int stock) {
    Warehouse w = new Warehouse();
    w.businessUnitCode = code;
    w.location = location;
    w.capacity = capacity;
    w.stock = stock;
    return w;
  }
}
