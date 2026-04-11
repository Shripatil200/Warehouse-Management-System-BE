package com.infotact.warehouse.repository;

import com.infotact.warehouse.common_wrappers.VerifiedProof;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerifiedProofRepository extends CrudRepository<VerifiedProof, String> {}
