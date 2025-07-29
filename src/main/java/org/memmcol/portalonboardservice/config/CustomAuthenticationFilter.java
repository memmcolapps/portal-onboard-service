package org.memmcol.portalonboardservice.config;


import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.memmcol.portalonboardservice.mapper.PortalUserMapper;
import org.memmcol.portalonboardservice.model.CustomUserDetails;
import org.memmcol.portalonboardservice.model.audit.AuditLog;
import org.memmcol.portalonboardservice.model.Operator;
import org.memmcol.portalonboardservice.repository.AuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.memmcol.portalonboardservice.util.GenericHandler.getClientIp;


@RequiredArgsConstructor
public class CustomAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
	 private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationFilter.class);
	 private AuthenticationManager authenticationManager;
//	@Autowired
	private PortalUserMapper operatorMapper;

	private AuditRepository auditRepository;

	private IMap<String, Boolean> auditCache;

	private IMap<String, Boolean> authCache;

//	private HazelcastInstance hazelcastInstance;
	// Define the required headers
	private static final String ADMIN_HEADER_KEY = "custom";
	private static final String ADMIN_HEADER_VALUE = "YvW$%12xYz!@#8&!76P&*45QH@b33b%"; // Change this to a secure value

//	private static final String USER_HEADER_KEY = "custom";
//	private static final String USER_HEADER_VALUE = "H@beebUvW$%12xYz!@#9LmNoP&*45Q"; // Change this to a secure value

	public CustomAuthenticationFilter(
			AuthenticationManager authenticationManager,
			PortalUserMapper operatorMapper,
			AuditRepository auditRepository, HazelcastInstance hazelcastInstance) {
		this.authenticationManager = authenticationManager;
		this.operatorMapper = operatorMapper;
		this.auditRepository = auditRepository;
		this.auditCache = hazelcastInstance.getMap("auditCache");
		this.authCache = hazelcastInstance.getMap("authCache");
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException {
		String username = request.getParameter("username");
		String password = request.getParameter("password");

		// Fetch user details before authentication
		Operator user = operatorMapper.findByAuthEmail(username);
		if (user == null) {
			throw new UsernameNotFoundException("User not found");
		}

//		// Determine if user is admin or regular user
//		boolean isAdmin = user.isPermission();
//		String requiredHeaderKey = isAdmin ? ADMIN_HEADER_KEY : USER_HEADER_KEY;
//		String requiredHeaderValue = isAdmin ? ADMIN_HEADER_VALUE : USER_HEADER_VALUE;

		// Validate the required header
		String headerValue = request.getHeader(ADMIN_HEADER_KEY);
		if (headerValue == null || !headerValue.equals(ADMIN_HEADER_VALUE)) {
			throw new BadCredentialsException("Missing or invalid authentication header: " + ADMIN_HEADER_KEY);
		}

//		// Dynamically set login URL
//		if (isAdmin) {
//			setFilterProcessesUrl("/auth/service/admin/login");
//		} else {
//			setFilterProcessesUrl("/auth/service/login");
//		}
		UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
		return authenticationManager.authenticate(authenticationToken);
	}

	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
			Authentication authentication) throws IOException, ServletException {
		CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();
//		User user = (User) authentication.getPrincipal();// Add a custom header with the JWT token
		AuditLog auditNotificationDTO = new AuditLog();
		String ipAddress = getClientIp(request);
		String userAgent = request.getHeader("User-Agent");
		Algorithm algorithm = Algorithm.HMAC256("secret".getBytes()); //Encrypt/Sign the token
		String access_token = JWT.create()
				.withSubject(userPrincipal.getUsername())
				.withExpiresAt(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)) // 30 days expiration
				.withClaim("roles", userPrincipal.getAuthorities().stream()
						.map(GrantedAuthority::getAuthority)
						.collect(Collectors.toList()))
//				.withClaim("orgId", userPrincipal.getOrgId().toString())
				.sign(algorithm);

		Operator operator = operatorMapper.findByAuthEmail(userPrincipal.getUsername());
		operator.setPassword("");
		auditNotificationDTO.setCreator(operator);
		auditNotificationDTO.setIpAddress(ipAddress);
		auditNotificationDTO.setUserAgent(userAgent);
		auditNotificationDTO.setDescription("Logged in");
		auditNotificationDTO.setType("auth");
		for (String key : auditCache.keySet()) {
			if (key.startsWith("audit_log_page_")) {
				auditCache.remove(key);
			}
		}
//		authCache.remove("dashboard");
		auditRepository.save(auditNotificationDTO);
		Map<String, Object> resp = new HashMap<>();
		Map<String, Object> token = new HashMap<>();
		resp.put("responsecode", "001");
		resp.put("responsedesc", "Authentication Successful");
		token.put("user_info", operator);
		token.put("access_token", access_token);
		resp.put("responsedata", token);
		operatorMapper.updateLoginState(userPrincipal.getUsername());
		// Set content type to JSON
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		// Write the response as JSON
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.writeValue(response.getOutputStream(), resp);

	}

	@Override
	protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException failed) throws IOException, ServletException {
//	    log.error("Authentication failed: {}", failed.getMessage());

	    // Prepare the response message
	    Map<String, String> errorMessage = new HashMap<>();
	    errorMessage.put("responsecode", "122");
	    errorMessage.put("responsedesc", failed.getMessage());
	    errorMessage.put("responsedata", "");

	    // Set the response status to indicate authentication failure
	    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

	    // Write the error message to the response body
	    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
	    new ObjectMapper().writeValue(response.getOutputStream(), errorMessage);
	}

}

