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
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = CloudwebappApplication.class)
public class UserServiceTest {

    @InjectMocks
    UserService userService;

    private static String email;

    @Before
    public void initialSetup() {
        this.email = "hello.user@welcome.com";
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testValidateEmail() {
        assertEquals(true, userService.validateEmail(email));
    }

}

//
////    @Mock
////    UserRepository userRepository;
////
////    private static User USER;
////    //HashMap<String, Object> map = new HashMap<>();
////
////    @Before
////    public void initialSetup() {
////        this.USER = new User(UUID.randomUUID(), "test_firstName", "test_lastName", "test_Passw0od!", "test@test.com", new Date(), new Date());
//////        obj.put("id", user.getId().toString());
//////        obj.put("first_name", user.getFirst_name());
//////        obj.put("last_name", user.getLast_name());
//////        obj.put("email_address", user.getEmail_address());
//////        obj.put("account_created", user.getAccount_created());
//////        obj.put("account_updated", user.getAccount_updated());
////    }
////
////    @Test
////    public void test_getUser() {
////        Mockito.when(userRepository.findById(USER.getId())).thenReturn(Optional.of(USER));
////        User user = userService.findById(USER.getId().toString());
////        Assertions.assertThat(user).isEqualTo(USER);
////    }
//
//
