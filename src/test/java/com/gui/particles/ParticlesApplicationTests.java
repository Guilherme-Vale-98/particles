package com.gui.particles;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParticlesApplicationTests extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
		assertThat(mockMvc).isNotNull();
	}

}
