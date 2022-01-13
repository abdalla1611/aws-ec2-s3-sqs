

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
public class main {

    public static void main(String[] args) throws InterruptedException, IOException {
        if (args == null || args.length == 0){
            System.out.println("Missing class name to start!");
            return;
        }
        String path = System.getProperty("user.home") + File.separator + ".aws/credentials";
        Credentials.setAll(path);
        if (args[0].equals("Worker"))
            Worker.main(new String[]{});
        else if (args[0].equals("Local") && args.length == 4)
            Local.main(new String []{args[1],args[2],args[3]});
        else if (args[0].equals("Manager"))
            Manager.main(new String[]{});
        else
            System.out.println("there is no case that matches ur input");
    }
}
