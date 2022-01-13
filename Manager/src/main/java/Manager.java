import com.google.gson.Gson;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Manager {
    static boolean terminate = false;

    public static void main(String[] args) throws FileNotFoundException {
        SqsUtils.createSqsQueue(Defs.managerToWorkersQueue);
        System.out.println("created ManagertoWorkersQ");
        ExecutorService pool = Executors.newFixedThreadPool(8);
        //Starting one worker
        Ec2Utils.startEc2s("Worker", Scripts.initWorker, 1);
        while(true){
            List<Message> messages;
            if (terminate) {
                pool.shutdown();
                while (!pool.isTerminated()) {
                    ;
                }
                SqsUtils.deleteQueue(Defs.localToManagerQueue);
                SqsUtils.deleteQueue(Defs.managerToWorkersQueue);
                Ec2Utils.terminateAll("Worker");
                Ec2Utils.terminateAll("Manager"); // It's only one manager I know, but who the hell cares
                break;
            }
            messages = SqsUtils.receiveMessages(Defs.localToManagerQueue,1);
            if (messages != null && messages.size() != 0) {
                SqsMessage sqsmessage = new Gson().fromJson(messages.get(0).body(), SqsMessage.class);
                terminate = sqsmessage.terminate;
                ManagerTask task = new ManagerTask(sqsmessage);
                SqsUtils.deleteMessage(messages.get(0), Defs.localToManagerQueue); // We are dealing with it
                pool.execute(task);
            }
        }
    }
}
