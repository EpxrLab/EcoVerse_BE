package com.sep490.ecoverse_be.repository;

import com.sep490.ecoverse_be.entity.Account;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AccountRepository extends MongoRepository<Account, String> {
}
