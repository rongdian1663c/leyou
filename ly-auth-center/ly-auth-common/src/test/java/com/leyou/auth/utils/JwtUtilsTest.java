package com.leyou.auth.utils;

import com.leyou.auth.pojo.UserInfo;
import org.junit.Test;

import java.security.PrivateKey;
import java.security.PublicKey;

public class JwtUtilsTest {

        @Test
        public void generateTokenInMinutes() throws Exception {
            String publicKeyFilename = "D:\\test\\rsa\\rsa.pub";
            String privateKeyFilename = "D:\\test\\rsa\\rsa.pri";


            PrivateKey privateKey = RsaUtils.getPrivateKey(privateKeyFilename);
           PublicKey publicKey = RsaUtils.getPublicKey(publicKeyFilename);
            String token = JwtUtils.generateTokenInMinutes(new UserInfo(2L, "22"), privateKey, 5);

          // String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6MiwidXNlcm5hbWUiOiIyMiIsImV4cCI6MTUzNTc3MzE0NX0.R8lGQ_9ts4HbxrTUSLEbjH_5AyVbjy5WVl20VAl_LGB5e9aM37tJGUfBuPZAm5oRQTcxbprU6pFpd2gSLBSbrMEgZuaffWjL6Sp4-yZVPTWKZbtYKX6kDtRUCcoK9_AlhOCOnli_5PcWqUJj1E596Xs3PM7tMUoDIBxvtY7H7dM";
            System.out.println("token = " + token);

            Thread.sleep(6000l);

            UserInfo info = JwtUtils.getInfoFromToken(token, publicKey);
            System.out.println("info = " + info);
        }
    }