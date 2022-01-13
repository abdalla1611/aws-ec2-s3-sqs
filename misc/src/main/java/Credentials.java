
import software.amazon.awssdk.auth.credentials.*;

import java.io.*;

public class Credentials {
    public static String accessKey;
    public static String secretKey;
    public static String sessionToken;
    public static void setAll(String path){
        try {
            BufferedReader breader = new BufferedReader(new FileReader(path));
            String line = breader.readLine();
            line = breader.readLine();
            accessKey = line.split("=", 2)[1];;
            line = breader.readLine();
            secretKey = line.split("=", 2)[1];;;
            line = breader.readLine();
            if (line!=null) sessionToken = line.split("=", 2)[1];;;
        }
        catch (IOException e)
        {
            System.out.println(e);
        }

    }
    static public AwsCredentials getCredentials()
    {
        if (sessionToken==null)
            return AwsBasicCredentials.create(accessKey, secretKey);
        else
            return AwsSessionCredentials.create(accessKey,secretKey,sessionToken);
    }
    static public AwsCredentials create()
    {
        return new CredentialsObject(accessKey, secretKey, sessionToken).getCredentials();
    }

}
