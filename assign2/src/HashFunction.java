import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class HashFunction {
    private File file;
    private String str;


    HashFunction(String file) throws FileNotFoundException {
        this.file = new File(file);
        Scanner myReader = new Scanner(this.file);
        while (myReader.hasNextLine()) {
            String data = myReader.nextLine();
            this.str += data ;
        }
        myReader.close();
    }

    HashFunction(String IPaddress, int port, boolean isServer){
        this.str = IPaddress + port;
    }


    private byte[] hashNodes() throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        return digest.digest(
                this.str.getBytes(StandardCharsets.UTF_8));
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public String createHashNode() throws NoSuchAlgorithmException {
        return bytesToHex(hashNodes());
    }

}