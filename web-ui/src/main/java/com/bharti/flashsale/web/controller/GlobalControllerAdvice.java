package com.bharti.flashsale.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import com.bharti.flashsale.web.client.ApiException;
import com.bharti.flashsale.web.session.LoginUser;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class GlobalControllerAdvice {

    /** Every page gets the logged-in user (or null) for the header menu. */
    @ModelAttribute("currentUser")
    public LoginUser currentUser(HttpSession session) {
        Object user = session.getAttribute(LoginUser.SESSION_KEY);
        return user instanceof LoginUser loginUser ? loginUser : null;
    }

    @ExceptionHandler(ApiException.class)
    public String apiError(ApiException e, HttpSession session, Model model, HttpServletResponse response) {
        if (e.isUnauthorized()) {
            // Token expired or invalid: start a fresh login
            session.invalidate();
            return "redirect:/login";
        }
        response.setStatus(e.getStatus());
        model.addAttribute("title", e.getStatus() == 404 ? "Not found" : "Something went wrong");
        model.addAttribute("message", e.getMessage());
        return "error-page";
    }

    /** Gateway not reachable at all (container down, wrong URL). */
    @ExceptionHandler(ResourceAccessException.class)
    public String gatewayDown(ResourceAccessException e, Model model, HttpServletResponse response) {
        log.warn("Gateway unreachable: {}", e.getMessage());
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        model.addAttribute("title", "Service unavailable");
        model.addAttribute("message", "We can't reach the store right now. Please try again in a minute.");
        return "error-page";
    }

    @ExceptionHandler(ResponseStatusException.class)
    public String statusError(ResponseStatusException e, Model model, HttpServletResponse response) {
        response.setStatus(e.getStatusCode().value());
        model.addAttribute("title", e.getStatusCode().value() == 403 ? "Not allowed" : "Something went wrong");
        model.addAttribute("message", e.getReason());
        return "error-page";
    }
}
