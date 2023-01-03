package com.example.springtransaction.controller;

import com.example.springtransaction.service.ITransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionController {
    @Autowired
    private ITransactionService transactionService;

    @GetMapping("test")
    public String test() {
        transactionService.test();
        return "test";
    }
}
