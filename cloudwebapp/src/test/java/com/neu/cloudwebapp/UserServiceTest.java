package com.neu.cloudwebapp;

import com.neu.cloudwebapp.user.User;
import com.neu.cloudwebapp.user.UserRepository;
import com.neu.cloudwebapp.user.UserService;
import org.assertj.core.api.Assertions;
import org.junit.Before;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = CloudwebappApplication.class)
public class UserServiceTest {

    @InjectMocks
    UserService userService;

    @Mock
    UserRepository userRepository;

    private static User USER;

    @Before
    public void initialSetup() {
        this.USER = new User(UUID.randomUUID(), "test_firstName", "test_lastName", "test_Passw0od!", "test@test.com", new Date(), new Date());
    }

    @Test
    public void test_getUserById() {
        Mockito.when(userRepository.findById(USER.getId())).thenReturn(Optional.of(USER));
        User user = userService.findById(USER.getId().toString());
        Assertions.assertThat(user).isEqualTo(USER);
    }

}
