package com.ypc.solrdemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.ypc.solrdemo.dao")
public class SolrdemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SolrdemoApplication.class, args);
	}
}
