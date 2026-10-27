package com.abcde.hdbresale.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables the 24-hour valuation refresh (FR-28) and housekeeping jobs. */
@Configuration
@EnableScheduling
public class SchedulerConfig {
}
