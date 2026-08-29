package cn.nanpo.window.security;

import java.security.Principal;
import java.util.Set;

public record UserPrincipal(
        long id,
        String phone,
        String displayName,
        Set<String> roles) implements Principal {

    @Override
    public String getName() {
        return phone;
    }
}

