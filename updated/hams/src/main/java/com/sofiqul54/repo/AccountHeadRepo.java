package com.sofiqul54.repo;

import com.sofiqul54.entity.AccountHead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountHeadRepo extends JpaRepository<AccountHead, Long> {
    Optional<AccountHead> findByAccountname(String accountName);
    boolean existsAccountHeadByAccountname(String accountName);
}
