package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import java.util.List;

public record WarehouseSearchQueryResult(
    List<Warehouse> items, long totalCount, int page, int pageSize) {}
