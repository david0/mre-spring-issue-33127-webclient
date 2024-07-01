package com.github.david0.webclient_prematureend;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.ProblemDetail;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.netty.http.client.HttpClient;

class WebclientTest {


	@RegisterExtension
	static WireMockExtension wireMockExtension = WireMockExtension.newInstance().build();
	WebClient webClient = WebClient.builder()
			.clientConnector(new ReactorClientHttpConnector(HttpClient.create()
					.compress(false))) // different from default ReactorClientHttpConnectors configuration
			.baseUrl(wireMockExtension.baseUrl()).build();


	@Test
	void test() {
		wireMockExtension.stubFor(post(urlEqualTo("/"))
				.willReturn(aResponse()
						.withHeader("Content-Length", "2000")
						.withHeader("Content-Type", "application/json")
						.withBody("{")));

		var ex = assertThrows(WebClientException.class, () -> webClient.post().uri("/").retrieve()
				.onStatus(s -> s.isError(),
						resp -> resp.bodyToMono(ProblemDetail.class).map(httpBody -> new RuntimeException("problem" + httpBody)))
				.bodyToMono(JsonNode.class).block());

		ex.printStackTrace();
		assertEquals("Connection prematurely closed DURING response", ex.getCause().getMessage());
		assertEquals("[helpful error message]", ex.getMessage());
	}
}
