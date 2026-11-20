package ae.securepay.website.web;

import ae.securepay.website.content.Html;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/** Security headers on every response, and the deployment path for internal links. */
public class SiteFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        Html.setContextPath(request.getContextPath());
        try {
            chain.doFilter(request, response);
        } finally {
            Html.clearContextPath();
        }
    }
}
