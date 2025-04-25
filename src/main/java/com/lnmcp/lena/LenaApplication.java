package com.lnmcp.lena;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for the MCP (Message Context Protocol) server.
 * This server integrates Spring Boot with Spring AI to process user prompts,
 * extract context from documents and database, and generate AI responses.
 */
@SpringBootApplication
@EnableAsync
public class LenaApplication {

	public static void main(String[] args) {
		SpringApplication.run(LenaApplication.class, args);
		System.out.println("MCP Server started successfully!");
		System.out.println("API available at: http://localhost:8080/api/mcp/prompt");
	}

}
