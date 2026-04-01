package com.infotact.warehouse.repository;


import com.infotact.warehouse.common_wrappers.ResetPasswordToken;
import org.springframework.data.repository.CrudRepository;

public interface ResetPasswordRepository extends CrudRepository<ResetPasswordToken, String> {
}

