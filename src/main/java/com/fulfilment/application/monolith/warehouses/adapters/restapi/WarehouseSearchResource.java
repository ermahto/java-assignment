package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseSearchQueryResult;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
@Path("/warehouse/search")
@Produces(MediaType.APPLICATION_JSON)
public class WarehouseSearchResource {

  private static final Logger LOG = Logger.getLogger(WarehouseSearchResource.class);

  @Inject WarehouseRepository warehouseRepository;

  @GET
  @Transactional
  public WarehouseSearchPageResponse search(
      @QueryParam("location") String location,
      @QueryParam("minCapacity") Integer minCapacity,
      @QueryParam("maxCapacity") Integer maxCapacity,
      @QueryParam("sortBy") String sortBy,
      @QueryParam("sortOrder") String sortOrder,
      @QueryParam("page") Integer page,
      @QueryParam("pageSize") Integer pageSize) {

    String sortField = resolveSortField(sortBy);
    String order = sortOrder == null || sortOrder.isBlank() ? "asc" : sortOrder;
    boolean descending = "desc".equalsIgnoreCase(order);

    int pageIndex = page == null || page < 0 ? 0 : page;
    int size = pageSize == null ? 10 : Math.min(100, Math.max(1, pageSize));

    LOG.debugf(
        "warehouse search location=%s minCap=%s maxCap=%s sort=%s desc=%s page=%d size=%d",
        location, minCapacity, maxCapacity, sortField, descending, pageIndex, size);

    WarehouseSearchQueryResult result =
        warehouseRepository.searchActiveWarehouses(
            location, minCapacity, maxCapacity, sortField, descending, pageIndex, size);

    List<Warehouse> apiItems = result.items().stream().map(this::toApiWarehouse).toList();
    return new WarehouseSearchPageResponse(apiItems, result.totalCount(), result.page(), result.pageSize());
  }

  private String resolveSortField(String sortBy) {
    if (sortBy == null || sortBy.isBlank()) {
      return "createdAt";
    }
    if ("createdAt".equalsIgnoreCase(sortBy) || "capacity".equalsIgnoreCase(sortBy)) {
      return sortBy.equalsIgnoreCase("capacity") ? "capacity" : "createdAt";
    }
    LOG.warnf("invalid sortBy parameter: %s", sortBy);
    throw new WebApplicationException(
        Response.status(Response.Status.BAD_REQUEST)
            .entity("sortBy must be 'createdAt' or 'capacity'")
            .type(MediaType.TEXT_PLAIN)
            .build());
  }

  private Warehouse toApiWarehouse(com.fulfilment.application.monolith.warehouses.domain.models.Warehouse w) {
    Warehouse out = new Warehouse();
    out.setBusinessUnitCode(w.businessUnitCode);
    out.setLocation(w.location);
    out.setCapacity(w.capacity);
    out.setStock(w.stock);
    return out;
  }
}
