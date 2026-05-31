package com.wallet.transfer;

import com.wallet.transfer.dto.CreateWalletRequest;
import com.wallet.transfer.dto.TransferRequest;
import com.wallet.transfer.dto.WalletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TransferApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.0"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockmvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateWallet() throws Exception {
        CreateWalletRequest request = new CreateWalletRequest("Alice", new BigDecimal("1000.00"));

        mockmvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerName").value("Alice"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    private WalletResponse createWallet(String owner, String balance) throws Exception {
        CreateWalletRequest request = new CreateWalletRequest(owner, new BigDecimal(balance));
        MvcResult result = mockmvc.perform(post("/api/v1/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), WalletResponse.class);
    }

    @Test
    void shoudTransferMoney() throws Exception {
        WalletResponse source = createWallet("Bob", "500.00");
        WalletResponse target = createWallet("Charlie", "200.00");

        TransferRequest transfer = new TransferRequest(source.id(), target.id(), new BigDecimal("100.00"));

        mockmvc.perform(
                post("/api/v1/wallets/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-key", UUID.randomUUID().toString())
                        .content(objectMapper.writeValueAsString(transfer))
        ).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(100.00));


        mockmvc.perform(get("/api/v1/wallets/" + source.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(400.00));

        mockmvc.perform(get("api/v1/wallets/" + target.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(300.00));

    }

    @Test
    void shouldRejectDuplicateTransfer() throws Exception {

        WalletResponse source = createWallet("Dave", "500.00");
        WalletResponse target = createWallet("Eve", "200.00");


        TransferRequest transferRequest = new TransferRequest(source.id(), target.id(), new BigDecimal("50.00"));
        String idemopotencyKey = UUID.randomUUID().toString();

        mockmvc.perform(post("api/v1/wallets/transfers").contentType(MediaType.APPLICATION_JSON).header("Idempotency-Key", idemopotencyKey).content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isCreated());

        mockmvc.perform(post("/api/v1/wallets/transfers").contentType(MediaType.APPLICATION_JSON).header("Idempotency-key", idemopotencyKey).content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isConflict());

    }

    @Test
    void shouldRejectInsufficientBalance() throws Exception {

        WalletResponse source = createWallet("Frank", "50.00");
        WalletResponse target = createWallet("Grace", "200.00");

        TransferRequest transferRequest = new TransferRequest(source.id(), target.id(), new BigDecimal("100.00"));

        mockmvc.perform(post("/api/v1/wallets/transfers").contentType(MediaType.APPLICATION_JSON).header("Idempotency-Key", UUID.randomUUID().toString()).content(objectMapper.writeValueAsString(transferRequest))).andExpect(status().isUnprocessableEntity());

    }

    @Test
    void shouldReturn404ForNonExistentWallet() throws Exception {
        mockmvc.perform(get("/api/v1/wallets/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectSelfTransfer() throws Exception {

        WalletResponse wallet = createWallet("SelfSender", "500.00");

        TransferRequest transferRequest = new TransferRequest(wallet.id(), wallet.id(), new BigDecimal("50.00"));

        mockmvc.perform(post("/api/v1/wallets/transfers").contentType(MediaType.APPLICATION_JSON).header("Idempotency-key", UUID.randomUUID().toString()).content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isBadRequest());

    }

    @Test
    void shouldHandleConcurrentTransfersWithOptimisticLocking() throws Exception {

        WalletResponse source = createWallet("ConcurrentSender", "1000.00");
        WalletResponse target = createWallet("ConcurrentReceiver", "0.00");

        int threadCount = 10;
        BigDecimal transferAmount = new BigDecimal("100.00");
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++){

            executorService.submit(() -> {
                try {
                    TransferRequest transferRequest = new TransferRequest(source.id(), target.id(), transferAmount);

                    int status = mockmvc.perform(
                            post("/api/v1/wallets/transfers")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("Idempotency-Key", UUID.randomUUID().toString())
                                    .content(objectMapper.writeValueAsString(transferRequest))
                    ).andReturn().getResponse().getStatus();

                    if(status == 201) successCount.incrementAndGet();
                    if(status == 409) conflictCount.incrementAndGet();

                } catch(Exception e) {}
                finally {
                    latch.countDown();
                }
            });

        }

        latch.await();
        executorService.shutdown();

        assertThat(successCount.get() + conflictCount.get()).isEqualTo(threadCount);
        assertThat(conflictCount.get()).isGreaterThan(0);

        MvcResult sourceResult = mockmvc.perform(get("/api/v1/wallets/" + source.id()))
                .andExpect(status().isOk())
                .andReturn();

        WalletResponse updatedResponse = objectMapper.readValue(
                sourceResult.getResponse().getContentAsString(), WalletResponse.class
        );

        BigDecimal expectedDeducted = transferAmount.multiply(BigDecimal.valueOf(successCount.get()));
        assertThat(updatedResponse.balance()).isEqualByComparingTo(new BigDecimal("1000.00").subtract(expectedDeducted));

    }

    @Test
    void shouldPopulateLedgerEventually() throws Exception {

        WalletResponse source = createWallet("LedgerSender", "500.00");
        WalletResponse target = createWallet("LedgerReceiver", "200.00");

        TransferRequest transferRequest = new TransferRequest(source.id(), target.id(), new BigDecimal("75.00"));

        MvcResult transferResult = mockmvc.perform(
                post("/api/v1/wallets/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(objectMapper.writeValueAsString(transferRequest))
        ).andExpect(status().isCreated())
                .andReturn();

        String transferId = objectMapper.readTree(transferResult.getResponse().getContentAsString()).get("transferId").asText();

        await().atMost(10, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            mockmvc.perform(get("/api/v1/wallets/" + transferId)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED")).andExpect(jsonPath("$.amount").value(75.00));
        });

    }

}
