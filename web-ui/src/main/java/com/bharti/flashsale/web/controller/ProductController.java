package com.bharti.flashsale.web.controller;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bharti.flashsale.web.client.ApiException;
import com.bharti.flashsale.web.client.ApiModels.OrderView;
import com.bharti.flashsale.web.client.GatewayClient;
import com.bharti.flashsale.web.controller.Forms.BuyForm;
import com.bharti.flashsale.web.session.LoginUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final GatewayClient gatewayClient;

    @GetMapping("/products")
    public String list(Model model) {
        model.addAttribute("products", gatewayClient.listProducts());
        return "products";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Long id,
                         @SessionAttribute(name = LoginUser.SESSION_KEY, required = false) LoginUser user,
                         Model model) {
        model.addAttribute("product", gatewayClient.getProduct(id));
        if (!model.containsAttribute("buyForm")) {
            BuyForm form = new BuyForm();
            // One key per page view = one purchase attempt. Resubmitting this form reuses it -> no duplicate order.
            form.setIdempotencyKey(UUID.randomUUID().toString());
            if (user != null) {
                form.setCustomerEmail(user.username() + "@example.com");
            }
            model.addAttribute("buyForm", form);
        }
        return "product-detail";
    }

    @PostMapping("/products/{id}/buy")
    public String buy(@PathVariable Long id,
                      @Valid @ModelAttribute("buyForm") BuyForm form, BindingResult errors,
                      @SessionAttribute(LoginUser.SESSION_KEY) LoginUser user,
                      Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            model.addAttribute("product", gatewayClient.getProduct(id));
            return "product-detail";
        }
        try {
            OrderView order = gatewayClient.placeOrder(user.token(), form.getIdempotencyKey(), id,
                    form.getQuantity(), form.getCustomerEmail());
            redirect.addFlashAttribute("success",
                    "Order placed! We are reserving your stock and taking the payment.");
            return "redirect:/orders/" + order.id();
        } catch (ApiException e) {
            if (e.isUnauthorized()) {
                throw e; // handled globally: back to login
            }
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/products/" + id;
        }
    }
}
