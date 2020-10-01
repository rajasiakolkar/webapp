package com.neu.cloudwebapp.user;

import javassist.Loader;
import org.hibernate.SessionFactory;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public HashMap<String, Object> getUserById(UUID id) {
        try{

            HashMap<String, Object> obj = new HashMap<>();
            User user = userRepository.findById(id).get();

            obj.put("id", user.getId().toString());
            obj.put("first_name", user.getFirst_name());
            obj.put("last_name", user.getLast_name());
            obj.put("email_address", user.getEmail_address());
            obj.put("account_created", user.getAccount_created());
            obj.put("account_updated", user.getAccount_updated());

            return obj;

        }
        catch(Exception ex){
            return null;
        }
    }

//
//    public void saveUser(User user) {
//        SessionFactory sessionFactory = null;
//        sessionFactory.getCurrentSession().saveOrUpdate(user);
//    }

    public User findById(String suuid) {
        UUID uuid =  UUID.fromString(suuid);
        User user = userRepository.findById(uuid).get();
        return user;
    }

}
