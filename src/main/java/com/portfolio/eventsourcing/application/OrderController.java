package com.portfolio.eventsourcing.application;

import com.portfolio.eventsourcing.domain.Order;
import com.portfolio.eventsourcing.domain.OrderCommand;
import com.portfolio.eventsourcing.domain.OrderService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final CqrsProjection projection;

    public OrderController(OrderService orderService, CqrsProjection projection) {
        this.orderService = orderService;
        this.projection = projection;
    }

    @PostMapping
    public Map<String, Object> createOrder(@RequestBody Map<String, String> body) {
        var orderId = UUID.randomUUID();
        var command = new OrderCommand.CreateOrder(
            orderId,
            body.get("customerName"),
            body.get("product"),
            Integer.parseInt(body.get("quantity")));
        var event = orderService.handle(command);
        projection.rebuild();
        return Map.of("orderId", orderId.toString(), "event", event.getClass().getSimpleName());
    }

    @PostMapping("/{id}/ship")
    public Map<String, Object> shipOrder(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        var command = new OrderCommand.ShipOrder(id, body.get("trackingNumber"));
        var event = orderService.handle(command);
        projection.rebuild();
        return Map.of("orderId", id.toString(), "event", event.getClass().getSimpleName());
    }

    @PostMapping("/{id}/cancel")
    public Map<String, Object> cancelOrder(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        var command = new OrderCommand.CancelOrder(id, body.get("reason"));
        var event = orderService.handle(command);
        projection.rebuild();
        return Map.of("orderId", id.toString(), "event", event.getClass().getSimpleName());
    }

    @PostMapping("/{id}/deliver")
    public Map<String, Object> deliverOrder(@PathVariable UUID id) {
        var command = new OrderCommand.DeliverOrder(id);
        var event = orderService.handle(command);
        projection.rebuild();
        return Map.of("orderId", id.toString(), "event", event.getClass().getSimpleName());
    }

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable UUID id) {
        return projection.getOrder(id);
    }

    @GetMapping
    public List<Order> listOrders() {
        return projection.getAllOrders();
    }
}
