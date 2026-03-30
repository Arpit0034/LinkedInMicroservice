package com.linkedInProject.userService.utils;

import static org.mindrot.jbcrypt.BCrypt.*;

public class BCrypt {
    public static String hash(String s){
        return hashpw(s,gensalt()) ;
    }

    public static boolean match(String password , String passwordHashed){
        return checkpw(password,passwordHashed) ;
    }
}
