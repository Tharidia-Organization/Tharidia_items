package com.tharidia.tharidia_things.servertransfer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.UUID;

public class UUIDHashGenerator {
    private static final String SALT = "tharidia_fallback_salt_2024_secure";
    
    public static String hashUUID(UUID uuid) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String uuidWithSalt = uuid.toString() + SALT;
            byte[] hash = digest.digest(uuidWithSalt.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash UUID", e);
        }
    }
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== UUID Hash Generator for Fallback Whitelist ===");
        System.out.println("This tool generates SHA-256 hashes for the whitelist");
        System.out.println();
        
        while (true) {
            System.out.print("Enter UUID (or 'quit' to exit): ");
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("quit")) {
                break;
            }
            
            try {
                UUID uuid = UUID.fromString(input);
                String hash = hashUUID(uuid);
                System.out.println("UUID: " + uuid);
                System.out.println("Hash: " + hash);
                System.out.println("Add this to ALLOWED_UUIDS_HASH in ServerTransferFallback.java");
                System.out.println();
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid UUID format. Please use format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx");
                System.out.println();
            }
        }
        
        scanner.close();
        System.out.println("Goodbye!");
    }
}
