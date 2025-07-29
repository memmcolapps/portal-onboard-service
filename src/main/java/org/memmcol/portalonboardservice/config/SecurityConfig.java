package org.memmcol.portalonboardservice.config;


import com.hazelcast.core.HazelcastInstance;
import lombok.RequiredArgsConstructor;
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
				authenticationManager(userDetailsService, bCryptPasswordEncoder), operatorMapper, auditRepository, hazelcastInstance);
		userAuthFilter.setFilterProcessesUrl("/gridflex/auth/service/login");

//		CustomAuthenticationFilter adminAuthFilter = new CustomAuthenticationFilter(
//				authenticationManager(userDetailsService, bCryptPasswordEncoder), operatorMapper, auditRepository, hazelcastInstance);
//		adminAuthFilter.setFilterProcessesUrl("/auth/service/admin/login");

		// // disable cors
		// http.cors((cors) -> cors.disable());
		// http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
		// disable csrf
		http.csrf((csrf) -> csrf.disable());

		// header
		http.headers(headers -> headers.contentTypeOptions().disable()// Set X-Content-Type-Options header to nosniff
				.frameOptions().deny() // Set X-Frame-Options header to DENY
				.xssProtection()); // Enable XSS protection

		// Authorization
		http.authorizeHttpRequests((authorize) -> authorize
				.requestMatchers("/gridflex/auth/service/login/**", "/gridflex/auth/service/logout/**", "/actuator/prometheus").permitAll()
				.requestMatchers("/auth/service/**")
				.hasAnyAuthority("WRITE","SUPER_ADMIN")
				.requestMatchers("/gridflex/operator/service/get")
				.hasAnyAuthority("WRITE","SUPER_ADMIN", "READ")
				.anyRequest().authenticated());

//		http.addFilter(customAuthenticationFilter);
		http.addFilter(userAuthFilter);
//		http.addFilter(adminAuthFilter);
		http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.addFilterBefore(new CustomAuthorizationFilter(), UsernamePasswordAuthenticationFilter.class);

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
