/**
 * @Author Aditya Kelkar
 */


package com.data.datagen;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

@RestController
public class GeneratorController {

    @GetMapping("/datahit")
    public Data getData(@RequestHeader(value = "User-Agent") String userAgent) {
        InetAddress networkinfo;
        try {
            networkinfo = InetAddress.getLocalHost();
            Data d = new Data(getRandom(20), getRandomIP(), networkinfo.getHostName(), userAgent);
            return d;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getRandomIP() {
        Random r = new Random();
        return r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256) + "." + r.nextInt(256);
    }

    public String getRandom(int n) {
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";


        // create StringBuffer size of AlphaNumericString
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            int index = (int) (AlphaNumericString.length() * Math.random());
            sb.append(AlphaNumericString.charAt(index));
        }
        return sb.toString();
    }
}
