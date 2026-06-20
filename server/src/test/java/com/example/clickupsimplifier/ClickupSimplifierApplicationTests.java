package com.example.clickupsimplifier;

import com.example.clickupsimplifier.persistence.PostgresTestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresTestcontainersConfig.class)
class ClickupSimplifierApplicationTests {

	@Test
	void contextLoads() {
	}

}
