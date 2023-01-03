package com.example.springtransaction.service.impl;

import com.example.springtransaction.service.ITransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionServiceImpl implements ITransactionService {

    @Transactional
    @Override
    public void test() {
        System.out.println("test transaction");
    }
}
