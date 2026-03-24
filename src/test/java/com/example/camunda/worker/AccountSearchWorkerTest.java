package com.example.camunda.worker;

import com.example.camunda.model.Account;
import com.example.camunda.model.AccountRole;
import com.example.camunda.model.Customer;
import com.example.camunda.model.CustomerAccount;
import com.example.camunda.repository.AccountRepository;
import com.example.camunda.repository.CustomerAccountRepository;
import com.example.camunda.service.AccountService;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountSearchWorkerTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerAccountRepository customerAccountRepository;

    @Mock
    private ActivatedJob job;

    private AccountSearchWorker worker;

    @BeforeEach
    void setUp() {
        AccountService accountService = new AccountService(accountRepository, customerAccountRepository);
        worker = new AccountSearchWorker(accountService);
    }

    @Test
    void handleJob_withEmptyAccountNumberAndCustomerId_shouldSearchByCustomerOnly() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("accountNumber", "");
        variables.put("customerId", "100");

        when(job.getVariablesAsMap()).thenReturn(variables);
        when(job.getKey()).thenReturn(123L);
        Account account = buildAccount();
        CustomerAccount link = buildLink(account);
        when(customerAccountRepository.findByCustomer_CustomerId(100L)).thenReturn(List.of(link));
        when(accountRepository.findByAccountId(10001L)).thenReturn(Optional.of(account));

        Map<String, Object> result = worker.handleJob(job);

        assertNotNull(result);
        assertEquals("SUCCESS", result.get("searchStatus"));
        assertEquals(1, result.get("accountCount"));
        verify(customerAccountRepository).findByCustomer_CustomerId(100L);
        verify(accountRepository).findByAccountId(10001L);
    }

    @Test
    void handleJob_withArgumentsWrapperAndCustomerIDAlias_shouldSearchByCustomerOnly() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("accountNumber", "");
        arguments.put("customerID", "100");

        Map<String, Object> variables = new HashMap<>();
        variables.put("arguments", arguments);

        when(job.getVariablesAsMap()).thenReturn(variables);
        when(job.getKey()).thenReturn(124L);
        Account account = buildAccount();
        CustomerAccount link = buildLink(account);
        when(customerAccountRepository.findByCustomer_CustomerId(100L)).thenReturn(List.of(link));
        when(accountRepository.findByAccountId(10001L)).thenReturn(Optional.of(account));

        Map<String, Object> result = worker.handleJob(job);

        assertNotNull(result);
        assertEquals("SUCCESS", result.get("searchStatus"));
        assertEquals(1, result.get("accountCount"));
        verify(customerAccountRepository).findByCustomer_CustomerId(100L);
        verify(accountRepository).findByAccountId(10001L);
    }

    private Account buildAccount() {
        Account account = new Account();
        account.setAccountId(10001L);
        account.setAccountNumber("SA-1001-NDL");
        return account;
    }

    private CustomerAccount buildLink(Account account) {
        Customer customer = new Customer();
        customer.setCustomerId(100L);
        customer.setCustomerName("Niall Deehan");
        customer.setEmail("niall.deehan@camunda.com");

        CustomerAccount link = new CustomerAccount();
        link.setId(1L);
        link.setAccount(account);
        link.setCustomer(customer);
        link.setRole(AccountRole.OWNER);
        return link;
    }
}
