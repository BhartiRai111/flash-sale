package com.bharti.flashsale.web.session;

import java.io.Serializable;
import java.util.List;

/** What the UI keeps in the HTTP session after login. The JWT itself is checked by the gateway on every call. */
public record LoginUser(String username, String token, List<String> roles) implements Serializable {

    public static final String SESSION_KEY = "loginUser";

    public boolean isAdmin() {
        return roles != null && roles.contains("ADMIN");
    }
}
