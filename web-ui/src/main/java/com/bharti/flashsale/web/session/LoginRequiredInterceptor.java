package com.bharti.flashsale.web.session;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/** Pages that need a logged-in user redirect to /login instead. */
@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(LoginUser.SESSION_KEY) != null) {
            return true;
        }
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }
}
