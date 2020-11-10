package com.neu.cloudwebapp.user;

import com.neu.cloudwebapp.response.CustomResponse;
import com.timgroup.statsd.StatsDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.*;
import java.lang.reflect.Field;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@RestController
public class UserController {

    String PASSWORD_PATTERN = "((?=.*[a-z])(?=.*\\d)(?=.*[A-Z])(?=.*[@#$%!]).{9,40})";

    Pattern passPattern = Pattern.compile(PASSWORD_PATTERN);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StatsDClient statsDClient;

    private final static Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    @PostMapping("/v1/user")
    public ResponseEntity<HashMap<String, Object>> register(@RequestBody User user){

        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.v1.user.api.post");
        LOGGER.info("Registering new user");

        user.setAccount_created(new Date());
        user.setAccount_updated(new Date());

        user.setUsername(user.getUsername());

        long startdb = System.currentTimeMillis();
        User findUser = userRepository.findUserByUsername(user.getUsername());

        if(findUser!=null) {
            LOGGER.error("User already exists");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("dbquery.post.user", (System.currentTimeMillis() - startdb));
            statsDClient.recordExecutionTime("timer.user.http.post", time);
            return new ResponseEntity(new CustomResponse(new Date(),"User Already Exists","" ), HttpStatus.BAD_REQUEST);
        }

        if (user.getUsername() == null) {
            LOGGER.error("Username/Email is empty");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.user.http.post", time);
            return new ResponseEntity(new CustomResponse(new Date(),"Username/Email must not be empty","" ),HttpStatus.BAD_REQUEST);
        }

        if(!userService.validateEmail(user.getUsername())) {
            LOGGER.error("Username is invalid");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.user.http.post", time);
            return new ResponseEntity(new CustomResponse(new Date(),"Username must be a valid email","" ),HttpStatus.BAD_REQUEST);
        }

        if(!passPattern.matcher(user.getPassword()).matches()) {
            LOGGER.error("Password is invalid");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.user.http.post", time);
            return new ResponseEntity(new CustomResponse(new Date(),"Enter a valid password "," " +
                    "Be between 8 and 40 characters long  ||  " +
                    "Contain at least one digit.  ||  " +
                    "Contain at least one lower case character.  ||  " +
                    "Contain at least one upper case character.  ||  " +
                    "Contain at least on special character from [ @ # $ % ! ] " ), HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userRepository.save(user);

        LOGGER.info("New user registered successfully!");
        long end = System.currentTimeMillis();
        long time = end-start;
        statsDClient.recordExecutionTime("dbquery.post.user", (System.currentTimeMillis() - startdb));
        statsDClient.recordExecutionTime("timer.user.http.post", time);
        return new ResponseEntity<>(userService.getUser(user.getUsername()), HttpStatus.CREATED);
    }



    @GetMapping("v1/user/self")
    public ResponseEntity<HashMap<String, Object>> getUser(Principal principal){

        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.v1.user.self.api.get");
        LOGGER.info("Getting user data: self");

        long startdb = System.currentTimeMillis();
        User user = userRepository.findUserByUsername(principal.getName());

        if(user == null) {
            LOGGER.error("User not found");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.user.http.get", time);
            return new ResponseEntity(new CustomResponse(new Date(),"User not found","" ), HttpStatus.NOT_FOUND);

        }

        LOGGER.info("User fetched successfully!");
        long end = System.currentTimeMillis();
        long time = end-start;
        statsDClient.recordExecutionTime("dbquery.get.user", (System.currentTimeMillis() - startdb));
        statsDClient.recordExecutionTime("timer.user.http.get", time);
        return  ResponseEntity.ok(userService.getUser(user.getUsername()));

    }

    @GetMapping("/v1/user/{suuid}")
    public ResponseEntity<HashMap<String, Object>> getUserById(@PathVariable String suuid){

        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.v1.user.id.api.get");
        LOGGER.info("Getting user data by ID");

        if(!userService.checkUuid(suuid)) {
            LOGGER.error("Invalid type of ID");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.user.http.get", time);
            return new ResponseEntity(new CustomResponse(new Date(),"ID must be of type UUID","" ),HttpStatus.BAD_REQUEST);
        }

        UUID uuid = UUID.fromString(suuid);

        long startdb = System.currentTimeMillis();
        Optional<User> user = userRepository.findById(uuid);

        if(!user.isPresent()) {
            LOGGER.error("User not found");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.user.http.get", time);
            return new ResponseEntity(new CustomResponse(new Date(),"User not found","" ), HttpStatus.NOT_FOUND);
        }

        LOGGER.info("User data fetched successfully!");
        long end = System.currentTimeMillis();
        long time = end-start;
        statsDClient.recordExecutionTime("dbquery.get.user", (System.currentTimeMillis() - startdb));
        statsDClient.recordExecutionTime("timer.user.http.get", time);
        return  ResponseEntity.ok(userService.getUser(user.get().getUsername()));

    }

    @PutMapping("v1/user/self")
    public ResponseEntity<HashMap<String, Object>> updateUser(@RequestBody Map<Object, Object> fields, Principal principal){

        long start = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.v1.user.self.api.put");
        LOGGER.info("Updating user");
        User user = userRepository.findUserByUsername(principal.getName());
        long startdb = System.currentTimeMillis();

        if(user == null) {
            LOGGER.error("User not found");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.user.http.put", time);
            statsDClient.recordExecutionTime("dbquery.put.user", (System.currentTimeMillis() - startdb));
            return new ResponseEntity(new CustomResponse(new Date(),"User not found","" ),HttpStatus.NOT_FOUND);
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
                    || k.equals("username")
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
            LOGGER.error("Invalid password");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("dbquery.put.user", (System.currentTimeMillis() - startdb));
            statsDClient.recordExecutionTime("timer.user.http.put", time);
            return new ResponseEntity(new CustomResponse(new Date(),"Enter a valid password "," " +
                    "Be between 8 and 40 characters long  ||  " +
                    "Contain at least one digit.  ||  " +
                    "Contain at least one lower case character.  ||  " +
                    "Contain at least one upper case character.  ||  " +
                    "Contain at least on special character from [ @ # $ % ! ] " ), HttpStatus.BAD_REQUEST);
        }

        if(flag.get() == true) {
            LOGGER.error("Attempting to update fields [Username, Account Created/Updated, Email Address, ID] : Denied");
            long end = System.currentTimeMillis();
            long time = end-start;
            statsDClient.recordExecutionTime("timer.user.http.put", time);
            return new ResponseEntity(new CustomResponse(new Date(),"Cannot update details","Username, Account Created/Updated, Email Address, ID" ),HttpStatus.BAD_REQUEST);
        }

        user.setAccount_updated(new Date());
        userRepository.save(user);

        LOGGER.info("User data updated successfully!");
        long end = System.currentTimeMillis();
        long time = end-start;
        statsDClient.recordExecutionTime("dbquery.put.user", (System.currentTimeMillis() - startdb));
        statsDClient.recordExecutionTime("timer.user.http.get", time);
        return new ResponseEntity<>(userService.getUser(user.getUsername()), HttpStatus.OK);

    }

}
