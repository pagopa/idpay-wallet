package it.gov.pagopa.wallet.service;

import feign.FeignException;
import it.gov.pagopa.wallet.connector.InitiativeRestConnector;
import it.gov.pagopa.wallet.connector.OnboardingRestConnector;
import it.gov.pagopa.wallet.connector.PaymentInstrumentRestConnector;
import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.Counters;
import it.gov.pagopa.wallet.dto.DeactivationBodyDTO;
import it.gov.pagopa.wallet.dto.EnrollmentStatusDTO;
import it.gov.pagopa.wallet.dto.EvaluationDTO;
import it.gov.pagopa.wallet.dto.IbanQueueDTO;
import it.gov.pagopa.wallet.dto.IbanQueueWalletDTO;
import it.gov.pagopa.wallet.dto.InitiativeListDTO;
import it.gov.pagopa.wallet.dto.InstrumentAckDTO;
import it.gov.pagopa.wallet.dto.InstrumentCallBodyDTO;
import it.gov.pagopa.wallet.dto.NotificationQueueDTO;
import it.gov.pagopa.wallet.dto.QueueOperationDTO;
import it.gov.pagopa.wallet.dto.RefundDTO;
import it.gov.pagopa.wallet.dto.RewardTransactionDTO;
import it.gov.pagopa.wallet.dto.UnsubscribeCallDTO;
import it.gov.pagopa.wallet.dto.WalletDTO;
import it.gov.pagopa.wallet.dto.WalletPIBodyDTO;
import it.gov.pagopa.wallet.dto.WalletPIDTO;
import it.gov.pagopa.wallet.dto.initiative.InitiativeDTO;
import it.gov.pagopa.wallet.dto.mapper.TimelineMapper;
import it.gov.pagopa.wallet.dto.mapper.WalletMapper;
import it.gov.pagopa.wallet.enums.WalletStatus;
import it.gov.pagopa.wallet.event.producer.ErrorProducer;
import it.gov.pagopa.wallet.event.producer.IbanProducer;
import it.gov.pagopa.wallet.event.producer.NotificationProducer;
import it.gov.pagopa.wallet.event.producer.TimelineProducer;
import it.gov.pagopa.wallet.exception.WalletException;
import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.model.Wallet.RefundHistory;
import it.gov.pagopa.wallet.repository.WalletRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.iban4j.IbanUtil;
import org.iban4j.UnsupportedCountryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WalletServiceImpl implements WalletService {

  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100L);

  @Autowired WalletRepository walletRepository;

  @Autowired PaymentInstrumentRestConnector paymentInstrumentRestConnector;

  @Autowired OnboardingRestConnector onboardingRestConnector;
  @Autowired IbanProducer ibanProducer;
  @Autowired TimelineProducer timelineProducer;
  @Autowired WalletMapper walletMapper;
  @Autowired TimelineMapper timelineMapper;
  @Autowired ErrorProducer errorProducer;
  @Autowired NotificationProducer notificationProducer;
  @Autowired InitiativeRestConnector initiativeRestConnector;
  @Value(
      "${spring.cloud.stream.binders.kafka-timeline.environment.spring.cloud.stream.kafka.binder.brokers}")
  String timelineServer;
  @Value("${spring.cloud.stream.bindings.walletQueue-out-1.destination}")
  String timelineTopic;
  @Value(
      "${spring.cloud.stream.binders.kafka-notification.environment.spring.cloud.stream.kafka.binder.brokers}")
  String notificationServer;
  @Value("${spring.cloud.stream.bindings.walletQueue-out-2.destination}")
  String notificationTopic;
  @Value(
      "${spring.cloud.stream.binders.kafka-iban.environment.spring.cloud.stream.kafka.binder.brokers}")
  String ibanServer;
  @Value("${spring.cloud.stream.bindings.walletQueue-out-0.destination}")
  String ibanTopic;

  @Override
  public EnrollmentStatusDTO getEnrollmentStatus(String initiativeId, String userId) {
    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);
    return new EnrollmentStatusDTO(wallet.getStatus());
  }

  @Override
  public WalletDTO getWalletDetail(String initiativeId, String userId) {
    Wallet wallet =
        walletRepository
            .findByInitiativeIdAndUserId(initiativeId, userId)
            .orElseThrow(
                () ->
                    new WalletException(
                        HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND));
    return walletMapper.toInitiativeDTO(wallet);
  }

  @Override
  public void enrollInstrument(String initiativeId, String userId, String idWallet) {
    log.info("[ENROLL_INSTRUMENT] Checking the status of initiative {}", initiativeId);

    getInitiative(initiativeId);

    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);

    if (wallet.getStatus().equals(WalletStatus.UNSUBSCRIBED)) {
      throw new WalletException(
          HttpStatus.BAD_REQUEST.value(), WalletConstants.ERROR_INITIATIVE_UNSUBSCRIBED);
    }
    InstrumentCallBodyDTO dto =
        new InstrumentCallBodyDTO(userId, initiativeId, idWallet, WalletConstants.CHANNEL_APP_IO);

    try {
      log.info("[ENROLL_INSTRUMENT] Calling Payment Instrument");
      paymentInstrumentRestConnector.enrollInstrument(dto);
    } catch (FeignException e) {
      log.error("[ENROLL_INSTRUMENT] Error in Payment Instrument Request");
      throw new WalletException(e.status(), e.getMessage());
    }
  }

  @Override
  public void deleteInstrument(String initiativeId, String userId, String instrumentId) {
    log.info("[DELETE_INSTRUMENT] Checking the status of initiative {}", initiativeId);

    getInitiative(initiativeId);

    findByInitiativeIdAndUserId(initiativeId, userId);

    DeactivationBodyDTO dto = new DeactivationBodyDTO(userId, initiativeId, instrumentId);

    try {
      paymentInstrumentRestConnector.deleteInstrument(dto);
    } catch (FeignException e) {
      throw new WalletException(e.status(), e.getMessage());
    }
  }

  @Override
  public void enrollIban(String initiativeId, String userId, String iban, String description) {
    log.info("[ENROLL_IBAN] Checking the status of initiative {}", initiativeId);

    getInitiative(initiativeId);

    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);
    if (wallet.getStatus().equals(WalletStatus.UNSUBSCRIBED)) {
      throw new WalletException(
          HttpStatus.BAD_REQUEST.value(), WalletConstants.ERROR_INITIATIVE_UNSUBSCRIBED);
    }

    iban = iban.toUpperCase();
    this.formalControl(iban);
    if (wallet.getIban() == null || !(wallet.getIban().equals(iban))) {
      wallet.setIban(iban);
      IbanQueueDTO ibanQueueDTO =
          new IbanQueueDTO(
              userId,
              initiativeId,
              iban,
              description,
              WalletConstants.CHANNEL_APP_IO,
              LocalDateTime.now());

      try {
        log.info("[ENROLL_IBAN] Sending event to IBAN");
        ibanProducer.sendIban(ibanQueueDTO);
      } catch (Exception e) {
        log.error("[ENROLL_IBAN] An error has occurred. Sending message to Error queue");
        final MessageBuilder<?> errorMessage = MessageBuilder.withPayload(ibanQueueDTO);
        this.sendToQueueError(e, errorMessage, ibanServer, ibanTopic);
      }
    }

    setStatus(wallet);

    walletRepository.save(wallet);

    sendToTimeline(timelineMapper.ibanToTimeline(initiativeId, userId, iban));
  }

  @Override
  public InitiativeListDTO getInitiativeList(String userId) {
    List<Wallet> walletList = walletRepository.findByUserId(userId);
    InitiativeListDTO initiativeListDTO = new InitiativeListDTO();
    List<WalletDTO> walletDTOList = new ArrayList<>();

    for (Wallet wallet : walletList) {
      walletDTOList.add(walletMapper.toInitiativeDTO(wallet));
    }
    initiativeListDTO.setInitiativeList(walletDTOList);
    return initiativeListDTO;
  }

  @Override
  public void createWallet(EvaluationDTO evaluationDTO) {
    if (evaluationDTO.getStatus().equals(WalletConstants.STATUS_ONBOARDING_OK)) {
      Wallet wallet = walletMapper.map(evaluationDTO);
      walletRepository.save(wallet);
      sendToTimeline(timelineMapper.onboardingToTimeline(evaluationDTO));
    }
  }

  @Override
  public void unsubscribe(String initiativeId, String userId) {
    log.info("[UNSUBSCRIBE] Unsubscribing user");
    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);
    String statusTemp = wallet.getStatus();
    if (!wallet.getStatus().equals(WalletStatus.UNSUBSCRIBED)) {
      wallet.setStatus(WalletStatus.UNSUBSCRIBED);
      wallet.setRequestUnsubscribeDate(LocalDateTime.now());
      walletRepository.save(wallet);
      log.info("[UNSUBSCRIBE] Wallet disabled");
      UnsubscribeCallDTO unsubscribeCallDTO =
          new UnsubscribeCallDTO(
              initiativeId, userId, wallet.getRequestUnsubscribeDate().toString());

      try {
        onboardingRestConnector.disableOnboarding(unsubscribeCallDTO);
        log.info("[UNSUBSCRIBE] Onboarding disabled");
      } catch (FeignException e) {
        this.rollbackWallet(statusTemp, wallet);
        throw new WalletException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
      }
      try {
        paymentInstrumentRestConnector.disableAllInstrument(unsubscribeCallDTO);
        log.info("[UNSUBSCRIBE] Payment instruments disabled");
      } catch (FeignException e) {
        this.rollbackWallet(statusTemp, wallet);
        onboardingRestConnector.rollback(initiativeId, userId);
        throw new WalletException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
      }
    }
  }

  @Override
  public void processTransaction(RewardTransactionDTO rewardTransactionDTO) {
    log.info("[PROCESS_TRANSACTION] Transaction not in status REWARDED, skipping message");
    if (!rewardTransactionDTO.getStatus().equals("REWARDED")) {
      return;
    }
    log.info("[PROCESS_TRANSACTION] New trx from Rule Engine");
    rewardTransactionDTO
        .getRewards()
        .forEach(
            (initiativeId, reward) -> {
              log.info("[PROCESS_TRANSACTION] Processing initiative: {}", initiativeId);
              updateWalletFromTransaction(
                  initiativeId,
                  rewardTransactionDTO,
                  reward.getCounters(),
                  reward.getAccruedReward());
            });
  }

  @Override
  public void updateWallet(WalletPIBodyDTO walletPIDTO) {
    for (WalletPIDTO walletPI : walletPIDTO.getWalletDTOlist()) {
      Wallet wallet = findByInitiativeIdAndUserId(walletPI.getInitiativeId(), walletPI.getUserId());
      wallet.setNInstr(wallet.getNInstr() - 1);
      this.setStatus(wallet);
      walletRepository.save(wallet);
      QueueOperationDTO queueOperationDTO =
          timelineMapper.deleteInstrumentToTimeline(
              wallet.getInitiativeId(),
              wallet.getUserId(),
              walletPI.getMaskedPan(),
              walletPI.getBrandLogo());

      sendToTimeline(queueOperationDTO);
    }
  }

  @Override
  public void processAck(InstrumentAckDTO instrumentAckDTO) {

    if (!instrumentAckDTO.getOperationType().endsWith("KO")) {

      Wallet wallet =
          findByInitiativeIdAndUserId(
              instrumentAckDTO.getInitiativeId(), instrumentAckDTO.getUserId());

      wallet.setNInstr(instrumentAckDTO.getNinstr());

      setStatus(wallet);

      walletRepository.save(wallet);
    }

    QueueOperationDTO queueOperationDTO = timelineMapper.ackToTimeline(instrumentAckDTO);

    sendToTimeline(queueOperationDTO);
  }

  @Override
  public void processRefund(RefundDTO refundDTO) {
    log.info("[PROCESS_REFUND] Processing new refund");

    Wallet wallet =
        walletRepository
            .findByInitiativeIdAndUserId(refundDTO.getInitiativeId(), refundDTO.getUserId())
            .orElse(null);

    if (wallet == null) {
      log.info("[PROCESS_REFUND] Wallet not found, skipping message");
      return;
    }

    Map<String, RefundHistory> history = wallet.getRefundHistory();

    if (history == null) {
      history = new HashMap<>();
      wallet.setRefundHistory(history);
    }

    if (history.containsKey(refundDTO.getRewardNotificationId())
        && history.get(refundDTO.getRewardNotificationId()).getFeedbackProgressive()
            >= refundDTO.getFeedbackProgressive()) {
      log.info("[PROCESS_REFUND] Feedback already processed, skipping message");
      return;
    }

    BigDecimal refunded =
        BigDecimal.valueOf(refundDTO.getRewardCents())
            .divide(ONE_HUNDRED, 2, RoundingMode.HALF_DOWN);

    wallet.setRefunded(wallet.getRefunded().add(refunded));

    history.put(
        refundDTO.getRewardNotificationId(), new RefundHistory(refundDTO.getFeedbackProgressive()));

    walletRepository.save(wallet);

    QueueOperationDTO queueOperationDTO = timelineMapper.refundToTimeline(refundDTO);

    sendToTimeline(queueOperationDTO);

    NotificationQueueDTO notificationQueueDTO =
        NotificationQueueDTO.builder()
            .operationType("REFUND")
            .userId(refundDTO.getUserId())
            .initiativeId(refundDTO.getInitiativeId())
            .rewardNotificationId(refundDTO.getRewardNotificationId())
            .refundReward(refundDTO.getEffectiveRewardCents())
            .rejectionCode(refundDTO.getRejectionCode())
            .rejectionReason(refundDTO.getRejectionReason())
            .refundDate(refundDTO.getExecutionDate())
            .refundFeedbackProgressive(refundDTO.getFeedbackProgressive())
            .refundCro(refundDTO.getCro())
            .status(refundDTO.getStatus())
            .build();

    sendNotification(notificationQueueDTO);
  }

  private void updateWalletFromTransaction(
      String initiativeId,
      RewardTransactionDTO rewardTransactionDTO,
      Counters counters,
      BigDecimal accruedReward) {
    Wallet wallet =
        walletRepository
            .findByInitiativeIdAndUserId(initiativeId, rewardTransactionDTO.getUserId())
            .orElse(null);

    if (wallet == null) {
      log.info("[updateWalletFromTransaction] No wallet found for this initiativeId");
      return;
    }

    log.info(
        "[updateWalletFromTransaction] Found wallet for initiative and user: {} {}",
        initiativeId,
        rewardTransactionDTO.getUserId());
    wallet.setNTrx(counters.getTrxNumber());
    log.info("[updateWalletFromTransaction] New value for nTrx: {}", wallet.getNTrx());
    wallet.setAccrued(counters.getTotalReward());
    log.info("[updateWalletFromTransaction] New value for Accrued: {}", wallet.getAccrued());
    wallet.setAmount(
        counters
            .getInitiativeBudget()
            .subtract(counters.getTotalReward())
            .setScale(2, RoundingMode.HALF_DOWN));
    log.info("[updateWalletFromTransaction] New value for Amount: {}", wallet.getAmount());
    walletRepository.save(wallet);

    log.info("[updateWalletFromTransaction] Sending transaction to Timeline");
    sendToTimeline(
        timelineMapper.transactionToTimeline(initiativeId, rewardTransactionDTO, accruedReward));
  }

  private Wallet findByInitiativeIdAndUserId(String initiativeId, String userId) {
    return walletRepository
        .findByInitiativeIdAndUserId(initiativeId, userId)
        .orElseThrow(
            () ->
                new WalletException(
                    HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND));
  }

  private void setStatus(Wallet wallet) {
    boolean hasIban = wallet.getIban() != null;
    boolean hasInstrument = wallet.getNInstr() > 0;
    String status = WalletStatus.getByBooleans(hasIban, hasInstrument).name();
    wallet.setStatus(status);
  }

  @Override
  public void deleteOperation(IbanQueueWalletDTO iban) {
    if (!iban.getStatus().equals("KO")) {
      log.info("[CHECK_IBAN_OUTCOME] Skipping outcome with status {}.", iban.getStatus());
      return;
    }

    walletRepository
        .findByInitiativeIdAndUserId(iban.getInitiativeId(), iban.getUserId())
        .ifPresent(
            wallet -> {
              if (!wallet.getIban().equals(iban.getIban())) {
                log.warn(
                    "[CHECK_IBAN_OUTCOME] The IBAN contained in the message is different from the IBAN currently enrolled.");
                return;
              }

              wallet.setIban(null);
              setStatus(wallet);

              walletRepository.save(wallet);
              sendCheckIban(iban);
            });
  }

  private void sendCheckIban(IbanQueueWalletDTO iban) {
    NotificationQueueDTO notificationQueueDTO =
        NotificationQueueDTO.builder()
            .operationType("CHECKIBAN")
            .userId(iban.getUserId())
            .initiativeId(iban.getInitiativeId())
            .iban(iban.getIban())
            .status(WalletConstants.STATUS_KO)
            .build();

    sendNotification(notificationQueueDTO);
  }

  private void sendNotification(NotificationQueueDTO notificationQueueDTO) {
    try {
      log.info("[SEND_NOTIFICATION] Sending event to Notification");
      notificationProducer.sendCheckIban(notificationQueueDTO);
    } catch (Exception e) {
      log.error("[SEND_NOTIFICATION] An error has occurred. Sending message to Error queue");
      final MessageBuilder<?> errorMessage = MessageBuilder.withPayload(notificationQueueDTO);
      this.sendToQueueError(e, errorMessage, notificationServer, notificationTopic);
    }
  }

  private void formalControl(String iban) {
    Iban ibanValidator = Iban.valueOf(iban);
    IbanUtil.validate(iban);
    if (!ibanValidator.getCountryCode().equals(CountryCode.IT)) {
      throw new UnsupportedCountryException(iban + " Iban is not italian");
    }
  }

  private void rollbackWallet(String oldStatus, Wallet wallet) {
    log.info("[ROLLBACK_WALLET] Wallet, old status: {}", oldStatus);
    wallet.setStatus(oldStatus);
    wallet.setRequestUnsubscribeDate(null);
    walletRepository.save(wallet);
    log.info("[ROLLBACK_WALLET] Rollback wallet, new status: {}", wallet.getStatus());
  }

  private void getInitiative(String initiativeId) {
    try {
      InitiativeDTO initiativeDTO =
          initiativeRestConnector.getInitiativeBeneficiaryView(initiativeId);
      if (!initiativeDTO.getStatus().equals("PUBLISHED")) {
        throw new WalletException(
            HttpStatus.FORBIDDEN.value(), WalletConstants.ERROR_INITIATIVE_KO);
      }
    } catch (FeignException e) {
      throw new WalletException(e.status(), e.contentUTF8());
    }
  }

  private void sendToTimeline(QueueOperationDTO queueOperationDTO) {
    try {
      log.info("[SEND_TO_TIMELINE] Sending queue message to Timeline");
      timelineProducer.sendEvent(queueOperationDTO);
    } catch (Exception exception) {
      log.error("[SEND_TO_TIMELINE] An error has occurred. Sending message to Error queue");
      final MessageBuilder<?> errorMessage = MessageBuilder.withPayload(queueOperationDTO);
      this.sendToQueueError(exception, errorMessage, timelineServer, timelineTopic);
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
}
