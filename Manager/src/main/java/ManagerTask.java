import com.google.gson.Gson;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ManagerTask implements Runnable {
    public volatile static int CurrWorkers = 1;
    public volatile static int MaxWorkers = 15;
    private int random = (int) (Math.random() * 10000);
    final SqsMessage msg;
    final String replyQueue;

    public ManagerTask(SqsMessage msg) {
        this.msg = msg;
        this.replyQueue = msg.sqsReplyQueue;
    }

    public void sendTasksToWorkers(Mission m) {
        SqsMessage managerToWorkerMessage = new SqsMessage(Defs.workerToManagerQueue + random,
                new Gson().toJson(m), false);
        SqsUtils.sendMessage(managerToWorkerMessage, SqsUtils.getQueueUrl(Defs.managerToWorkersQueue));
    }

    public void run() {
        // ------------------------- taking s3 content as the msg says -------------------------
        SqsUtils.createSqsQueue(Defs.workerToManagerQueue + random); //workers Queue
        String ReceivedfromS3 = S3Utils.getObject("MyInput.txt", msg.body);
        String[] splitbyline = ReceivedfromS3.split("\n");
        int NumOfMissions = 0;
        // ----------------------------- sending Missions --------------------------------------
        for (String l : splitbyline) {
            System.out.println("reading line:" + l); //FOR TEST
            String[] splitbytab = l.split("\t");
            String op = splitbytab[0];
            String URL = splitbytab[1];
            Mission newmisson = new Mission(op, URL);
            sendTasksToWorkers(newmisson);
            NumOfMissions++;
        }
        int NumOfWorkers = NumOfMissions / msg.numOfMessagesPerWorker;
        if (NumOfWorkers > 0) {
            MoreWorkers(NumOfWorkers);
        }
        // --------------------- lets see the workers replies -------------------------------
        int RepliedMesseges = 0;
        SqsMessage sqsmsg;
        File WorkersOutputFile = new File("WorkersOutputFile.txt");
        FileWriter fw = null;
        try {
            fw = new FileWriter("WorkersOutputFile.txt");
        } catch (IOException e) {
            System.out.println("FileWriter Failed");
            e.printStackTrace();
        }
        while (RepliedMesseges < NumOfMissions) {
            List<Message> Messeges = SqsUtils.receiveMessages(Defs.workerToManagerQueue + random);
            if (Messeges.size() != 0) {
                for (Message msg : Messeges) {
                    System.out.println("We have a Reply !!");
                    sqsmsg = new Gson().fromJson(msg.body(), SqsMessage.class);
                    try {
                        assert fw != null;
                        fw.write(sqsmsg.body);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    RepliedMesseges++;
                    SqsUtils.deleteMessage(msg, Defs.workerToManagerQueue + random);
                }
            }
            if (Messeges.size() == 0) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("we don't have a reply atm");
            }
        }
        System.out.println("uploading file to: " + msg.body);
        S3Utils.putObject(Paths.get("WorkersOutputFile.txt"), "WorkersOutputFile.txt", msg.body);
        WorkersOutputFile.delete();
        System.out.println("manager is done");
        SqsMessage done = new SqsMessage("", "done", false);
        SqsUtils.sendMessage(done, SqsUtils.getQueueUrl(msg.sqsReplyQueue));
        SqsUtils.deleteQueue(Defs.workerToManagerQueue + random);
    }

    private synchronized static void MoreWorkers(int numOfWantedWorkers) {
        if (numOfWantedWorkers > MaxWorkers) {
            int remaining = MaxWorkers - CurrWorkers;
            if (remaining > 0) {
                Ec2Utils.startEc2s("Worker", Scripts.initWorker, remaining);
                CurrWorkers = MaxWorkers;
            }
        } else if (numOfWantedWorkers > CurrWorkers) {
            Ec2Utils.startEc2s("Worker", Scripts.initWorker, numOfWantedWorkers - CurrWorkers);
            CurrWorkers = numOfWantedWorkers;
        }
        if (CurrWorkers >= MaxWorkers) return;
    }
}
