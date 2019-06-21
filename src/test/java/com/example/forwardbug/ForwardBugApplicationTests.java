package com.example.forwardbug;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@AutoConfigureWireMock(port = 0)
@Import(ForwardBugApplicationTests.Config.class)
@EnableAutoConfiguration
public class ForwardBugApplicationTests {

	@Autowired
	private WebTestClient webTestClient;

	@Before
	public void setup() {
		WireMock.reset();
		WireMock.stubFor(WireMock.get("/luckyNumber")
			.willReturn(ResponseDefinitionBuilder.responseDefinition()
				.withBody("{\"number\":7}")
				.withHeader("Content-Type", "application/json")));
	}

	/**
	 * this passes
	 */
	@Test
	public void testDirectUri() {
		webTestClient
			.get()
			.uri("/luckyNumber")
			.exchange()
			.expectBody()
			.json("{\"number\":7}");
	}

	/**
	 * this fails
	 */
	@Test
	public void testForwardedUri() {
		webTestClient
			.get()
			.uri("/shouldForward")
			.exchange()
			.expectBody()
			.json("{\"number\":7}");

	}

	@Configuration
	static class Config {
		@Bean
		public RouteLocator routes(RouteLocatorBuilder builder, @Value("${wiremock.server.port}") int port) {
			return builder.routes()
				.route(p -> p.path("/luckyNumber").uri("http://localhost:" + port))
				.route(p -> p.path("/shouldForward").uri("forward:///luckyNumber"))
				.build();
		}
	}
}
