package guru.springframework.spring6restclient.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

/**
 * RestClient configuration with OAuth2 support for Spring Boot 4.0.2
 * <p>
 * This configuration uses Spring Security 7.0's built-in OAuth2ClientHttpRequestInterceptor
 * to automatically handle OAuth2 client credentials flow for RestClient.
 * <p>
 * Key components:
 * - OAuth2AuthorizedClientManager: Manages OAuth2 authorization and token lifecycle
 * - OAuth2ClientHttpRequestInterceptor: Built-in interceptor that adds Bearer tokens to requests
 * - OAuth2AuthorizedClientService: Required for token caching and reuse
 *
 * @author Adapted by Anthropic Sonnet 4.5, 2026-01-24
 * @author Updated by Pierrot, 2026-01-29
 * @author Fixed and verified by Claude (Anthropic), 2026-02-14
 * @version Spring Boot 4.0.2, Spring Security 7.0
 */
@Configuration
public class RestClientConfig {

    @Value("${restclient.rootUrl}")
    String rootUrl;

    /**
     * Creates the OAuth2AuthorizedClientManager bean for managing OAuth2 authorization.
     * Configures client credentials grant type for service-to-service authentication.
     *
     * @param clientRegistrationRepository Repository containing OAuth2 client registrations
     * @param oAuth2AuthorizedClientService Service for storing and retrieving authorized clients (token cache)
     * @return Configured OAuth2AuthorizedClientManager
     */
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService oAuth2AuthorizedClientService) {

        var authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        var authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository, oAuth2AuthorizedClientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    /**
     * Creates Spring Security's built-in OAuth2ClientHttpRequestInterceptor.
     * This interceptor automatically obtains and adds OAuth2 Bearer tokens to RestClient requests.
     * <p>
     * Note: This is the modern approach introduced in Spring Security 6.4 and used in Spring Security 7.0.
     * It eliminates the need for custom interceptor implementations.
     * <p>
     * The interceptor is configured to use the "springauth" client registration ID,
     * which matches the configuration in application.properties.
     *
     * @param authorizedClientManager The manager responsible for OAuth2 authorization
     * @return Configured OAuth2ClientHttpRequestInterceptor
     */
    @Bean
    public OAuth2ClientHttpRequestInterceptor oauth2ClientHttpRequestInterceptor(
            OAuth2AuthorizedClientManager authorizedClientManager) {
        OAuth2ClientHttpRequestInterceptor interceptor =
                new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        // Set the client registration ID resolver to use "springauth"
        interceptor.setClientRegistrationIdResolver(request -> "springauth");
        return interceptor;
    }

    /**
     * Configures the RestClient.Builder bean with OAuth2 support.
     * All RestClient instances created from this builder will automatically include OAuth2 authentication.
     *
     * @param oauth2ClientHttpRequestInterceptor The OAuth2 interceptor to add authentication headers
     * @return Configured RestClient.Builder
     */
    @Bean
    public RestClient.Builder restClientBuilder(OAuth2ClientHttpRequestInterceptor oauth2ClientHttpRequestInterceptor) {
        return RestClient.builder()
                .baseUrl(rootUrl)
                .requestInterceptor(oauth2ClientHttpRequestInterceptor);
    }
}