package com.shopmart.payment.service;

import com.shopmart.payment.domain.CustomerWallet;
import com.shopmart.payment.repository.CustomerWalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WalletDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WalletDataInitializer.class);

    static final List<CustomerWallet> SAMPLE_WALLETS = List.of(
            new CustomerWallet("KH-001", "Nguyễn Văn An", 20_000_000L),
            new CustomerWallet("KH-002", "Trần Thị Bình", 200_000L),
            new CustomerWallet("KH-003", "Lê Minh Châu", 5_000_000L));

    private final CustomerWalletRepository walletRepository;

    public WalletDataInitializer(CustomerWalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        int created = 0;
        for (CustomerWallet sample : SAMPLE_WALLETS) {
            if (walletRepository.existsById(sample.getCustomerId())) {
                continue;
            }
            try {
                walletRepository.save(new CustomerWallet(sample.getCustomerId(), sample.getCustomerName(), sample.getBalance()));
                created++;
            } catch (DataIntegrityViolationException e) {
                log.info("Ví {} vừa được tạo bởi tiến trình khác, bỏ qua", sample.getCustomerId());
            }
        }
        log.info("Khởi tạo ví mẫu xong: tạo mới {} ví, tổng số ví hiện có {}", created, walletRepository.count());
    }
}
