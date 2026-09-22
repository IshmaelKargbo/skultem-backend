package com.moriba.skultem.infrastructure.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.moriba.skultem.application.usecase.BackfillPlatformFeesUseCase;

import lombok.RequiredArgsConstructor;

// Runs once, right after the app finishes starting up - ensures every school has a platform fee
// setting row (copying a default amount from other schools where needed) and catches up any
// school/student that's missing the fee itself. See BackfillPlatformFeesUseCase for the actual
// work; this class is just the "on start" trigger for it.
@Component
@Profile("!test")
@RequiredArgsConstructor
public class PlatformFeeStartupListener implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformFeeStartupListener.class);

    private final BackfillPlatformFeesUseCase backfillPlatformFeesUseCase;

    @Override
    public void run(ApplicationArguments args) {
        try {
            backfillPlatformFeesUseCase.execute();
        } catch (Exception e) {
            // Never block the app from starting over this - worst case, this quietly runs again
            // next restart.
            log.error("Platform fee startup backfill failed", e);
        }
    }
}
