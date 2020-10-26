package com.satya.springbatchexample.batch;

import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.satya.springbatchexample.model.User;
import com.satya.springbatchexample.repository.UserRepository;

import java.util.List;

@Component
public class DBWriter1 implements ItemWriter<User> {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void write(List<? extends User> users) throws Exception {

        System.out.println("Data Saved for Users: " + users);
        userRepository.saveAll(users);
    }
}
