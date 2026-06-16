package com.athena.interview.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WeeklyInterviewScheduler {

    @Scheduled(cron = "${athena.interview.weekly-cron:0 0 6 * * MON}", zone = "UTC")
    public void triggerWeeklyInterviews() {
        log.info("Weekly interview scheduler tick — due-user roster sourcing is pending (see class javadoc)");
    }

}
