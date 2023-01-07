package com.example.springtransaction.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class TransactionConfig {
    @Bean
    public TransactionTemplate myTransactionTemplate(PlatformTransactionManager platformTransactionManager) {
        TransactionTemplate myTransactionTemplate = new TransactionTemplate(platformTransactionManager);
        // 自定义事务配置
        myTransactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        return myTransactionTemplate;
    }
}
