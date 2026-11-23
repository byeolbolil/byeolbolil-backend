package xyz.ncookie.stargazer.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 통합 테스트용 MySQL, Redis 컨테이너.
 *
 * 이미지 태그는 docker-compose.dev.yml과 맞춘다. 컨테이너는 스프링 컨텍스트와 수명이 같고,
 * 컨텍스트가 테스트 클래스 사이에서 캐시되므로 전체 테스트에서 한 번만 뜬다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig {

	@Bean
	@ServiceConnection
	MySQLContainer mysqlContainer() {
		return new MySQLContainer(DockerImageName.parse("mysql:8.0"))
			.withDatabaseName("stargazer")
			.withEnv("TZ", "Asia/Seoul")
			.withCommand(
				"--character-set-server=utf8mb4",
				"--collation-server=utf8mb4_unicode_ci"
			);
	}

	@Bean
	@ServiceConnection(name = "redis")
	GenericContainer<?> redisContainer() {
		return new GenericContainer<>(DockerImageName.parse("redis:alpine"))
			.withExposedPorts(6379);
	}
}
