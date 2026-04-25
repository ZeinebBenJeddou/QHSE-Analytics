package com.QHSEAnalytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.QHSEAnalytics", "com.qhseanalytics.importengine"})
public class QhseAnalyticsApplication {

	public static void main(String[] args) {
		SpringApplication.run(QhseAnalyticsApplication.class, args);
	}

}
