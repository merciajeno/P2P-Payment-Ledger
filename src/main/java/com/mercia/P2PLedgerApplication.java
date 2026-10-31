package com.mercia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class P2PLedgerApplication {

	public static void main(String[] args) {
		SpringApplication.run(P2PLedgerApplication.class, args);
	}

}
