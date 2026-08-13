package org.fsv.instagramuploader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class InstagramUploaderApplication {
 
 public static void main(String[] args) {
	SpringApplication.run(InstagramUploaderApplication.class, args);
 }
}
