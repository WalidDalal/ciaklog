package com.project.ciaklog;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Richiede un database e delle chiavi reali non disponibili sul runner CI — da riattivare quando avremo un profilo di test con H2")
class CiaklogApplicationTests {

	@Test
	void contextLoads() {
	}
}