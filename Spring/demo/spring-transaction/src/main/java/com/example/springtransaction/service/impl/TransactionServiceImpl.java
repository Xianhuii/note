package com.example.springtransaction.service.impl;

import com.example.springtransaction.service.ITransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionServiceImpl implements ITransactionService {
    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Transactional
    @Override
    public void test() {
        System.out.println("test transaction");
    }
}
