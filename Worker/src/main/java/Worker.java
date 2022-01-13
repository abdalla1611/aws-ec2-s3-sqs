import com.google.gson.Gson;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class Worker {

    public static String PdfConvertion(String op,String inputurl)
    {
        String outputLine ;
        String outputUrl="" ; //change
        String FileName = "";
        try {
            FileName = pdf.Convert(op,inputurl);
            S3Utils.putObject(Paths.get(FileName),FileName,Defs.awsoutputbucket);
            outputUrl = "https://myawscodebucket.s3.amazonaws.com/" + FileName;
            outputLine = op + ": " + inputurl + " " + outputUrl +'\n';
        } catch (IOException e) {
            outputLine = op + ": " + inputurl + " this Url is broken or the file isn't of type pdf" + '\n' ;
        }
        System.out.println("conversion is done filename = "+FileName);
        return outputLine ;

    }

    public static void main(String[] args) throws InterruptedException {
        RunningMessage rm = null;
        Thread keeprunning = null;
        while (true) {
            System.out.println("worker is trying to take a message from the manager");
            List<Message> messages = SqsUtils.receiveMessages(Defs.managerToWorkersQueue, 1);
            if (messages != null && messages.size() > 0) {
                try {
                    System.out.println("worker is working on msg.body = " + messages.get(0).body());
                    // we keep the message until we are sure that its done.
                    rm = new RunningMessage(messages.get(0), Defs.managerToWorkersQueue);
                    keeprunning = new Thread(rm);
                    keeprunning.start();
                    // receive the message and try to execute its op.
                    String sqsstrmsg = messages.get(0).body();
                    SqsMessage msg = new Gson().fromJson(sqsstrmsg, SqsMessage.class);
                    Mission m = new Gson().fromJson(msg.body, Mission.class);
                    String answer = PdfConvertion(m.op,m.URL) ; // here we do the pdf conversion (Execute the op)
                    SqsMessage reply = new SqsMessage("", answer, false); // we don't need a sqs queue cuz no reply needed.
                    SqsUtils.sendMessage(reply, SqsUtils.getQueueUrl(msg.sqsReplyQueue));
                    rm.terminate = true; // here the thread removes the message automatically.
                } catch (Exception e) {
                    System.out.println("OOPS");
                    if (keeprunning != null)
                        keeprunning.interrupt();
                }
            }else {
                Thread.sleep(5*1000);
            }
        }
    }
}
