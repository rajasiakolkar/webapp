package com.neu.cloudwebapp.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public HashMap<String, Object> getUser(String username) {
        try{

            HashMap<String, Object> obj = new HashMap<>();
            User user = userRepository.findUserByUsername(username);

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

    public boolean validateEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\."+
                "[a-zA-Z0-9_+&*-]+)*@" +
                "(?:[a-zA-Z0-9-]+\\.)+[a-z" +
                "A-Z]{2,7}$";

        Pattern pat = Pattern.compile(emailRegex);

        if(pat.matcher(email).matches()) return true;

        return false;
    }

    public boolean checkUuid(String uuid){
        try{
            UUID nuuid = UUID.fromString(uuid);
        }
        catch (Exception ex){
            return false;
        }
        return true;
    }

}
