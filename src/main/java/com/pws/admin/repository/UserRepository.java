package com.pws.admin.repository;

import org.reactivestreams.Publisher;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pws.admin.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    @Query("select o from User o where o.email = :email")
    Optional <User> findUserByEmail(String email);


    @Query("select o from User o where o.email = :email")
    public User findByEmail(String email);

    @Query("select o from User o where o.resetPasswordOtp = :otp")
    User findByResetPasswordOtp(Integer otp);



}