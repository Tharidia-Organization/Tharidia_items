import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class hash_uuid {
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
        if (args.length == 0) {
            System.out.println("Usage: java hash_uuid <uuid>");
            System.out.println("Example: java hash_uuid 12345678-1234-1234-1234-123456789012");
            return;
        }
        
        try {
            UUID uuid = UUID.fromString(args[0]);
            String hash = hashUUID(uuid);
            System.out.println("UUID: " + uuid);
            System.out.println("Hash: " + hash);
            System.out.println();
            System.out.println("Add this line to ALLOWED_UUIDS_HASH in ServerTransferFallback.java:");
            System.out.println("ALLOWED_UUIDS_HASH.add(\"" + hash + "\");");
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid UUID format. Please use format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx");
        }
    }
}
