package org.memmcol.portalonboardservice.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import lombok.RequiredArgsConstructor;
import org.memmcol.portalonboardservice.components.CustomAccessDeniedHandler;
import org.memmcol.portalonboardservice.components.CustomAuthorizationFilter;
import org.memmcol.portalonboardservice.components.GenericHandler;
import org.memmcol.portalonboardservice.mapper.PortalUserMapper;
import org.memmcol.portalonboardservice.repository.AuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class  SecurityConfig {

	@Autowired
	private UserDetailsService userDetailsService;

    @Autowired
    private PortalUserMapper operatorMapper;

	@Autowired
	private AuditRepository auditRepository;

	@Autowired
	private CustomAccessDeniedHandler customAccessDeniedHandler;

	@Autowired
	private GenericHandler genericHandler;

	@Autowired
	private ObjectMapper objectMapper;

    @Qualifier("hazelcastInstance")
    @Autowired private HazelcastInstance hazelcastInstance;
//	private final IMap<String, Boolean> authCache;
	private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

	public void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService).passwordEncoder(bCryptPasswordEncoder);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@SuppressWarnings("removal")
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		CustomAuthenticationFilter userAuthFilter = new CustomAuthenticationFilter(
				authenticationManager(userDetailsService, bCryptPasswordEncoder), operatorMapper, auditRepository, hazelcastInstance, genericHandler,objectMapper);
		userAuthFilter.setFilterProcessesUrl("/gfPortal/auth/service/login");

//		CustomAuthenticationFilter adminAuthFilter = new CustomAuthenticationFilter(
//				authenticationManager(userDetailsService, bCryptPasswordEncoder), operatorMapper, auditRepository, hazelcastInstance);
//		adminAuthFilter.setFilterProcessesUrl("/auth/service/admin/login");
		http.csrf((csrf) -> csrf.disable());

		// header
		http.headers(headers -> headers.contentTypeOptions().disable()// Set X-Content-Type-Options header to nosniff
				.frameOptions().deny() // Set X-Frame-Options header to DENY
				.xssProtection()); // Enable XSS protection

		// Authorization
		http.authorizeHttpRequests((authorize) -> authorize
				.requestMatchers("/gfPortal/auth/service/login/**", "/gfPortal/auth/service/logout/**", "/actuator/prometheus", "/gfPortal/service/message/create",
						"/gfPortal/auth/service/generate-otp","/gfPortal/auth/service/forget-password", "/gfPortal/analytic/service/all", "/gfPortal/auth/service/test").permitAll()
				.requestMatchers("/gfPortal/service/organization/update", "/gfPortal/service/organization/create", "/gfPortal/service/organization/suspend",
						"/gfPortal/service/organization/get", "/gfPortal/service/organization/all", "/gfPortal/auth/service/profile", "/gfPortal/auth/service/update",
						"/gfPortal/auth/service/single", "/gfPortal/auth/service/create","/gfPortal/auth/service/change-status","/gfPortal/auth/service/all",
						"/gfPortal/auth/service/recent/activity", "/gfPortal/audit-log/service/all", "/gfPortal/node/service/create/node/region-bhub-service-center",
						"/gfPortal/node/service/update/node/region-bhub-service-center", "/gfPortal/node/service/create/node/substation-transformer-feeder-line",
						"/gfPortal/node/service/update/node/substation-transformer-feeder-line", "/gfPortal/node/service/single", "/gfPortal/node/service/all",
						"/gfPortal/analytic/service/dashboard", "/gfPortal/analytic/service/incident/report", "/gfPortal/analytic/service/incident/report/resolve",
						"/gfPortal/service/message/read", "/gfPortal/service/message/get")
				.hasAnyAuthority("ADMIN","SUPER_ADMIN", "DEVELOPER", "SUPPORT")
//				.requestMatchers("/gfPortal/service/organization/get", "/gfPortal/service/organization/all", "/gfPortal/auth/service/profile",
//						"/gfPortal/auth/service/logout", "/gfPortal/auth/service/update", "/gfPortal/auth/service/single", "/gfPortal/audit-log/service")
//				.hasAnyAuthority("ADMIN", "SUPER_ADMIN", "DEVELOPER", "SUPPORT")
				.anyRequest().authenticated()).exceptionHandling(ex -> ex
				.accessDeniedHandler(customAccessDeniedHandler)
		);

//		http.addFilter(customAuthenticationFilter);
		http.addFilter(userAuthFilter);
//		http.addFilter(adminAuthFilter);
		http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.addFilterBefore(new CustomAuthorizationFilter(hazelcastInstance), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
												PasswordEncoder passwordEncoder) {

		DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
		authenticationProvider.setUserDetailsService(userDetailsService);
		authenticationProvider.setPasswordEncoder(passwordEncoder);

		return new ProviderManager(authenticationProvider);
	}

}
