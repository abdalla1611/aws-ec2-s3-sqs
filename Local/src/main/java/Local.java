import com.google.gson.Gson;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetBucketWebsiteRequest;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

public class Local {
    final static String managerToLocalReplyQueue = Defs.managerToLocalQueue + LocalDate.now();
    final static String managerToLocalReplyBucket = Defs.localToManagerBucket + LocalDate.now() + Math.random() * 1000;

    public static void main(String[] args) throws InterruptedException, IOException {
        System.out.println("Local started");
        Ec2Utils.startEc2IfNotExist("Manager", Scripts.initManager);
        //created a manager.
        int filesperworker = Integer.parseInt(args[1]);
        boolean terminate = false;
        if (args[2].equalsIgnoreCase("terminate")) {
            terminate = true;
        }
        SqsUtils.createSqsQueue(Defs.localToManagerQueue);
        if (!S3Utils.checkIfBucketExistsAndHasAccessToIt(managerToLocalReplyBucket))
            S3Utils.createBucket(managerToLocalReplyBucket);
        if (!S3Utils.checkIfBucketExistsAndHasAccessToIt(Defs.awsoutputbucket)) //TODO make this bucket in the manager, in case of multiple locals this won't work
            S3Utils.createBucket(Defs.awsoutputbucket);
        SqsUtils.createSqsQueue(managerToLocalReplyQueue);

        Thread.sleep(5 * 1000);
        //------------------------------- Now lets send the file ----------------------------------
        S3Utils.putObject(Paths.get(args[0]), "MyInput.txt", managerToLocalReplyBucket); // send the file.
        SqsMessage sqsMessage = new SqsMessage(managerToLocalReplyQueue, managerToLocalReplyBucket, terminate);
        sqsMessage.s3ReplyBucket = managerToLocalReplyBucket;
        sqsMessage.numOfMessagesPerWorker = filesperworker;
        SqsUtils.sendMessage(sqsMessage, SqsUtils.getQueueUrl(Defs.localToManagerQueue)); //Letting the manager know where we put the file
        Files.deleteIfExists(Paths.get(args[1])); // getObj will not work if the file still exists
        System.out.println("Done uploading the file");
        ResponseInputStream stream = null;
        BufferedReader bufferedReader = null;
        FileWriter fileWriter;
        Writer writer;
        while (true) {
            try {
                List<Message> messages = SqsUtils.receiveMessages(managerToLocalReplyQueue);
                if (messages.size() != 0) {
                    System.out.println("we got a reply message from the manager !");
                    //No need for the message, its only to inform us we are done (its from the managerTask)
                    SqsUtils.deleteMessage(messages.get(0), managerToLocalReplyQueue);
                    stream = S3Utils.getObjectS3("WorkersOutputFile.txt", managerToLocalReplyBucket);
                    bufferedReader = new BufferedReader(new InputStreamReader(stream));
                    fileWriter = new FileWriter("Output.html");
                    writer = new BufferedWriter(fileWriter);
                    writer.write("<!DOCTYPE html>");
                    writer.write("<html>");
                    writer.write("<body>");
                    String Line;
                    while ((Line = bufferedReader.readLine()) != null) {
                        writer.write("<p>" + Line + "</p>");
                        writer.write('\n');
                    }
                    writer.write("</body>");
                    writer.write("</html>");
                    writer.close();
                    SqsUtils.deleteQueue(managerToLocalReplyQueue);
                    S3Utils.deleteBucket(managerToLocalReplyBucket);
                    S3Utils.deleteBucket(Defs.awsoutputbucket);
                    return;
                }
            } catch (SdkClientException ignored) {
                ;
            }
        }
    }
}
