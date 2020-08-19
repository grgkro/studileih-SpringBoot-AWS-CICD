package com.example.studileih.Controller;

import com.example.studileih.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
//import io.swagger.annotations.ApiResponses;
//import io.swagger.annotations.ApiResponse;

import com.example.studileih.Security.AuthRequest;
import com.example.studileih.Security.JwtUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@CrossOrigin("http://localhost:4200")
@Api(tags = "Authentication API - controller methods for authenticating Users")
public class AuthenticationController {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private AuthenticationManager authenticationManager;

    @GetMapping("/")
    @ApiOperation(value = "Dummy welcome message")
    public String welcome() {
        return "Welcome to StudiLeih !!!";
    }

    @PostMapping("/authenticate")
    @ApiOperation(value = "Authenticate user by username and password, return the JWT Token")
    public ResponseEntity<String> generateToken(@RequestBody AuthRequest authRequest) throws Exception {
    	System.out.println("Username: " + authRequest.getUserName() + ", Password: " + authRequest.getPassword());

    	try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUserName(), authRequest.getPassword())
            );
        } catch (Exception ex) {
            System.out.println(ex);
            return new ResponseEntity<>("Invalid Username / password", HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity<>(jwtUtil.generateToken(authRequest.getUserName()), HttpStatus.OK);
    }
}

//@RestController
//@CrossOrigin
//@Api(tags = "Authentication API - controller methods for authenticating Users")
//public class AuthenticationController {
//
//    @Autowired
//    private JwtUtil jwtUtil;
//    @Autowired
//    private AuthenticationManager authenticationManager;
//    @Autowired
//    private UserService userService;
//
//    @GetMapping("/")
//    @ApiOperation(value = "Dummy welcome message")
//    public String welcome(HttpServletRequest httpServletRequest) {
//        String autorizationHeader = httpServletRequest.getHeader("Authorization");
//        String token;
//        String userName;
//        if( autorizationHeader != null && autorizationHeader.startsWith("Bearer")) {
//            token = autorizationHeader.substring(7);
//            if (token != null) {
//                return "Welcome to StudiLeih !!!";
//            }
//        }
//        return "Access denied !!!";
//    }
//
//    @PostMapping("/authenticate")
//    @ApiOperation(value = "Authenticate user by username and password, return the JWT Token")
//    public String generateToken(String username, String password) throws Exception {
//    	System.out.println("Username: " + username + ", Password: " + password);
////        authenticationManager.authenticate(
////                new UsernamePasswordAuthenticationToken(username, password)
////        );
//    	try {
//            authenticationManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(username, password)
//            );
//        } catch (Exception ex) {
//            System.out.println(ex);
//            throw new Exception("inavalid username/password");
//        }
//        String token = jwtUtil.generateToken(username);
//        System.out.println(token);
//        return token;
//    }
//}