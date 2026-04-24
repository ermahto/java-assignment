package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.warehouse.api.beans.Warehouse;
import java.util.List;

public record WarehouseSearchPageResponse(
    List<Warehouse> items, long totalCount, int page, int pageSize) {}
