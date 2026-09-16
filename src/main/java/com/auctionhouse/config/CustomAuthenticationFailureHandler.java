package com.auctionhouse.config;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Custom authentication failure handler that distinguishes between
 * different failure reasons (bad credentials vs banned account).
 */
@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        
        // Check if the account is disabled/banned
        if (exception instanceof DisabledException || exception instanceof LockedException) {
            getRedirectStrategy().sendRedirect(request, response, "/login?error=banned");
            return;
        }
        
        // Check if the exception message contains our deactivated message
        String message = exception.getMessage();
        if (message != null && message.contains("deactivated")) {
            getRedirectStrategy().sendRedirect(request, response, "/login?error=banned");
            return;
        }
        
        // Default: bad credentials
        getRedirectStrategy().sendRedirect(request, response, "/login?error=true");
    }
}
