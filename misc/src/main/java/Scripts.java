
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.auth.credentials.internal.ProfileCredentialsUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Scripts {
    public static String loadCredentials() {
        BufferedReader bufferedReader;

        try {
            String path = System.getProperty("user.home") + File.separator + "/.aws/credentials";
            File customDir = new File(path);
            bufferedReader = new BufferedReader(new FileReader(customDir));
            StringBuilder ret = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                ret.append(line).append("\n");
            }
            return ret.toString();
        } catch (IOException e) {
            System.out.println(e);
            return null;
        }
    }

    public static String initInstanceBash =
            "#!/bin/bash\n" +
                    "sudo mkdir /root/.aws\n" +
                    "sudo yum update -y\n" +
                    "wget https://myawscodebucket.s3.amazonaws.com/Local-1.0-SNAPSHOT.jar\n"+
                    "sudo yum install java-1.8.0 -y\n" +
                    "echo \"" + loadCredentials() + "\" > /root/.aws/credentials" + "\n";
    public static String initWorker = initInstanceBash + "java -jar /Local-1.0-SNAPSHOT.jar Worker\n";
    public static String initManager = initInstanceBash + "java -jar /Local-1.0-SNAPSHOT.jar Manager\n";

    public static void main(String[] args) throws InterruptedException {
        int fun2 = 1;
        int fun = 0;
        while(true) {
            Thread.sleep(2*1000);
            if (fun2 != 0) System.out.println("Waiting manager");
            fun2 = 0;
            if (fun % 5 == 4) System.out.print("\b\b\b\b\b\b    \b\b\b\b");
            if (fun % 5 != 4 )System.out.print(".");
            fun++;
        }
    }
}


