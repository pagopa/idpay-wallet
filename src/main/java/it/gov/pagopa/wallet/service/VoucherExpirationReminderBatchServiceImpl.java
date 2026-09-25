package it.gov.pagopa.wallet.service;

import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.NotificationQueueDTO;
import it.gov.pagopa.wallet.event.producer.ErrorProducer;
import it.gov.pagopa.wallet.event.producer.NotificationProducer;
import it.gov.pagopa.wallet.exception.custom.ReminderBatchException;
import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.repository.WalletRepository;
import it.gov.pagopa.wallet.repository.WalletUpdatesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class VoucherExpirationReminderBatchServiceImpl implements VoucherExpirationReminderBatchService {

    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Rome");

    // [TEMP-SIMULATION] REMOVE BEFORE MERGE: forces a single failure on the 2nd initiative to trigger the
    // cronjob OnFailure retry. The flag is static/in-memory: it survives across retries because the wallet
    // service stays up (only the curl cronjob container restarts), so the retry bypasses the forced failure.
    private static final AtomicBoolean SIMULATED_FAILURE_DONE = new AtomicBoolean(false);

    private final WalletRepository walletRepository;
    private final WalletUpdatesRepository walletUpdatesRepository;
    private final NotificationProducer notificationProducer;
    private final ErrorProducer errorProducer;
    private final int blockReminderBatch;
    private final String notificationTopic;
    private final String notificationServer;

    public VoucherExpirationReminderBatchServiceImpl(WalletRepository walletRepository,
                                                     WalletUpdatesRepository walletUpdatesRepository,
                                                     NotificationProducer notificationProducer,
                                                     ErrorProducer errorProducer,
                                                     @Value("${app.wallet.blockReminderBatch}") int blockReminderBatch,
                                                     @Value("${spring.cloud.stream.binders.kafka-notification.environment.spring.cloud.stream.kafka.binder.brokers}") String notificationServer,
                                                     @Value("${spring.cloud.stream.bindings.walletQueue-out-2.destination}") String notificationTopic
    ) {
        this.walletRepository = walletRepository;
        this.walletUpdatesRepository = walletUpdatesRepository;
        this.notificationProducer = notificationProducer;
        this.errorProducer = errorProducer;
        this.blockReminderBatch = blockReminderBatch;
        this.notificationTopic = notificationTopic;
        this.notificationServer = notificationServer;
    }


    public void runReminderBatch(String initiativeId, int expiringDay) {
        long startTime = System.currentTimeMillis();
        executeBatchLogic(initiativeId, expiringDay);
        performanceLog(startTime, WalletConstants.REMINDER);
    }

    @Override
    public void runReminderBatch(List<String> initiativeIds, int expiringDay) {
        List<String> failedInitiatives = new ArrayList<>();
        int iteration = 0;
        for (String initiativeId : initiativeIds) {
            iteration++;
            long startTime = System.currentTimeMillis();
            try {
                // [TEMP-SIMULATION] REMOVE BEFORE MERGE: throw only on the 2nd iteration and only on the first
                // overall run; on retry compareAndSet returns false so the initiative is processed normally.
                if (iteration == 2 && SIMULATED_FAILURE_DONE.compareAndSet(false, true)) {
                    throw new ReminderBatchException("[SIMULATION] forced failure on iteration 2 to trigger the cronjob retry");
                }
                executeBatchLogic(initiativeId, expiringDay);
            } catch (Exception e) {
                failedInitiatives.add(sanitizeString(initiativeId));
                log.error("[REMINDER_BATCH] An error occurred while processing the initiative {}. Continuing with the remaining initiatives",
                        sanitizeString(initiativeId), e);
            } finally {
                performanceLog(startTime, WalletConstants.REMINDER);
            }
        }
        if (!failedInitiatives.isEmpty()) {
            // surface a 5xx so the cronjob OnFailure restart policy retries; other initiatives were still processed
            throw new ReminderBatchException(
                    String.format(WalletConstants.ExceptionMessage.ERROR_REMINDER_BATCH_MSG, failedInitiatives));
        }
    }

    private void executeBatchLogic(String initiativeId, int expiringDay) {
        String sanitizedInitiativeId = sanitizeString(initiativeId);

        LocalDate now = LocalDate.now();
        LocalDate expirationDate = now.plusDays((long)expiringDay-1);
        // idempotency window: skip wallets already reminded in the current run day so job retries do not duplicate notifications
        LocalDateTime cycleStart = LocalDate.now(ZONE_ID).atStartOfDay();

        int page = 0;
        Pageable pageable = PageRequest.of(page, blockReminderBatch);
        Page<Wallet> walletPage;

        log.info("[REMINDER_BATCH] Searching for expiring vouchers for the initiative {} and expirationDateUTC {}", sanitizedInitiativeId, expirationDate);

        do {
            LocalDate target = LocalDate.now(ZONE_ID).plusDays(expiringDay);
            Instant startUtc = target.atStartOfDay(ZONE_ID).toInstant();
            Instant endUtc   = target.plusDays(1).atStartOfDay(ZONE_ID).toInstant();
            walletPage = walletRepository.findVoucherExpiredIntoRange(initiativeId, Date.from(startUtc), Date.from(endUtc), pageable);
            List<Wallet> walletList = walletPage.getContent();
            log.info("[REMINDER_BATCH] Page {} - {} expiring vouchers found", page, walletList.size());

            if (!walletList.isEmpty()) {
                log.info("[REMINDER_BATCH] Start sending notifications for expiring vouchers - Page {}", page);
                for (Wallet wallet : walletList) {
                    if (alreadyReminded(wallet, cycleStart)) {
                        log.info("[REMINDER_BATCH] Skipping wallet already reminded in this cycle for initiative {}", sanitizedInitiativeId);
                        continue;
                    }

                    NotificationQueueDTO notificationQueueDTO = NotificationQueueDTO.builder()
                            .operationType(WalletConstants.REMINDER)
                            .userId(wallet.getUserId())
                            .initiativeId(wallet.getInitiativeId())
                            .serviceId(wallet.getServiceId())
                            .channel(wallet.getChannel())
                            .initiativeName(wallet.getInitiativeName())
                            .name(wallet.getName())
                            .surname(wallet.getSurname())
                            .userMail(wallet.getUserMail())
                            .voucherEndDate(wallet.getVoucherEndDate())
                            .build();

                    if (sendNotification(notificationQueueDTO)) {
                        // mark only on successful send so a failed one is retried, not skipped
                        walletUpdatesRepository.updateReminderNotifiedDate(
                                wallet.getInitiativeId(), wallet.getUserId(), LocalDateTime.now());
                    }
                }
                log.info("[REMINDER_BATCH] End sending notifications for expiring vouchers - Page {}", page);
            }
            pageable = walletPage.nextPageable();
            page++;
        } while (walletPage.hasNext());

    }

    private boolean alreadyReminded(Wallet wallet, LocalDateTime cycleStart) {
        return wallet.getReminderNotifiedDate() != null
                && !wallet.getReminderNotifiedDate().isBefore(cycleStart);
    }

    private boolean sendNotification(NotificationQueueDTO notificationQueueDTO) {
        try {
            log.info("[SEND_NOTIFICATION] Sending event to Notification");
            notificationProducer.sendNotification(notificationQueueDTO);
            return true;
        } catch (Exception e) {
            log.error("[SEND_NOTIFICATION] An error has occurred. Sending message to Error queue");
            final MessageBuilder<?> errorMessage = MessageBuilder.withPayload(notificationQueueDTO);
            this.sendToQueueError(e, errorMessage, notificationServer, notificationTopic);
            return false;
        }
    }

    private void sendToQueueError(
            Exception e, MessageBuilder<?> errorMessage, String server, String topic) {
        errorMessage
                .setHeader(WalletConstants.ERROR_MSG_HEADER_SRC_TYPE, WalletConstants.KAFKA)
                .setHeader(WalletConstants.ERROR_MSG_HEADER_SRC_SERVER, server)
                .setHeader(WalletConstants.ERROR_MSG_HEADER_SRC_TOPIC, topic)
                .setHeader(WalletConstants.ERROR_MSG_HEADER_DESCRIPTION, WalletConstants.ERROR_QUEUE)
                .setHeader(WalletConstants.ERROR_MSG_HEADER_RETRYABLE, true)
                .setHeader(WalletConstants.ERROR_MSG_HEADER_STACKTRACE, e.getStackTrace())
                .setHeader(WalletConstants.ERROR_MSG_HEADER_CLASS, e.getClass())
                .setHeader(WalletConstants.ERROR_MSG_HEADER_MESSAGE, e.getMessage());
        errorProducer.sendEvent(errorMessage.build());
    }

    private void performanceLog(long startTime, String service) {
        log.info(
                "[PERFORMANCE_LOG] [{}] Time occurred to perform business logic: {} ms",
                service,
                System.currentTimeMillis() - startTime);
    }

    public static String sanitizeString(String str) {
        return str == null ? null : str.replaceAll("[\\r\\n]", "").replaceAll("[^\\w\\s-]", "");
    }

}
