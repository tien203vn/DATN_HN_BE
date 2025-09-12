package net.codejava.config;

import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.codejava.job.BookingCarCancelSyncJob;
import net.codejava.job.BookingCarCompleteJob;

@Configuration
public class QuartzConfig {
    @Bean
    public JobDetail bookingCarSyncJobDetail() {
        return JobBuilder.newJob(BookingCarCancelSyncJob.class)
                .withIdentity("bookingCarSyncJob", "sync-status-job")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger bookingCarSyncTrigger(JobDetail bookingCarSyncJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(bookingCarSyncJobDetail)
                .withIdentity("bookingCarSyncTrigger", "sync-status-trigger")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(60) // chạy mỗi 10 giây
                        .repeatForever())
                .build();
    }

    //    ============================job 2=====================================
    @Bean
    public JobDetail bookingCarCompleteSyncJobDetail() {
        return JobBuilder.newJob(BookingCarCompleteJob.class)
                .withIdentity("bookingCarCompleteSyncJob", "sync-active-job")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger bookingCarCompleteSyncTrigger(JobDetail bookingCarSyncJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(bookingCarCompleteSyncJobDetail())
                .withIdentity("bookingCarSyncTrigger", "sync-active-job")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(60) // chạy mỗi 10 giây
                        .repeatForever())
                .build();
    }
}
