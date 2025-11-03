package it.gov.pagopa.wallet.service;

import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.NotificationQueueDTO;
import it.gov.pagopa.wallet.event.producer.ErrorProducer;
import it.gov.pagopa.wallet.event.producer.NotificationProducer;
import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.repository.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class VoucherExpirationReminderBatchServiceImpl implements VoucherExpirationReminderBatchService {

    private final WalletRepository walletRepository;
    private final NotificationProducer notificationProducer;
    private final ErrorProducer errorProducer;
    private final int blockReminderBatch;
    private final String notificationTopic;
    private final String notificationServer;

    public VoucherExpirationReminderBatchServiceImpl(WalletRepository walletRepository,
                                                     NotificationProducer notificationProducer,
                                                     ErrorProducer errorProducer,
                                                     @Value("${app.wallet.blockReminderBatch}") int blockReminderBatch,
                                                     @Value("${spring.cloud.stream.binders.kafka-notification.environment.spring.cloud.stream.kafka.binder.brokers}") String notificationServer,
                                                     @Value("${spring.cloud.stream.bindings.walletQueue-out-2.destination}") String notificationTopic
    ) {
        this.walletRepository = walletRepository;
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

    private void executeBatchLogic(String initiativeId, int expiringDay) {
        String sanitizedInitiativeId = sanitizeString(initiativeId);

        LocalDate now = LocalDate.now();
        LocalDate expirationDate = now.plusDays((long)expiringDay-1);
        String expirationDateString = expirationDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

        int page = 0;
        Pageable pageable = PageRequest.of(page, blockReminderBatch);
        Page<Wallet> walletPage;

        log.info("[REMINDER_BATCH] Searching for expiring vouchers for the initiative {} and expirationDate {}", sanitizedInitiativeId, expirationDate);

        do {
            walletPage = walletRepository.findByInitiativeIdAndVoucherEndDateIgnoringTime(initiativeId, expirationDateString, pageable);
            List<Wallet> walletList = walletPage.getContent();
            log.info("[REMINDER_BATCH] Page {} - {} expiring vouchers found", page, walletList.size());

            if (!walletList.isEmpty()) {
                log.info("[REMINDER_BATCH] Start sending notifications for expiring vouchers - Page {}", page);
                for (Wallet wallet : walletList) {
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

                    sendNotification(notificationQueueDTO);
                }
                log.info("[REMINDER_BATCH] End sending notifications for expiring vouchers - Page {}", page);
            }
            pageable = walletPage.nextPageable();
            page++;
        } while (walletPage.hasNext());

    }

    private void sendNotification(NotificationQueueDTO notificationQueueDTO) {
        try {
            log.info("[SEND_NOTIFICATION] Sending event to Notification");
            notificationProducer.sendNotification(notificationQueueDTO);
        } catch (Exception e) {
            log.error("[SEND_NOTIFICATION] An error has occurred. Sending message to Error queue");
            final MessageBuilder<?> errorMessage = MessageBuilder.withPayload(notificationQueueDTO);
            this.sendToQueueError(e, errorMessage, notificationServer, notificationTopic);
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
