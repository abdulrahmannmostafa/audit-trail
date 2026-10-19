package com.qeema.util.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.qeema.util.identity.entity.User;

public interface UserRepository extends JpaRepository<User, String> {

}
