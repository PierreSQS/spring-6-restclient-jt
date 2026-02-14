package guru.springframework.spring6restclient;

import guru.springframework.spring6restclient.client.BeerClient;
import guru.springframework.spring6restclient.client.BeerClientImpl;
import guru.springframework.spring6restclient.config.RestClientConfig;
import guru.springframework.spring6restclient.model.BeerDTO;
import guru.springframework.spring6restclient.model.BeerStyle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Mock tests for BeerClient using Spring Boot 4.0.2 and Spring Security 7.0.
 * <p>
 * This test class uses @RestClientTest slice to test RestClient with OAuth2 authentication
 * without requiring an actual OAuth2 authorization server.
 * <p>
 * Key Testing Components:
 * - MockRestServiceServer: Simulates HTTP responses without real network calls
 * - JsonMapper: Jackson 3 (tools.jackson.*) for JSON serialization
 * - @MockitoBean OAuth2AuthorizedClientManager: Mocked to return test tokens
 * - TestConfiguration: Provides minimal beans needed for OAuth2 RestClient testing
 * <p>
 * Configuration Approach:
 * - Imports RestClientConfig which provides OAuth2ClientHttpRequestInterceptor
 * - Provides ClientRegistrationRepository for OAuth2 client configuration
 * - Provides OAuth2AuthorizedClientService for token caching (required by manager)
 * - No custom interceptor needed - RestClientConfig provides Spring Security's built-in
 *
 * @author Updated for Spring Boot 4.x using OAuth2ClientHttpRequestInterceptor
 * @author Modified by Claude (Anthropic), 2026-02-12
 * @author Fixed and verified by Claude (Anthropic), 2026-02-14
 * @version Spring Boot 4.0.2, Spring Security 7.0, Jackson 3
 */
@RestClientTest
public class BeerClientMockTest {

    static final String URL = "http://localhost:8080";
    public static final String BEARER_TEST = "Bearer test";

    BeerClient beerClient;

    MockRestServiceServer server;

    @Autowired
    RestClient.Builder restClientBuilder;

    @Autowired
    JsonMapper jsonMapper;  // Jackson 3: tools.jackson.databind.json.JsonMapper

    BeerDTO dto;
    String dtoJson;

    @MockitoBean
    OAuth2AuthorizedClientManager authorizedClientManager;

    /**
     * Test configuration that provides minimal beans for OAuth2 RestClient testing.
     * <p>
     * Imports RestClientConfig which provides:
     * - OAuth2AuthorizedClientManager (created from mocked bean above)
     * - OAuth2ClientHttpRequestInterceptor (Spring Security 7.0 built-in)
     * - RestClient.Builder configured with OAuth2 support
     * <p>
     * Provides test-specific beans:
     * - ClientRegistrationRepository: In-memory repository with test OAuth2 client registration
     * - OAuth2AuthorizedClientService: Required by AuthorizedClientServiceOAuth2AuthorizedClientManager
     *   for token caching and management
     */
    @TestConfiguration
    @Import(RestClientConfig.class)
    public static class TestConfig {

        /**
         * Provides a test OAuth2 client registration for "springauth".
         * This matches the registration ID used in application.properties.
         *
         * @return In-memory client registration repository with test configuration
         */
        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return new InMemoryClientRegistrationRepository(ClientRegistration
                    .withRegistrationId("springauth")
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .clientId("test")
                    .tokenUri("test")
                    .build());
        }

        /**
         * Provides OAuth2AuthorizedClientService for storing authorized clients.
         * This is required by AuthorizedClientServiceOAuth2AuthorizedClientManager
         * for token caching and lifecycle management.
         * <p>
         * In tests, we use in-memory implementation. In production, this would
         * cache tokens to avoid repeated authorization server calls.
         *
         * @param clientRegistrationRepository The client registration repository
         * @return In-memory OAuth2 authorized client service
         */
        @Bean
        OAuth2AuthorizedClientService oAuth2AuthorizedClientService(
                ClientRegistrationRepository clientRegistrationRepository) {
            return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
        }
    }

    @Autowired
    ClientRegistrationRepository clientRegistrationRepository;

    /**
     * Set up method executed before each test.
     * <p>
     * Configures:
     * 1. Mock OAuth2 token that will be used in test requests
     * 2. MockRestServiceServer bound to RestClient.Builder
     * 3. BeerClient instance with configured RestClient.Builder
     * 4. Test DTO and its JSON representation
     */
    @BeforeEach
    void setUp() {
        ClientRegistration clientRegistration = clientRegistrationRepository
                .findByRegistrationId("springauth");

        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test",
                Instant.MIN,
                Instant.MAX
        );

        // Mock the OAuth2AuthorizedClientManager to return an authorized client
        // This simulates successful OAuth2 token acquisition
        when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenReturn(new OAuth2AuthorizedClient(
                        clientRegistration,
                        "test",
                        token
                ));

        // Bind MockRestServiceServer to the RestClient.Builder
        // This allows us to mock HTTP responses without actual network calls
        server = MockRestServiceServer.bindTo(restClientBuilder).build();

        // Create BeerClient with the configured RestClient.Builder
        // The builder already has OAuth2ClientHttpRequestInterceptor configured
        beerClient = new BeerClientImpl(restClientBuilder);

        dto = getBeerDto();
        dtoJson = jsonMapper.writeValueAsString(dto);
    }

    @Test
    void testListBeersWithQueryParam() {
        String response = jsonMapper.writeValueAsString(pagePayload());

        URI uri = UriComponentsBuilder.fromUriString(URL + BeerClientImpl.GET_BEER_PATH)
                .queryParam("beerName", "ALE")
                .build().toUri();

        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(uri))
                .andExpect(header("Authorization", BEARER_TEST))
                .andExpect(queryParam("beerName", "ALE"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        Page<BeerDTO> responsePage = beerClient
                .listBeers("ALE", null, null, null, null);

        assertThat(responsePage.getContent()).hasSize(1);
        server.verify();
    }

    @Test
    void testDeleteNotFound() {
        server.expect(method(HttpMethod.DELETE))
                .andExpect(requestToUriTemplate(URL + BeerClientImpl.GET_BEER_BY_ID_PATH,
                        dto.getId()))
                .andExpect(header("Authorization", BEARER_TEST))
                .andRespond(withResourceNotFound());

        UUID uuid = dto.getId();

        assertThatThrownBy(() -> beerClient.deleteBeer(uuid))
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageContaining("404 Not Found");

        server.verify();
    }

    @Test
    void testDeleteBeer() {
        server.expect(method(HttpMethod.DELETE))
                .andExpect(requestToUriTemplate(URL + BeerClientImpl.GET_BEER_BY_ID_PATH,
                        dto.getId()))
                .andExpect(header("Authorization", BEARER_TEST))
                .andRespond(withNoContent());

        beerClient.deleteBeer(dto.getId());

        server.verify();
    }

    @Test
    void testUpdateBeer() {
        server.expect(method(HttpMethod.PUT))
                .andExpect(requestToUriTemplate(URL + BeerClientImpl.GET_BEER_BY_ID_PATH,
                        dto.getId()))
                .andExpect(header("Authorization", BEARER_TEST))
                .andRespond(withNoContent());

        mockGetOperation();

        BeerDTO responseDto = beerClient.updateBeer(dto);
        assertThat(responseDto.getId()).isEqualTo(dto.getId());
        server.verify();
    }

    @Test
    void testCreateBeer() {
        URI uri = UriComponentsBuilder.fromPath(BeerClientImpl.GET_BEER_BY_ID_PATH)
                .build(dto.getId());

        server.expect(method(HttpMethod.POST))
                .andExpect(requestTo(URL + BeerClientImpl.GET_BEER_PATH))
                .andExpect(header("Authorization", BEARER_TEST))
                .andRespond(withAccepted().location(uri));

        mockGetOperation();

        BeerDTO responseDto = beerClient.createBeer(dto);
        assertThat(responseDto.getId()).isEqualTo(dto.getId());
        server.verify();
    }

    @Test
    void testGetById() {
        mockGetOperation();

        BeerDTO responseDto = beerClient.getBeerById(dto.getId());
        assertThat(responseDto.getId()).isEqualTo(dto.getId());
        server.verify();
    }

    private void mockGetOperation() {
        server.expect(method(HttpMethod.GET))
                .andExpect(requestToUriTemplate(URL + BeerClientImpl.GET_BEER_BY_ID_PATH,
                        dto.getId()))
                .andExpect(header("Authorization", BEARER_TEST))
                .andRespond(withSuccess(dtoJson, MediaType.APPLICATION_JSON));
    }

    @Test
    void testListBeers() {
        String payload = jsonMapper.writeValueAsString(pagePayload());

        server.expect(method(HttpMethod.GET))
                .andExpect(requestTo(URL + BeerClientImpl.GET_BEER_PATH))
                .andExpect(header("Authorization", BEARER_TEST))
                .andRespond(withSuccess(payload, MediaType.APPLICATION_JSON));

        Page<BeerDTO> dtos = beerClient.listBeers();
        assertThat(dtos.getContent()).hasSizeGreaterThan(0);
        server.verify();
    }

    BeerDTO getBeerDto(){
        return BeerDTO.builder()
                .id(UUID.randomUUID())
                .price(new BigDecimal("10.99"))
                .beerName("Mango Bobs")
                .beerStyle(BeerStyle.IPA)
                .quantityOnHand(500)
                .upc("123245")
                .build();
    }

    private Object pagePayload() {
        BeerDTO beer = getBeerDto();

        return Map.of(
                "content", List.of(beer),
                "page", Map.of(
                        "number", 1,
                        "size", 25,
                        "totalElements", 1
                )
        );
    }

}