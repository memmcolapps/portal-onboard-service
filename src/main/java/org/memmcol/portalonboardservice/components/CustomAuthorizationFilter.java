package org.memmcol.portalonboardservice.components;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;


@Component
@Slf4j
public class CustomAuthorizationFilter extends OncePerRequestFilter {

//	@Autowired
	private final IMap<String, Boolean> portalOtpExpCache;

    public CustomAuthorizationFilter(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.portalOtpExpCache = hazelcastInstance.getMap("portalOtpExpCache");
    }
//	private AuthCache authCache;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (request.getServletPath().equals("/gfPortal/auth/service/login")
				|| request.getServletPath().equals("/actuator/prometheus")
				|| request.getServletPath().equals("/gfPortal/auth/service/generate-otp")
				|| request.getServletPath().equals("/gfPortal/auth/service/forget-password")
				|| request.getServletPath().equals("/gfPortal/analytic/service/all")
				|| request.getServletPath().equals("/gfPortal/service/message/create")
				|| request.getServletPath().equals("/gfPortal/auth/service/test")) {
			filterChain.doFilter(request, response);
		} else {
			String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
			if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
				try {
					String token = authorizationHeader.substring("Bearer ".length());
					// Check if token is blacklisted
					if (Boolean.TRUE.equals(portalOtpExpCache.get(token))) {
						handleException(response, new Exception("Token is blacklisted"),
								"Token is blacklisted", HttpServletResponse.SC_UNAUTHORIZED);
						return;
					}
					Algorithm algorithm = Algorithm.HMAC256("secret".getBytes());
					JWTVerifier verifier = JWT.require(algorithm).build();
					DecodedJWT decodedJWT = verifier.verify(token);
					String username = decodedJWT.getSubject();
					String[] roles = decodedJWT.getClaim("roles").asArray(String.class);
					Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
					Stream<String> rolesStream = Arrays.stream(roles);
					rolesStream.forEach((role) -> {
						authorities.add(new SimpleGrantedAuthority(role));
					});
					UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
							username, null, authorities);
					SecurityContextHolder.getContext().setAuthentication(authenticationToken);
					filterChain.doFilter(request, response);
				} catch (JWTVerificationException exception) {
					handleException(response, exception, "Authorization Token Expired", HttpServletResponse.SC_FORBIDDEN);
				} catch (Exception exception) {
					handleException(response, exception, "Internal Server Error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				}
			} else {
				handleException(response, new Exception("Authorization Token Not Found"), "Authorization Token Not Found", HttpServletResponse.SC_UNAUTHORIZED);
			}
		}
	}
	// Helper method to handle exceptions and send a custom error message
	private void handleException(HttpServletResponse response, Exception exception, String description, int statusCode) throws IOException {
		Map<String, String> errorMessage = new HashMap<>();
		errorMessage.put("responsecode", String.valueOf(statusCode));
		errorMessage.put("responsedesc", description);
		errorMessage.put("responsedata", exception.getMessage());

		response.setStatus(statusCode);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		new ObjectMapper().writeValue(response.getOutputStream(), errorMessage);
	}
}
