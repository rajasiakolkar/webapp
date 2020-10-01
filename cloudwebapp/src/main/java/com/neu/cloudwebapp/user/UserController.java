package com.neu.cloudwebapp.user;

import com.neu.cloudwebapp.response.CustomResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.security.Principal;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@RestController
public class UserController {

    String PASSWORD_PATTERN = "((?=.*[a-z])(?=.*\\d)(?=.*[A-Z])(?=.*[@#$%!]).{8,40})";

    Pattern passPattern = Pattern.compile(PASSWORD_PATTERN);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @RequestMapping(method= RequestMethod.GET,path = "/")
    public String beginApp(){
        LocalTime localTime= LocalTime.now();
        return localTime.toString();
    }

    @PostMapping("/user/register")
    public ResponseEntity<HashMap<String, Object>> register(@RequestBody User user){

        user.setAccount_created(new Date());
        user.setAccount_updated(new Date());


        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\."+
                "[a-zA-Z0-9_+&*-]+)*@" +
                "(?:[a-zA-Z0-9-]+\\.)+[a-z" +
                "A-Z]{2,7}$";

        Pattern pat = Pattern.compile(emailRegex);


        user.setUsername(user.getEmail_address());

        User findUser = userRepository.findUserByUsername(user.getUsername());

        if(findUser!=null)
            return new ResponseEntity(new CustomResponse(new Date(),"User Already Exist","" ), HttpStatus.BAD_REQUEST);


        if (user.getUsername() == null)
            return new ResponseEntity(new CustomResponse(new Date(),"Username/Email must not be empty","" ),HttpStatus.BAD_REQUEST);

        if(!pat.matcher(user.getUsername()).matches())
            return new ResponseEntity(new CustomResponse(new Date(),"Username must be a valid email","" ),HttpStatus.BAD_REQUEST);

        if(!passPattern.matcher(user.getPassword()).matches())
            return new ResponseEntity(new CustomResponse(new Date(),"Enter a valid password "," " +
                    "Be between 8 and 40 characters long  ||  " +
                    "Contain at least one digit.  ||  " +
                    "Contain at least one lower case character.  ||  " +
                    "Contain at least one upper case character.  ||  " +
                    "Contain at least on special character from [ @ # $ % ! ] " ), HttpStatus.BAD_REQUEST);


        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userRepository.save(user);

        return new ResponseEntity<>(userService.getUserById(user.getId()), HttpStatus.CREATED);
    }



    @GetMapping("/user/{suuid}")
    public ResponseEntity<HashMap<String, Object>> getUser(@PathVariable String suuid, Principal principal){

        UUID uuid;
        if(!checkUuid(suuid))
            return new ResponseEntity(new CustomResponse(new Date(),"ID must be of type UUID","" ), HttpStatus.BAD_REQUEST);

        uuid = UUID.fromString(suuid);
        Optional<User> user = userRepository.findById(uuid);


        if(!principal.getName().equals(user.get().getEmail_address())) {
            return new ResponseEntity(new CustomResponse(new Date(),"Not Authenticated","" ), HttpStatus.BAD_REQUEST);
        }

        if(!user.isPresent())
            return new ResponseEntity(new CustomResponse(new Date(),"User not found","" ), HttpStatus.NOT_FOUND);

        return  ResponseEntity.ok(userService.getUserById(uuid));

    }

    @PutMapping("/user/{suuid}")
    public ResponseEntity<HashMap<String, Object>> updateUser(@RequestBody Map<Object, Object> fields, @PathVariable String suuid, Principal principal){

        UUID uuid;
        if(!checkUuid(suuid))
            return new ResponseEntity(new CustomResponse(new Date(),"ID must be of type UUID","" ),HttpStatus.BAD_REQUEST);

        uuid = UUID.fromString(suuid);

        User user = userService.findById(suuid);

        if(user.getId() == null)
            return new ResponseEntity(new CustomResponse(new Date(),"User not found","" ),HttpStatus.NOT_FOUND);

        if(!principal.getName().equals(user.getEmail_address())) {
            return new ResponseEntity(new CustomResponse(new Date(),"Not Authenticated","" ), HttpStatus.BAD_REQUEST);
        }

        AtomicBoolean flag = new AtomicBoolean(false);
        AtomicBoolean flagPass = new AtomicBoolean(false);

        for (Map.Entry<Object, Object> entry : fields.entrySet()) {
            Object k = entry.getKey();
            Object v = entry.getValue();
            Field field = ReflectionUtils.findField(User.class, (String) k);
            field.setAccessible(true);
            if (k.equals("username")
                    || k.equals("account_created")
                    || k.equals("account_updated")
                    || k.equals("email_address")
                    || k.equals("id")) {
                flag.set(true);
                break;
            }

            if(k.equals("password")) {
                if(!passPattern.matcher((CharSequence) v).matches()) {
                    flagPass.set(true);
                    break;
                } else {
                    v = (passwordEncoder.encode((CharSequence) v));
                }
            }

            ReflectionUtils.setField(field, user, v);
        }

        if(flagPass.get() == true) {
            return new ResponseEntity(new CustomResponse(new Date(),"Enter a valid password "," " +
                    "Be between 8 and 40 characters long  ||  " +
                    "Contain at least one digit.  ||  " +
                    "Contain at least one lower case character.  ||  " +
                    "Contain at least one upper case character.  ||  " +
                    "Contain at least on special character from [ @ # $ % ! ] " ), HttpStatus.BAD_REQUEST);
        }

        if(flag.get() == true) {
            return new ResponseEntity(new CustomResponse(new Date(),"Cannot update details","Username, Account Created/Updated, Email Address, ID" ),HttpStatus.BAD_REQUEST);
        }

        user.setAccount_updated(new Date());
        userRepository.save(user);

        return new ResponseEntity<>(userService.getUserById(uuid), HttpStatus.OK);

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
