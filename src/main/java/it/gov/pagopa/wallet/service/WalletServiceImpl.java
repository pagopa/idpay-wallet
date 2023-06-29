package it.gov.pagopa.wallet.service;

import feign.FeignException;
import it.gov.pagopa.wallet.connector.OnboardingRestConnector;
import it.gov.pagopa.wallet.connector.PaymentInstrumentRestConnector;
import it.gov.pagopa.wallet.constants.WalletConstants;
import it.gov.pagopa.wallet.dto.*;
import it.gov.pagopa.wallet.dto.mapper.TimelineMapper;
import it.gov.pagopa.wallet.dto.mapper.WalletMapper;
import it.gov.pagopa.wallet.enums.BeneficiaryType;
import it.gov.pagopa.wallet.enums.WalletStatus;
import it.gov.pagopa.wallet.event.producer.ErrorProducer;
import it.gov.pagopa.wallet.event.producer.IbanProducer;
import it.gov.pagopa.wallet.event.producer.NotificationProducer;
import it.gov.pagopa.wallet.event.producer.TimelineProducer;
import it.gov.pagopa.wallet.exception.WalletException;
import it.gov.pagopa.wallet.exception.WalletUpdateException;
import it.gov.pagopa.wallet.model.Wallet;
import it.gov.pagopa.wallet.model.Wallet.RefundHistory;
import it.gov.pagopa.wallet.repository.WalletRepository;
import it.gov.pagopa.wallet.repository.WalletUpdatesRepository;
import it.gov.pagopa.wallet.utils.AuditUtilities;
import it.gov.pagopa.wallet.utils.Utilities;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WalletServiceImpl implements WalletService {

  public static final String SERVICE_ENROLL_IBAN = "ENROLL_IBAN";
  public static final String SERVICE_UNSUBSCRIBE = "UNSUBSCRIBE";
  public static final String SERVICE_CHECK_IBAN_OUTCOME = "CHECK_IBAN_OUTCOME";
  public static final String SERVICE_ENROLL_INSTRUMENT_ISSUER = "ENROLL_INSTRUMENT_ISSUER";
  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100L);
  public static final String SERVICE_PROCESS_REFUND = "PROCESS_REFUND";
  public static final String SERVICE_PROCESS_TRANSACTION = "PROCESS_TRANSACTION";

  @Autowired WalletRepository walletRepository;
  @Autowired WalletUpdatesRepository walletUpdatesRepository;
  @Autowired PaymentInstrumentRestConnector paymentInstrumentRestConnector;
  @Autowired OnboardingRestConnector onboardingRestConnector;
  @Autowired IbanProducer ibanProducer;
  @Autowired TimelineProducer timelineProducer;
  @Autowired WalletMapper walletMapper;
  @Autowired TimelineMapper timelineMapper;
  @Autowired ErrorProducer errorProducer;
  @Autowired NotificationProducer notificationProducer;
  @Autowired AuditUtilities auditUtilities;
  @Autowired Utilities utilities;

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

  @Value(
      "${spring.cloud.stream.binders.kafka-re.environment.spring.cloud.stream.kafka.binder.brokers}")
  String transactionServer;

  @Value("${spring.cloud.stream.bindings.consumerRefund-in-0.destination}")
  String transactionTopic;

  @Override
  public EnrollmentStatusDTO getEnrollmentStatus(String initiativeId, String userId) {
    long startTime = System.currentTimeMillis();
    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);
    performanceLog(startTime, "GET_ENROLLMENT_STATUS");
    return new EnrollmentStatusDTO(wallet.getStatus());
  }

  @Override
  public WalletDTO getWalletDetail(String initiativeId, String userId) {
    long startTime = System.currentTimeMillis();
    Wallet wallet = getWallet(initiativeId, userId);
    performanceLog(startTime, "GET_WALLET_DETAIL");
    return walletMapper.toInitiativeDTO(wallet);
  }

  @Override
  public WalletDTO getWalletDetailIssuer(String initiativeId, String userId) {
    long startTime = System.currentTimeMillis();
    Wallet wallet = getWallet(initiativeId, userId);
    performanceLog(startTime, "GET_WALLET_DETAIL_ISSUER");
    return walletMapper.toIssuerInitiativeDTO(wallet);
  }

  private Wallet getWallet(String initiativeId, String userId) {
    return walletRepository
        .findByInitiativeIdAndUserId(initiativeId, userId)
        .orElseThrow(
            () ->
                new WalletException(
                    HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND));
  }

  @Override
  public void enrollInstrument(String initiativeId, String userId, String idWallet) {
    long startTime = System.currentTimeMillis();

    log.info("[ENROLL_INSTRUMENT] Checking the status of initiative {}", initiativeId);
    auditUtilities.logEnrollmentInstrument(userId, initiativeId, idWallet);

    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);

    if (WalletConstants.INITIATIVE_REWARD_TYPE_DISCOUNT.equals(wallet.getInitiativeRewardType())) {
      auditUtilities.logEnrollmentInstrumentKO(
          userId, initiativeId, idWallet, "the initiative is discount type");
      throw new WalletException(
          HttpStatus.FORBIDDEN.value(), WalletConstants.ERROR_INITIATIVE_DISCOUNT_PI);
    }

    checkEndDate(wallet.getEndDate());

    if (wallet.getStatus().equals(WalletStatus.UNSUBSCRIBED)) {
      auditUtilities.logEnrollmentInstrumentKO(
          userId, initiativeId, idWallet, "wallet in status unsubscribed");
      throw new WalletException(
          HttpStatus.BAD_REQUEST.value(), WalletConstants.ERROR_INITIATIVE_UNSUBSCRIBED);
    }
    InstrumentCallBodyDTO dto =
        new InstrumentCallBodyDTO(userId, initiativeId, idWallet, WalletConstants.CHANNEL_APP_IO);

    try {
      log.info("[ENROLL_INSTRUMENT] Calling Payment Instrument");
      paymentInstrumentRestConnector.enrollInstrument(dto);
      performanceLog(startTime, "ENROLL_INSTRUMENT");
    } catch (FeignException e) {
      log.error("[ENROLL_INSTRUMENT] Error in Payment Instrument Request");
      auditUtilities.logEnrollmentInstrumentKO(
          userId, initiativeId, idWallet, "error in payment instrument request");
      performanceLog(startTime, "ENROLL_INSTRUMENT");
      throw new WalletException(e.status(), utilities.exceptionConverter(e));
    }
  }

  @Override
  public void deleteInstrument(String initiativeId, String userId, String instrumentId) {
    long startTime = System.currentTimeMillis();

    log.info("[DELETE_INSTRUMENT] Checking the status of initiative {}", initiativeId);
    auditUtilities.logInstrumentDeleted(userId, initiativeId);

    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);

    checkEndDate(wallet.getEndDate());

    DeactivationBodyDTO dto = new DeactivationBodyDTO(userId, initiativeId, instrumentId);

    try {
      paymentInstrumentRestConnector.deleteInstrument(dto);
      performanceLog(startTime, "DELETE_INSTRUMENT");
    } catch (FeignException e) {
      performanceLog(startTime, "DELETE_INSTRUMENT");
      throw new WalletException(e.status(), e.getMessage());
    }
  }

  @Override
  public void enrollIban(
      String initiativeId, String userId, String iban, String channel, String description) {
    long startTime = System.currentTimeMillis();

    log.info("[ENROLL_IBAN] Checking the status of initiative {}", initiativeId);
    auditUtilities.logEnrollmentIban(userId, initiativeId, channel);

    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);

    if (WalletConstants.INITIATIVE_REWARD_TYPE_DISCOUNT.equals(wallet.getInitiativeRewardType())) {
      auditUtilities.logEnrollmentIbanKO(
          "the initiative is discount type", userId, initiativeId, channel);
      throw new WalletException(
          HttpStatus.FORBIDDEN.value(), WalletConstants.ERROR_INITIATIVE_DISCOUNT_IBAN);
    }

    checkEndDate(wallet.getEndDate());
    if (wallet.getStatus().equals(WalletStatus.UNSUBSCRIBED)) {
      performanceLog(startTime, SERVICE_ENROLL_IBAN);
      auditUtilities.logEnrollmentIbanKO(
          "wallet in status unsubscribed", userId, initiativeId, channel);
      throw new WalletException(
          HttpStatus.BAD_REQUEST.value(), WalletConstants.ERROR_INITIATIVE_UNSUBSCRIBED);
    }

    if (wallet.getIban() != null && (wallet.getIban().equals(iban))) {
      log.info("[ENROLL_IBAN] The IBAN matches with the one already enrolled");
      performanceLog(startTime, SERVICE_ENROLL_IBAN);
      auditUtilities.logEnrollmentIbanKO(
          "the IBAN matches with the one already enrolled", userId, initiativeId, channel);
      return;
    }

    iban = iban.toUpperCase();
    formalControl(iban);
    wallet.setIban(iban);
    IbanQueueDTO ibanQueueDTO =
        new IbanQueueDTO(userId, initiativeId, iban, description, channel, LocalDateTime.now());

    walletUpdatesRepository.enrollIban(initiativeId, userId, iban, setStatus(wallet));
    auditUtilities.logEnrollmentIbanComplete(userId, initiativeId, iban);

    try {
      log.info("[ENROLL_IBAN] Sending event to IBAN");
      ibanProducer.sendIban(ibanQueueDTO);
    } catch (Exception e) {
      log.error("[ENROLL_IBAN] An error has occurred. Sending message to Error queue", e);
      auditUtilities.logEnrollmentIbanKO(
          "error in sending request to checkIban", userId, initiativeId, channel);
      final MessageBuilder<?> errorMessage = MessageBuilder.withPayload(ibanQueueDTO);
      this.sendToQueueError(e, errorMessage, ibanServer, ibanTopic);
      performanceLog(startTime, SERVICE_ENROLL_IBAN);
    }

    performanceLog(startTime, SERVICE_ENROLL_IBAN);
  }

  @Override
  public void suspendWallet(String initiativeId, String userId) {
    long startTime = System.currentTimeMillis();

    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);
    if (!wallet.getStatus().equals(WalletStatus.SUSPENDED)) {
      if (wallet.getStatus().equals(WalletStatus.UNSUBSCRIBED)) {
        auditUtilities.logSuspensionKO(userId, initiativeId);
        throw new WalletException(
            HttpStatus.BAD_REQUEST.value(), WalletConstants.ERROR_INITIATIVE_UNSUBSCRIBED);
      }

      LocalDateTime localDateTime = LocalDateTime.now();
      String backupStatus = wallet.getStatus();
      try {
        walletUpdatesRepository.suspendWallet(
            initiativeId, userId, WalletStatus.SUSPENDED, localDateTime);
        log.info("[SUSPEND_USER] Sending event to ONBOARDING");
        onboardingRestConnector.suspendOnboarding(initiativeId, userId);
      } catch (Exception e) {
        auditUtilities.logSuspensionKO(userId, initiativeId);
        this.rollbackWallet(backupStatus, wallet);
        performanceLog(startTime, WalletConstants.SUSPENSION);
        throw new WalletException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
      }
      sendToTimeline(timelineMapper.suspendToTimeline(initiativeId, userId, localDateTime));
      sendSuspensionReadmissionNotification(
          WalletConstants.SUSPENSION, initiativeId, userId, wallet.getInitiativeName());

      log.info("[SUSPENSION] Wallet is suspended from the initiative {}", initiativeId);
      auditUtilities.logSuspension(userId, initiativeId);
    }
    performanceLog(startTime, WalletConstants.SUSPENSION);
  }

  @Override
  public void readmitWallet(String initiativeId, String userId) {
    long startTime = System.currentTimeMillis();

    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);
    if (wallet.getStatus().equals(WalletStatus.UNSUBSCRIBED)) {
      auditUtilities.logReadmissionKO(userId, initiativeId);
      log.info(
          "[READMISSION] Wallet readmission to the initiative {} is not possible", initiativeId);
      throw new WalletException(
          HttpStatus.BAD_REQUEST.value(), WalletConstants.ERROR_INITIATIVE_UNSUBSCRIBED);
    }

    LocalDateTime localDateTime = LocalDateTime.now();
    String backupStatus = wallet.getStatus();
    LocalDateTime backupSuspensionDate = wallet.getSuspensionDate();
    String readmittedStatus = WalletStatus.getByBooleans(wallet.getIban() != null, wallet.getNInstr() > 0).name();
    try {
      walletUpdatesRepository.readmitWallet(initiativeId, userId, readmittedStatus, localDateTime);
      log.info("[READMIT_USER] Sending event to ONBOARDING");
      onboardingRestConnector.readmitOnboarding(initiativeId, userId);
    } catch (Exception e) {
      auditUtilities.logReadmissionKO(userId, initiativeId);
      log.info("[READMISSION] Wallet readmission to the initiative {} is failed", initiativeId);
      wallet.setStatus(backupStatus);
      wallet.setSuspensionDate(backupSuspensionDate);
      wallet.setUpdateDate(localDateTime);
      walletRepository.save(wallet);
      performanceLog(startTime, WalletConstants.READMISSION);
      throw new WalletException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
    }
    sendToTimeline(timelineMapper.readmitToTimeline(initiativeId, userId, localDateTime));
    sendSuspensionReadmissionNotification(
        WalletConstants.READMISSION, initiativeId, userId, wallet.getInitiativeName());

    log.info("[READMISSION] Wallet is readmitted to the initiative {}", initiativeId);
    auditUtilities.logReadmission(userId, initiativeId);
    performanceLog(startTime, WalletConstants.READMISSION);
  }

  @Override
  public InitiativeListDTO getInitiativeList(String userId) {
    long startTime = System.currentTimeMillis();
    List<Wallet> walletList = walletRepository.findByUserId(userId);
    walletList.sort(Comparator.comparing(Wallet::getAcceptanceDate).reversed());
    InitiativeListDTO initiativeListDTO = new InitiativeListDTO();
    List<WalletDTO> walletDTOList = new ArrayList<>();

    for (Wallet wallet : walletList) {
      walletDTOList.add(walletMapper.toInitiativeDTO(wallet));
    }
    initiativeListDTO.setInitiativeList(walletDTOList);

    performanceLog(startTime, "GET_INITIATIVE_LIST");
    return initiativeListDTO;
  }

  @Override
  public void createWallet(EvaluationDTO evaluationDTO) {
    log.info(
        "[CREATE_WALLET] EvaluationDTO received. userId: {}; initiativeId: {}; status: {}",
        evaluationDTO.getUserId(),
        evaluationDTO.getInitiativeId(),
        evaluationDTO.getStatus());

    long startTime = System.currentTimeMillis();

    if (WalletConstants.STATUS_ONBOARDING_OK.equals(evaluationDTO.getStatus())
        || WalletConstants.STATUS_JOINED.equals(evaluationDTO.getStatus())) {
      Wallet wallet = walletMapper.map(evaluationDTO);

      if (evaluationDTO.getFamilyId() != null) {
        List<Wallet> familyWallets =
            walletRepository.findByInitiativeIdAndFamilyId(
                evaluationDTO.getInitiativeId(), evaluationDTO.getFamilyId());
        if (!familyWallets.isEmpty()) {
          wallet.setAmount(familyWallets.get(0).getAmount());
        }
      }

      if (WalletConstants.INITIATIVE_REWARD_TYPE_DISCOUNT.equals(
          evaluationDTO.getInitiativeRewardType())) {
        wallet.setStatus(WalletStatus.REFUNDABLE.name());
        paymentInstrumentRestConnector.enrollDiscountInitiative(
            InstrumentFromDiscountDTO.builder()
                .initiativeId(evaluationDTO.getInitiativeId())
                .userId(evaluationDTO.getUserId())
                .build());
      }

      walletRepository.save(wallet);
      sendToTimeline(timelineMapper.onboardingToTimeline(evaluationDTO));
    }

    performanceLog(startTime, "CREATE_WALLET");
    auditUtilities.logCreatedWallet(evaluationDTO.getUserId(), evaluationDTO.getInitiativeId());
  }
  @Override
  public void unsubscribe(String initiativeId, String userId) {
    long startTime = System.currentTimeMillis();

    log.info("[UNSUBSCRIBE] Unsubscribing user {} on initiative {}",userId,initiativeId);
    auditUtilities.logUnsubscribe(userId, initiativeId);
    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);
    LocalDateTime localDateTime = LocalDateTime.now();
    String statusTemp = wallet.getStatus();
    wallet.setRequestUnsubscribeDate(LocalDateTime.now());
    UnsubscribeCallDTO unsubscribeCallDTO =
            new UnsubscribeCallDTO(
                    initiativeId, userId, wallet.getRequestUnsubscribeDate().toString());
    try {
      paymentInstrumentRestConnector.disableAllInstrument(unsubscribeCallDTO);
      log.info("[UNSUBSCRIBE] Payment instruments disabled on initiative {} for user {}",initiativeId,userId);
    } catch (FeignException e) {
      performanceLog(startTime, SERVICE_UNSUBSCRIBE);
      auditUtilities.logUnsubscribeKO(
              userId, initiativeId, "request of disabling all payment instruments failed");
      throw new WalletException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
    }
    try {
      onboardingRestConnector.disableOnboarding(unsubscribeCallDTO);
      log.info("[UNSUBSCRIBE] Onboarding disabled on initiative {} for user {}",initiativeId,userId);
    } catch (FeignException e) {
      paymentInstrumentRestConnector.rollback(initiativeId, userId);
      performanceLog(startTime, SERVICE_UNSUBSCRIBE);
      auditUtilities.logUnsubscribeKO(
              userId, initiativeId, "request of disabling onboarding failed");
      throw new WalletException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
    }
    try {
      if (!wallet.getStatus().equals(WalletStatus.UNSUBSCRIBED)) {
        wallet.setStatus(WalletStatus.UNSUBSCRIBED);
        walletRepository.save(wallet);
        log.info("[UNSUBSCRIBE] Wallet disabled on initiative {} for user {}",initiativeId,userId);
        sendToTimeline(timelineMapper.unsubscribeToTimeline(initiativeId, userId, localDateTime));
      }
      performanceLog(startTime, SERVICE_UNSUBSCRIBE);
    } catch (FeignException e) {
      this.rollbackWallet(statusTemp, wallet);
      onboardingRestConnector.rollback(initiativeId, userId);
      paymentInstrumentRestConnector.rollback(initiativeId, userId);
      performanceLog(startTime, SERVICE_UNSUBSCRIBE);
      auditUtilities.logUnsubscribeKO(
              userId, initiativeId, "request of disabling wallet failed");
      throw new WalletException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
    }
  }

  @Override
  public void processTransaction(RewardTransactionDTO rewardTransactionDTO) {
    long startTime = System.currentTimeMillis();

    if (!rewardTransactionDTO.getStatus().equals("REWARDED")
        && !(rewardTransactionDTO.getChannel().equals("QRCODE")
            && (rewardTransactionDTO.getStatus().equals("AUTHORIZED")
            || rewardTransactionDTO.getStatus().equals("CANCELLED")))) {
      log.info("[PROCESS_TRANSACTION] Transaction not in status REWARDED, skipping message");
      performanceLog(startTime, SERVICE_PROCESS_TRANSACTION);
      return;
    }
    log.info("[PROCESS_TRANSACTION] New trx from Rule Engine");
    rewardTransactionDTO
        .getRewards()
        .forEach(
            (initiativeId, reward) -> {
              log.info("[PROCESS_TRANSACTION] Processing initiative: {}", initiativeId);
              try {
                updateWalletFromTransaction(
                    initiativeId,
                    rewardTransactionDTO,
                    reward.getCounters(),
                    reward.getAccruedReward());
              } catch (WalletUpdateException e) {
                log.error(
                    "[PROCESS_TRANSACTION] An error has occurred. Sending message to Error queue",
                    e);
                final MessageBuilder<?> errorMessage =
                    MessageBuilder.withPayload(rewardTransactionDTO);
                this.sendToQueueError(e, errorMessage, transactionServer, transactionTopic);
                performanceLog(startTime, SERVICE_PROCESS_TRANSACTION);
              }
            });
    performanceLog(startTime, SERVICE_PROCESS_TRANSACTION);
  }

  @Override
  public void updateWallet(WalletPIBodyDTO walletPIDTO) {
    long startTime = System.currentTimeMillis();

    log.info("[UPDATE_WALLET] New revoke from PM");
    for (WalletPIDTO walletPI : walletPIDTO.getWalletDTOlist()) {
      Wallet wallet =
          walletRepository
              .findByInitiativeIdAndUserId(walletPI.getInitiativeId(), walletPI.getUserId())
              .orElse(null);
      if (wallet == null) {
        log.info(
            "[UPDATE_WALLET] Wallet with initiativeId {} not found", walletPI.getInitiativeId());
        continue;
      }
      wallet.setNInstr(wallet.getNInstr() - 1);
      walletUpdatesRepository.decreaseInstrumentNumber(
          walletPI.getInitiativeId(), walletPI.getUserId(), setStatus(wallet));
      QueueOperationDTO queueOperationDTO =
          timelineMapper.deleteInstrumentToTimeline(
              wallet.getInitiativeId(),
              wallet.getUserId(),
              walletPI.getMaskedPan(),
              walletPI.getBrandLogo(),
              walletPI.getBrand());

      sendToTimeline(queueOperationDTO);
    }
    performanceLog(startTime, "UPDATE_WALLET");
  }

  @Override
  public void processAck(InstrumentAckDTO instrumentAckDTO) {

    long startTime = System.currentTimeMillis();

    log.info(
        "[PROCESS_ACK] Processing new ack {} from PaymentInstrument",
        instrumentAckDTO.getOperationType());

    Wallet wallet =
        walletRepository
            .findByInitiativeIdAndUserId(
                instrumentAckDTO.getInitiativeId(), instrumentAckDTO.getUserId())
            .orElse(null);

    if (wallet == null) {
      log.error("[PROCESS_ACK] Wallet not found");
      performanceLog(startTime, "PROCESS_ACK");
      throw new WalletException(
          HttpStatus.NOT_FOUND.value(), WalletConstants.ERROR_WALLET_NOT_FOUND);
    }

    if (!instrumentAckDTO.getOperationType().equals(WalletConstants.REJECTED_ADD_INSTRUMENT)) {
      wallet.setNInstr(instrumentAckDTO.getNinstr());
      walletUpdatesRepository.updateInstrumentNumber(
          instrumentAckDTO.getInitiativeId(),
          instrumentAckDTO.getUserId(),
          instrumentAckDTO.getNinstr(),
          setStatus(wallet));
    }

    QueueOperationDTO queueOperationDTO = timelineMapper.ackToTimeline(instrumentAckDTO);

    sendToTimeline(queueOperationDTO);
    performanceLog(startTime, "PROCESS_ACK");
  }

  @Override
  public void processRefund(RefundDTO refundDTO) {
    long startTime = System.currentTimeMillis();

    if (BeneficiaryType.MERCHANT.equals(refundDTO.getBeneficiaryType())) {
      log.info("[PROCESS_REFUND] Beneficiary type is a merchant, skipping message");
      performanceLog(startTime, SERVICE_PROCESS_REFUND);
      return;
    }

    log.info("[PROCESS_REFUND] Processing new refund");

    Wallet wallet =
        walletRepository
            .findByInitiativeIdAndUserId(refundDTO.getInitiativeId(), refundDTO.getBeneficiaryId())
            .orElse(null);

    if (wallet == null) {
      log.info("[PROCESS_REFUND] Wallet not found, skipping message");
      performanceLog(startTime, SERVICE_PROCESS_REFUND);
      return;
    }

    Map<String, RefundHistory> history = wallet.getRefundHistory();

    if (history == null) {
      history = new HashMap<>();
    }

    if (history.containsKey(refundDTO.getRewardNotificationId())
        && history.get(refundDTO.getRewardNotificationId()).getFeedbackProgressive()
            >= refundDTO.getFeedbackProgressive()) {
      log.info("[PROCESS_REFUND] Feedback already processed, skipping message");
      performanceLog(startTime, SERVICE_PROCESS_REFUND);
      return;
    }

    BigDecimal refunded =
        BigDecimal.valueOf(refundDTO.getRewardCents())
            .divide(ONE_HUNDRED, 2, RoundingMode.HALF_DOWN);

    history.put(
        refundDTO.getRewardNotificationId(), new RefundHistory(refundDTO.getFeedbackProgressive()));

    walletUpdatesRepository.processRefund(
        refundDTO.getInitiativeId(),
        refundDTO.getBeneficiaryId(),
        wallet.getRefunded().add(refunded),
        history);

    QueueOperationDTO queueOperationDTO = timelineMapper.refundToTimeline(refundDTO);

    sendToTimeline(queueOperationDTO);
    sendRefundNotification(refundDTO);

    performanceLog(startTime, SERVICE_PROCESS_REFUND);
  }

  @Override
  public void enrollInstrumentIssuer(String initiativeId, String userId, InstrumentIssuerDTO body) {
    long startTime = System.currentTimeMillis();

    log.info("[ENROLL_INSTRUMENT_ISSUER] Checking the status of initiative {}", initiativeId);
    auditUtilities.logEnrollmentInstrumentIssuer(userId, initiativeId, body.getChannel());

    Wallet wallet = findByInitiativeIdAndUserId(initiativeId, userId);

    if (WalletConstants.INITIATIVE_REWARD_TYPE_DISCOUNT.equals(wallet.getInitiativeRewardType())) {
      throw new WalletException(
          HttpStatus.FORBIDDEN.value(), WalletConstants.ERROR_INITIATIVE_DISCOUNT_PI);
    }

    checkEndDate(wallet.getEndDate());

    if (wallet.getStatus().equals(WalletStatus.UNSUBSCRIBED)) {
      performanceLog(startTime, SERVICE_ENROLL_INSTRUMENT_ISSUER);
      throw new WalletException(
          HttpStatus.BAD_REQUEST.value(), WalletConstants.ERROR_INITIATIVE_UNSUBSCRIBED);
    }

    InstrumentIssuerCallDTO instrumentIssuerCallDTO =
        InstrumentIssuerCallDTO.builder()
            .initiativeId(initiativeId)
            .userId(userId)
            .hpan(body.getHpan())
            .channel(body.getChannel())
            .brandLogo(body.getBrandLogo())
            .brand(body.getBrand())
            .maskedPan(body.getMaskedPan())
            .build();

    try {
      log.info("[ENROLL_INSTRUMENT_ISSUER] Calling Payment Instrument");
      paymentInstrumentRestConnector.enrollInstrumentIssuer(instrumentIssuerCallDTO);
      performanceLog(startTime, SERVICE_ENROLL_INSTRUMENT_ISSUER);
    } catch (FeignException e) {
      log.error("[ENROLL_INSTRUMENT_ISSUER] Error in Payment Instrument Request");
      performanceLog(startTime, SERVICE_ENROLL_INSTRUMENT_ISSUER);
      throw new WalletException(e.status(), utilities.exceptionConverter(e));
    }
  }

  private void updateWalletFromTransaction(
      String initiativeId,
      RewardTransactionDTO rewardTransactionDTO,
      Counters counters,
      BigDecimal accruedReward) {

    if (!(rewardTransactionDTO.getChannel().equals("QRCODE")
            && rewardTransactionDTO.getStatus().equals("REWARDED"))) {

      Wallet userWallet;
      if ((userWallet =
              walletUpdatesRepository.rewardTransaction(
                      initiativeId,
                      rewardTransactionDTO.getUserId(),
                      rewardTransactionDTO.getElaborationDateTime(),
                      counters
                              .getInitiativeBudget()
                              .subtract(counters.getTotalReward())
                              .setScale(2, RoundingMode.HALF_DOWN),
                      counters.getTotalReward(),
                      counters.getTrxNumber()))
              == null) {
        log.info("[UPDATE_WALLET_FROM_TRANSACTION] No wallet found for this initiativeId");
        return;
      }

      if (userWallet.getFamilyId() != null) {
        BigDecimal familyTotalReward =
                walletUpdatesRepository.getFamilyTotalReward(initiativeId, userWallet.getFamilyId());
        log.info(
                "[UPDATE_WALLET_FROM_TRANSACTION][FAMILY_WALLET] Family {} total reward: {}",
                userWallet.getFamilyId(),
                familyTotalReward);

        boolean updateResult =
                walletUpdatesRepository.rewardFamilyTransaction(
                        initiativeId,
                        userWallet.getFamilyId(),
                        rewardTransactionDTO.getElaborationDateTime(),
                        counters
                                .getInitiativeBudget()
                                .subtract(familyTotalReward)
                                .setScale(2, RoundingMode.HALF_DOWN));

        if (!updateResult) {
          throw new WalletUpdateException(
                  "[UPDATE_WALLET_FROM_TRANSACTION][FAMILY_WALLET] Something went wrong updating wallet(s) of family having id: %s"
                          .formatted(userWallet.getFamilyId()));
        }
      }
    }

    log.info("[UPDATE_WALLET_FROM_TRANSACTION] Sending transaction to Timeline");
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

  private String setStatus(Wallet wallet) {
    if(!wallet.getStatus().equals(WalletStatus.SUSPENDED)){
      boolean hasIban = wallet.getIban() != null;
      boolean hasInstrument = wallet.getNInstr() > 0;
      return WalletStatus.getByBooleans(hasIban, hasInstrument).name();
    }
    return WalletStatus.SUSPENDED;
  }

  @Override
  public void processIbanOutcome(IbanQueueWalletDTO iban) {
    long startTime = System.currentTimeMillis();

    if (!iban.getStatus().equals(WalletConstants.STATUS_KO)) {
      log.info("[CHECK_IBAN_OUTCOME] Skipping outcome with status {}.", iban.getStatus());
      sendToTimeline(
          timelineMapper.ibanToTimeline(
              iban.getInitiativeId(), iban.getUserId(), iban.getIban(), iban.getChannel()));
      performanceLog(startTime, SERVICE_CHECK_IBAN_OUTCOME);
      return;
    }

    Wallet wallet =
        walletRepository
            .findByInitiativeIdAndUserId(iban.getInitiativeId(), iban.getUserId())
            .orElse(null);

    if (wallet == null) {
      log.warn("[CHECK_IBAN_OUTCOME] Wallet not found. Skipping message");
      performanceLog(startTime, SERVICE_CHECK_IBAN_OUTCOME);
      auditUtilities.logIbanDeletedKO(
          iban.getUserId(), iban.getInitiativeId(), iban.getIban(), "wallet not found");
      return;
    }

    if (!iban.getIban().equals(wallet.getIban())) {
      log.warn(
          "[CHECK_IBAN_OUTCOME] The IBAN contained in the message is different from the IBAN currently enrolled.");
      performanceLog(startTime, SERVICE_CHECK_IBAN_OUTCOME);
      auditUtilities.logIbanDeletedKO(
          iban.getUserId(), iban.getInitiativeId(), iban.getIban(), "iban mismatch");
      return;
    }

    wallet.setIban(null);

    walletUpdatesRepository.deleteIban(iban.getInitiativeId(), iban.getUserId(), setStatus(wallet));
    sendCheckIban(iban);
    auditUtilities.logIbanDeleted(iban.getUserId(), iban.getInitiativeId(), iban.getIban());

    performanceLog(startTime, SERVICE_CHECK_IBAN_OUTCOME);
  }

  private void sendCheckIban(IbanQueueWalletDTO iban) {
    NotificationQueueDTO notificationQueueDTO =
        NotificationQueueDTO.builder()
            .operationType(WalletConstants.CHECKIBAN_KO)
            .userId(iban.getUserId())
            .initiativeId(iban.getInitiativeId())
            .iban(iban.getIban())
            .status(WalletConstants.STATUS_KO)
            .build();

    sendNotification(notificationQueueDTO);
  }

  private void sendRefundNotification(RefundDTO refundDTO) {
    NotificationQueueDTO notificationQueueDTO =
        NotificationQueueDTO.builder()
            .operationType(WalletConstants.REFUND)
            .userId(refundDTO.getBeneficiaryId())
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

  private void sendSuspensionReadmissionNotification(
      String operationType, String initiativeId, String userId, String initiativeName) {
    NotificationQueueDTO notificationQueueDTO =
        NotificationQueueDTO.builder()
            .operationType(operationType)
            .userId(userId)
            .initiativeId(initiativeId)
            .initiativeName(initiativeName)
            .build();

    sendNotification(notificationQueueDTO);
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

  private void formalControl(String iban) {
    Iban ibanValidator = Iban.valueOf(iban);
    IbanUtil.validate(iban);
    if (!ibanValidator.getCountryCode().equals(CountryCode.IT)) {
      auditUtilities.logEnrollmentIbanValidationKO(iban);
      throw new UnsupportedCountryException(iban + " Iban is not italian");
    }
  }

  private void rollbackWallet(String statusToRollback, Wallet wallet) {
    log.info("[ROLLBACK_WALLET] Wallet, old status: {}", wallet.getStatus());
    if (WalletStatus.UNSUBSCRIBED.equals(wallet.getStatus())) {
      wallet.setRequestUnsubscribeDate(null);
    }
    if (WalletStatus.SUSPENDED.equals(wallet.getStatus())) {
      wallet.setSuspensionDate(null);
    }
    wallet.setStatus(statusToRollback);
    wallet.setUpdateDate(LocalDateTime.now());
    walletRepository.save(wallet);
    log.info("[ROLLBACK_WALLET] Rollback wallet, new status: {}", wallet.getStatus());
  }

  private void checkEndDate(LocalDate endDate) {
    try {
      LocalDate requestDate = LocalDate.now();

      if (requestDate.isAfter(endDate)) {
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

  private void performanceLog(long startTime, String service) {
    log.info(
        "[PERFORMANCE_LOG] [{}] Time occurred to perform business logic: {} ms",
        service,
        System.currentTimeMillis() - startTime);
  }

  @Override
  public InitiativesWithInstrumentDTO getInitiativesWithInstrument(String idWallet, String userId) {
    long startTime = System.currentTimeMillis();
    List<InitiativesStatusDTO> initiativesStatusDTO = new ArrayList<>();

    InitiativeListDTO initiativeListDTO = this.getInitiativeList(userId);

    log.info(
        "[GET_INSTRUMENT_DETAIL_ON_INITIATIVES] Get detail about payment instrument and its status on initiatives");
    try {
      log.info("[GET_INSTRUMENT_DETAIL_ON_INITIATIVES] Calling Payment Instrument");
      InstrumentDetailDTO instrumentDetailDTO =
          paymentInstrumentRestConnector.getInstrumentInitiativesDetail(
              idWallet, userId, WalletConstants.FILTER_INSTRUMENT_STATUS_LIST);

      if (initiativeListDTO.getInitiativeList().isEmpty()) {
        return walletMapper.toInstrumentOnInitiativesDTO(
            idWallet, instrumentDetailDTO, initiativesStatusDTO);
      }

      log.info("[GET_INSTRUMENT_DETAIL_ON_INITIATIVES] Get all initiatives still active for user");
      for (WalletDTO wallet : initiativeListDTO.getInitiativeList()) {
        if (!wallet.getStatus().equals(WalletStatus.UNSUBSCRIBED)
            && !wallet.getEndDate().isBefore(LocalDate.now())) {
          initiativesStatusDTO.add(walletMapper.toInstrStatusOnInitiativeDTO(wallet));
        }
      }

      if (!instrumentDetailDTO.getInitiativeList().isEmpty()) {
        List<StatusOnInitiativeDTO> instrStatusList = instrumentDetailDTO.getInitiativeList();

        Map<String, StatusOnInitiativeDTO> instrumentStatusOnInitiativeMap =
            instrStatusList.stream()
                .collect(
                    Collectors.toMap(StatusOnInitiativeDTO::getInitiativeId, Function.identity()));

        log.info(
            "[GET_INSTRUMENT_DETAIL_ON_INITIATIVES] Updating initiatives list with payment status");
        initiativesStatusDTO =
            initiativesStatusDTO.stream()
                .map(
                    i ->
                        new InitiativesStatusDTO(
                            i.getInitiativeId(),
                            i.getInitiativeName(),
                            (instrumentStatusOnInitiativeMap.get(i.getInitiativeId()) != null
                                ? instrumentStatusOnInitiativeMap
                                    .get(i.getInitiativeId())
                                    .getIdInstrument()
                                : null),
                            (instrumentStatusOnInitiativeMap.get(i.getInitiativeId()) != null
                                ? instrumentStatusOnInitiativeMap
                                    .get(i.getInitiativeId())
                                    .getStatus()
                                : WalletConstants.INSTRUMENT_STATUS_DEFAULT)))
                .toList();
      }

      performanceLog(startTime, "GET_INSTRUMENT_DETAIL_ON_INITIATIVES");
      return walletMapper.toInstrumentOnInitiativesDTO(
          idWallet, instrumentDetailDTO, initiativesStatusDTO);
    } catch (FeignException e) {
      log.error("[GET_INSTRUMENT_DETAIL_ON_INITIATIVES] Error in Payment Instrument Request");
      performanceLog(startTime, "GET_INSTRUMENT_DETAIL_ON_INITIATIVES");
      throw new WalletException(e.status(), e.getMessage());
    }
  }
}
