package com.bharti.flashsale.web.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.bharti.flashsale.web.client.ApiModels.OrderView;
import com.bharti.flashsale.web.client.ApiModels.ProductView;
import com.bharti.flashsale.web.client.GatewayClient;
import com.bharti.flashsale.web.session.LoginUser;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final GatewayClient gatewayClient;

    @GetMapping("/orders")
    public String list(@SessionAttribute(LoginUser.SESSION_KEY) LoginUser user, Model model) {
        List<OrderView> orders = gatewayClient.listOrders(user.token());
        model.addAttribute("orders", orders);
        model.addAttribute("productNames", productNames());
        // Keep refreshing while any order is still moving through the saga
        model.addAttribute("autoRefresh", orders.stream().anyMatch(OrderView::pending));
        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String detail(@PathVariable UUID id, @SessionAttribute(LoginUser.SESSION_KEY) LoginUser user,
                         Model model) {
        OrderView order = gatewayClient.getOrder(user.token(), id);
        model.addAttribute("order", order);
        model.addAttribute("productName", productNames().getOrDefault(order.productId(), "Product #" + order.productId()));
        model.addAttribute("autoRefresh", order.pending());
        return "order-detail";
    }

    /**
     * API composition (Din 2, tareeka 1): orders only store the product id, so the UI asks the
     * inventory service for names and joins them in memory (one call for the whole page, not one per order).
     */
    private Map<Long, String> productNames() {
        return gatewayClient.listProducts().stream()
                .collect(Collectors.toMap(ProductView::id, ProductView::name, (a, b) -> a));
    }
}
