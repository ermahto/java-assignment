package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  private static final Logger LOG = Logger.getLogger(WarehouseRepository.class);

  @Override
  public List<Warehouse> getAll() {
    return this.listAll().stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  public void create(Warehouse warehouse) {
    DbWarehouse dbWarehouse = new DbWarehouse();
    dbWarehouse.businessUnitCode = warehouse.businessUnitCode;
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.createdAt = warehouse.createdAt;
    dbWarehouse.archivedAt = warehouse.archivedAt;
    
    this.persist(dbWarehouse);
  }

  @Override
  public void update(Warehouse warehouse) {
    DbWarehouse entity = find("businessUnitCode", warehouse.businessUnitCode).firstResult();
    if (entity == null) {
      LOG.warnf("update skipped: no warehouse for businessUnitCode=%s", warehouse.businessUnitCode);
      throw new IllegalArgumentException(
          "Warehouse with business unit code '" + warehouse.businessUnitCode + "' does not exist");
    }
    entity.location = warehouse.location;
    entity.capacity = warehouse.capacity;
    entity.stock = warehouse.stock;
    entity.archivedAt = warehouse.archivedAt;
    flush();
  }

  /**
   * Active warehouses only ({@code archivedAt} is null). Filters combine with AND. Sort field must be
   * {@code createdAt} or {@code capacity}.
   */
  public WarehouseSearchQueryResult searchActiveWarehouses(
      String location,
      Integer minCapacity,
      Integer maxCapacity,
      String sortField,
      boolean descending,
      int page,
      int pageSize) {

    StringBuilder query = new StringBuilder("archivedAt is null");
    Parameters params = null;

    if (location != null && !location.isBlank()) {
      query.append(" and location = :location");
      params = Parameters.with("location", location.trim());
    }
    if (minCapacity != null) {
      query.append(" and capacity >= :minCap");
      params =
          params == null
              ? Parameters.with("minCap", minCapacity)
              : params.and("minCap", minCapacity);
    }
    if (maxCapacity != null) {
      query.append(" and capacity <= :maxCap");
      params =
          params == null
              ? Parameters.with("maxCap", maxCapacity)
              : params.and("maxCap", maxCapacity);
    }

    Sort sort =
        descending ? Sort.descending(sortField) : Sort.ascending(sortField);
    PanacheQuery<DbWarehouse> panacheQuery =
        params != null ? find(query.toString(), sort, params) : find(query.toString(), sort);
    long total = panacheQuery.count();
    List<Warehouse> items =
        panacheQuery.page(Page.of(page, pageSize)).list().stream()
            .map(DbWarehouse::toWarehouse)
            .toList();
    return new WarehouseSearchQueryResult(items, total, page, pageSize);
  }

  @Override
  public void remove(Warehouse warehouse) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'remove'");
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse dbWarehouse = find("businessUnitCode", buCode).firstResult();
    return dbWarehouse != null ? dbWarehouse.toWarehouse() : null;
  }
}
