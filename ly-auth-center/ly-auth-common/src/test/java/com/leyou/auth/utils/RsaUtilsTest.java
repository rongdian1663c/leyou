package com.leyou.auth.utils;

import org.junit.Test;

import java.security.PrivateKey;
import java.security.PublicKey;

public class RsaUtilsTest {

    @Test
    public void generateKey() throws Exception {
        String publicKeyFilename = "D:\\test\\rsa\\rsa.pub";
        String privateKeyFilename = "D:\\test\\rsa\\rsa.pri";
        //RsaUtils.generateKey(publicKeyFilename, privateKeyFilename, "helloWorld");

        // 获取公钥和私钥
        PublicKey publicKey = RsaUtils.getPublicKey(publicKeyFilename);
        System.out.println("publicKey = " + publicKey);

        PrivateKey privateKey = RsaUtils.getPrivateKey(privateKeyFilename);
        System.out.println("privateKey = " + privateKey);
    }
}