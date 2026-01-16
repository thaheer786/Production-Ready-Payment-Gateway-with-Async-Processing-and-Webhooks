package com.gateway.filter;

import com.gateway.model.Merchant;
import com.gateway.repository.MerchantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {
    
    private final MerchantRepository merchantRepository;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Skip authentication for health check and test endpoints
        if (path.equals("/health") || path.startsWith("/api/v1/test/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Skip authentication for non-API paths
        if (!path.startsWith("/api/v1/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String apiKey = request.getHeader("X-Api-Key");
        String apiSecret = request.getHeader("X-Api-Secret");
        
        if (apiKey == null || apiSecret == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":{\"code\":\"AUTHENTICATION_ERROR\",\"description\":\"API credentials not provided\"}}");
            return;
        }
        
        Optional<Merchant> merchantOpt = merchantRepository.findByApiKey(apiKey);
        
        if (merchantOpt.isEmpty() || !merchantOpt.get().getApiSecret().equals(apiSecret)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":{\"code\":\"AUTHENTICATION_ERROR\",\"description\":\"Invalid API credentials\"}}");
            return;
        }
        
        // Store merchant in request attribute
        request.setAttribute("merchant", merchantOpt.get());
        
        filterChain.doFilter(request, response);
    }
}
