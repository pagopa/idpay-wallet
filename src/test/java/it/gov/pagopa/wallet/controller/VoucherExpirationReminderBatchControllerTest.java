package it.gov.pagopa.wallet.controller;

import it.gov.pagopa.wallet.config.ServiceExceptionConfig;
import it.gov.pagopa.wallet.config.WalletErrorManagerConfig;
import it.gov.pagopa.wallet.service.VoucherExpirationReminderBatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(
        value = {VoucherExpirationReminderBatchController.class},
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import({ServiceExceptionConfig.class, WalletErrorManagerConfig.class})

class VoucherExpirationReminderBatchControllerTest {

    private static final String BASE_URL = "http://localhost:8080/idpay/wallet";
    private static final String REMINDER_BATCH_URL = "/batch/run";
    private static final String INITIATIVE_ID = "TEST_INITIATIVE_ID";
    private static final int DAYS_NUMBER = 3;



    @MockBean
    VoucherExpirationReminderBatchService voucherExpirationReminderBatchServiceMock;

    @Autowired
    protected MockMvc mvc;

    @Test
    void runReminderBatch_ok() throws Exception {

        Mockito.doNothing().when(voucherExpirationReminderBatchServiceMock).runReminderBatch(INITIATIVE_ID, DAYS_NUMBER);

        mvc.perform(
                        MockMvcRequestBuilders.post(
                                                BASE_URL
                                                + "/"
                                                + REMINDER_BATCH_URL
                                                + "/"
                                                + INITIATIVE_ID
                                                )
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
    }

}