package com.bharti.flashsale.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bharti.flashsale.web.client.ApiException;
import com.bharti.flashsale.web.client.ApiModels.TokenResponse;
import com.bharti.flashsale.web.client.GatewayClient;
import com.bharti.flashsale.web.controller.Forms.LoginForm;
import com.bharti.flashsale.web.session.JwtClaimsReader;
import com.bharti.flashsale.web.session.LoginUser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final GatewayClient gatewayClient;
    private final JwtClaimsReader jwtClaimsReader;

    @GetMapping("/")
    public String home() {
        return "redirect:/products";
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("loginForm", new LoginForm());
        return "login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginForm") LoginForm form, BindingResult errors,
                        HttpServletRequest request, Model model) {
        if (errors.hasErrors()) {
            return "login";
        }
        try {
            TokenResponse token = gatewayClient.login(form.getUsername(), form.getPassword());
            // New session id after login (prevents session fixation)
            HttpSession old = request.getSession(false);
            if (old != null) {
                old.invalidate();
            }
            HttpSession session = request.getSession(true);
            session.setAttribute(LoginUser.SESSION_KEY, new LoginUser(form.getUsername(), token.accessToken(),
                    jwtClaimsReader.roles(token.accessToken())));
            return "redirect:/products";
        } catch (ApiException e) {
            model.addAttribute("error", e.isUnauthorized() ? "Wrong username or password." : e.getMessage());
            return "login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirect) {
        session.invalidate();
        redirect.addFlashAttribute("success", "You have been logged out.");
        return "redirect:/products";
    }
}
