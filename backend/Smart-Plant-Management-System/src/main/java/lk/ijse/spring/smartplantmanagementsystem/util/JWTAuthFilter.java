package lk.ijse.spring.smartplantmanagementsystem.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.ijse.spring.smartplantmanagementsystem.service.TokenDanyListService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JWTAuthFilter extends OncePerRequestFilter {
    private final JWTUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final TokenDanyListService denyListService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwtToken;
        final String username;
        final String jti;

        if (authHeader==null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        jwtToken = authHeader.substring(7);

        try {

            jti = jwtUtil.extractJti(jwtToken);

            if (denyListService.isDenyList(jti)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has been invalidated");
                return;
            }

            username=jwtUtil.extractUsername(jwtToken);
            if (username!=null && SecurityContextHolder.getContext()
                    .getAuthentication()==null) {
                UserDetails userDetails=userDetailsService
                        .loadUserByUsername(username);
                if (jwtUtil.validateToken(jwtToken)){
                    UsernamePasswordAuthenticationToken authToken
                            =new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new org.springframework.security.authentication.BadCredentialsException("Token expired", e);
        } catch (Exception e) {
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid token", e);
        }

        filterChain.doFilter(request, response);
    }
}
