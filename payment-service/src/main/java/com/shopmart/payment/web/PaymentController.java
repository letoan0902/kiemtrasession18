package com.shopmart.payment.web;

import com.shopmart.payment.repository.CustomerWalletRepository;
import com.shopmart.payment.repository.PaymentTransactionRepository;
import com.shopmart.payment.service.ChaosService;
import com.shopmart.payment.service.PaymentStats;
import com.shopmart.payment.web.PaymentDtos.ChaosResponse;
import com.shopmart.payment.web.PaymentDtos.HealthResponse;
import com.shopmart.payment.web.PaymentDtos.TransactionResponse;
import com.shopmart.payment.web.PaymentDtos.WalletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Callable;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final CustomerWalletRepository walletRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final ChaosService chaosService;
    private final PaymentStats stats;
    private final String configSource;

    public PaymentController(CustomerWalletRepository walletRepository,
                             PaymentTransactionRepository transactionRepository,
                             ChaosService chaosService, PaymentStats stats,
                             @Value("${shopmart.config.nguon:local}") String configSource) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.chaosService = chaosService;
        this.stats = stats;
        this.configSource = configSource;
    }

    @GetMapping("/wallets/{customerId}")
    public Mono<WalletResponse> wallet(@PathVariable String customerId) {
        return blocking(() -> walletRepository.findById(customerId).orElse(null))
                .map(WalletResponse::from)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("Không tìm thấy ví của khách hàng " + customerId)));
    }

    @GetMapping("/transactions/{orderId}")
    public Flux<TransactionResponse> transactionsOfOrder(@PathVariable String orderId) {
        return blocking(() -> transactionRepository.findByOrderIdOrderByIdAsc(orderId))
                .flatMapMany(Flux::fromIterable)
                .map(TransactionResponse::from);
    }

    @GetMapping("/transactions")
    public Flux<TransactionResponse> allTransactions() {
        return blocking(transactionRepository::findAllByOrderByIdAsc)
                .flatMapMany(Flux::fromIterable)
                .map(TransactionResponse::from);
    }

    @PostMapping("/admin/chaos")
    public Mono<ChaosResponse> setChaos(@RequestParam String mode,
                                        @RequestParam(required = false) Integer delaySeconds) {
        return Mono.fromCallable(() -> ChaosResponse.from(chaosService.set(mode, delaySeconds)));
    }

    @GetMapping("/admin/chaos")
    public Mono<ChaosResponse> getChaos() {
        return Mono.fromSupplier(() -> ChaosResponse.from(chaosService.current()));
    }

    @GetMapping("/stats")
    public Mono<PaymentStats.Snapshot> stats() {
        return Mono.fromSupplier(stats::snapshot);
    }

    @GetMapping("/health")
    public Mono<HealthResponse> health() {
        return Mono.just(new HealthResponse("UP", configSource));
    }

    private static <T> Mono<T> blocking(Callable<T> call) {
        return Mono.fromCallable(call).subscribeOn(Schedulers.boundedElastic());
    }
}
