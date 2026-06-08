package com.erumpay.auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
	"spring.datasource.url=jdbc:h2:mem:auth_service_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
	"jwt.secret=test-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256",
	"encryption.aes-secret-key=0123456789abcdef0123456789abcdef",
	"encryption.phone-hash-salt=test-phone-hash-salt",
	"kakao.client-id=test-kakao-client-id",
	"kakao.client-secret=test-kakao-client-secret",
	"kakao.redirect-uri=http://localhost:8081/test/kakao/callback",
	"octomo.api-key=test-octomo-api-key"
})
class AuthServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
