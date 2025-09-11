package net.codejava.config;

import org.springframework.context.annotation.Configuration;
import net.codejava.job.BookingCarSyncJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class QuartzConfig {
    @Bean
    public JobDetail bookingCarSyncJobDetail() {
        return JobBuilder.newJob(BookingCarSyncJob.class)
                .withIdentity("bookingCarSyncJob", "syncStatus")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger bookingCarSyncTrigger(JobDetail bookingCarSyncJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(bookingCarSyncJobDetail)
                .withIdentity("bookingCarSyncTrigger", "syncStatus")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(10)   // chạy mỗi 10 giây
                        .repeatForever())
                .build();
    }
}
