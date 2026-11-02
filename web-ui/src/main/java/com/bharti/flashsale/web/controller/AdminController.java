package com.bharti.flashsale.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bharti.flashsale.web.client.ApiException;
import com.bharti.flashsale.web.client.GatewayClient;
import com.bharti.flashsale.web.controller.Forms.ProductForm;
import com.bharti.flashsale.web.controller.Forms.RestockForm;
import com.bharti.flashsale.web.session.LoginUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Admin screens. Hiding them for non-admins is only for convenience:
 * the real check (ADMIN role from the JWT) happens in inventory-service.
 */
@Controller
@RequiredArgsConstructor
public class AdminController {

    private final GatewayClient gatewayClient;

    @GetMapping("/admin/products")
    public String products(@SessionAttribute(LoginUser.SESSION_KEY) LoginUser user, Model model) {
        requireAdmin(user);
        model.addAttribute("products", gatewayClient.listProducts());
        if (!model.containsAttribute("productForm")) {
            model.addAttribute("productForm", new ProductForm());
        }
        model.addAttribute("restockForm", new RestockForm());
        return "admin/products";
    }

    @PostMapping("/admin/products")
    public String create(@SessionAttribute(LoginUser.SESSION_KEY) LoginUser user,
                         @Valid @ModelAttribute("productForm") ProductForm form, BindingResult errors,
                         Model model, RedirectAttributes redirect) {
        requireAdmin(user);
        if (errors.hasErrors()) {
            model.addAttribute("products", gatewayClient.listProducts());
            model.addAttribute("restockForm", new RestockForm());
            return "admin/products";
        }
        try {
            gatewayClient.createProduct(user.token(), form.getName(), form.getPrice(), form.getStock());
            redirect.addFlashAttribute("success", "Product \"" + form.getName() + "\" created.");
        } catch (ApiException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/admin/products/{id}/restock")
    public String restock(@SessionAttribute(LoginUser.SESSION_KEY) LoginUser user, @PathVariable Long id,
                          @Valid @ModelAttribute("restockForm") RestockForm form, BindingResult errors,
                          RedirectAttributes redirect) {
        requireAdmin(user);
        if (errors.hasErrors()) {
            redirect.addFlashAttribute("error", "Enter a restock quantity of at least 1.");
            return "redirect:/admin/products";
        }
        try {
            gatewayClient.restock(user.token(), id, form.getQuantity());
            redirect.addFlashAttribute("success", "Added " + form.getQuantity() + " units to product #" + id + ".");
        } catch (ApiException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/products";
    }

    private void requireAdmin(LoginUser user) {
        if (!user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins only");
        }
    }
}
