package net.codejava.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import net.codejava.service.BookingService;

public class BookingCarCompleteJob implements Job {

    @Autowired
    private BookingService bookingService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
       // bookingService.syncCarBookingComplete();
    }
}
