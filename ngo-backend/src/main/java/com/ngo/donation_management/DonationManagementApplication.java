package com.ngo.donation_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class DonationManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(DonationManagementApplication.class, args);
	}

}
