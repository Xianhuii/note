package com.xianhuii.springboot.service.impl;

import com.xianhuii.springboot.service.TransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {
}
