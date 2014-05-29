package com.github.mxab;

import java.io.File;

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public GradleConnector gradleConnector() {
		GradleConnector connector = GradleConnector.newConnector();

		connector.forProjectDirectory(new File(System.getProperty("user.dir")));

		return connector;

	}

	@Bean(destroyMethod = "close")
	public ProjectConnection projectConnection(GradleConnector connector) {

		ProjectConnection connect = connector.connect();

		return connect;
	}
}
