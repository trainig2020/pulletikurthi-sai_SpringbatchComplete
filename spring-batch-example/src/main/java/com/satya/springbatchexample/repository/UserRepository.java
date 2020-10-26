package com.satya.springbatchexample.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.satya.springbatchexample.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
}
