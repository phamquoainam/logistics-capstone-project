package com.elogistic.backend.finance.internal.web;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;
@RestController
@Profile("architecture-negative-fixture")
public class NegativeTransactionalController { @Transactional public void forbidden() {} }
