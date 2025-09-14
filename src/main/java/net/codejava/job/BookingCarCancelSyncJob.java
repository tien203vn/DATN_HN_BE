package net.codejava.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import net.codejava.service.BookingService;

@DisallowConcurrentExecution
public class BookingCarCancelSyncJob implements Job {

    @Autowired
    private BookingService bookingService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
       // bookingService.syncCancelStatus();
    }
}
