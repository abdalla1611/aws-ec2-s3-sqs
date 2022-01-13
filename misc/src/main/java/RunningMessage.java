import software.amazon.awssdk.services.sqs.model.Message;

public class RunningMessage implements Runnable {
    private Message message;
    public volatile boolean terminate = false;
    private String queueName;

    public RunningMessage(Message msg, String queueName) {
        this.queueName = queueName;
        this.message = msg;

    }

    @Override
    public void run() {
        while (true) {
            if (terminate) {
                SqsUtils.deleteMessage(message, queueName);
                break;
            } else {
                extendMessageTime(this.message, SqsUtils.getQueueUrl(queueName), 15);
            }
            try {
                Thread.sleep(10 * 1000);
            } catch (InterruptedException e) {
                System.out.println(e);
            }
        }
    }

    public static void extendMessageTime(Message msg, String queueUrl, Integer newVisibilityTimeout) {
        SqsUtils.changeVisibilityTime(queueUrl, msg, newVisibilityTimeout);
    }

}
