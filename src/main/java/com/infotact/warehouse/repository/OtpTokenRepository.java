package com.infotact.warehouse.repository;

import com.infotact.warehouse.common_wrappers.OtpToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpTokenRepository extends CrudRepository<OtpToken, String> {}
