package lk.ijse.spring.smartplantmanagementsystem.service;

import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenDanyListService {
    private final ConcurrentHashMap<String, Date> danyList = new ConcurrentHashMap<>();

    public void addToDenyList(String jti, Date expiryDate) {
        danyList.put(jti, expiryDate);
    }

    public boolean isDenyList(String jti) {
        // can add a scheduled task to clean up expired tokens from the map
        return danyList.containsKey(jti);
    }
}
