package transaction.service.impl;

import com.example.springtransaction.service.ITransactionService;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TransactionServiceImpl implements ITransactionService {
    @Autowired
    @Qualifier("myTransactionTemplate")
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private BeanFactory beanFactory;

    @Transactional
    @Override
    public void test() {
        System.out.println(transactionTemplate.getIsolationLevel());
        System.out.println("test transaction");
    }

    public Object testTransactionCallback() {
        return transactionTemplate.execute(new TransactionCallback<Object>() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                // 业务方法
                return null; // 业务返回值
            }
        });
    }

    public void testTransactionCallbackWithoutResult() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                // 业务方法
            }
        });
    }

    public void testRollback() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                try {
                    // 业务方法
                } catch (Exception e) {
                    // 手动回滚
                    status.setRollbackOnly();
                }
            }
        });
    }

    public void testPlatformTransactionManager() {
        // 事务配置
        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        transactionDefinition.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        // 创建事务
        TransactionStatus transaction = platformTransactionManager.getTransaction(transactionDefinition);
        // 执行业务方法
        try {
            // 执行业务方法
        } catch (Exception e) {
            // 回滚事务
            platformTransactionManager.rollback(transaction);
            throw e;
        }
        // 提交事务
        platformTransactionManager.commit(transaction);
    }
}
