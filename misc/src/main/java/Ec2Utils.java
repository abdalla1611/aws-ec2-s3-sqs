import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;


import java.util.List;

public class Ec2Utils {
    public static Ec2Client ec2 = Ec2Client.builder().region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(Credentials.getCredentials()))
            .build();

    public static void startEc2s(String name, String init, int amount) {
        System.out.println("Creating Ec2 Instance: " + name);
        List<Instance> instances = ec2.runInstances(RunInstancesRequest.builder()
                .maxCount(amount)
                .minCount(amount)
                .imageId("ami-00e95a9222311e8ed")
                .securityGroupIds("sg-091c716754e13a2d0")
                .keyName("mykey")
                .userData(Base64.encode(init.getBytes()))
                .instanceType(InstanceType.T1_MICRO)
                .build()).instances();
        Tag tag = Tag.builder().key("Job").value(name).build();
        for (Instance instance_ : instances) {
            if (!instance_.state().name().toString().equals("terminated")) {
                CreateTagsRequest tagRequest = CreateTagsRequest.builder()
                        .resources(instance_.instanceId())
                        .tags(tag)
                        .build();
                ec2.createTags(tagRequest);
            }
        }
    }

    public static void startEc2s(String name, String init) {
        startEc2s(name, init, 1);
    }

    /*
    Will not start if there exists one
     */
    public static void startEc2IfNotExist(String name, String init) {
        String nextToken = null;
        do {
            DescribeInstancesRequest request = DescribeInstancesRequest.builder().nextToken(nextToken)
                    .build();
            DescribeInstancesResponse response = ec2.describeInstances(request);
            if (response.reservations().size() == 0) {
                startEc2s(name, init);
                return;
            }
            for (Reservation reservation : response.reservations()) {
                if (reservation.instances().size() == 0) {
                    startEc2s(name, init);
                    return;
                }
                for (Instance instance : reservation.instances()) {
                    if (reservation.instances().size() == 0) {
                        startEc2s(name, init);
                        return;
                    }
                    if (instance.tags().size() != 0 && (instance.state().name().toString().equalsIgnoreCase("running") || instance.state().name().toString().equalsIgnoreCase("pending")) && instance.tags().get(0).value().equals(name)) {
                        System.out.println("Found manager, no need to start");
                        return;
                    }
                }
            }
            nextToken = response.nextToken();
        } while (nextToken != null);
        startEc2s(name, init);
    }

    public static void terminateAll() {
        String nextToken = null;

        try {

            do {
                DescribeInstancesRequest request = DescribeInstancesRequest.builder().nextToken(nextToken)
                        .build();
                DescribeInstancesResponse response = ec2.describeInstances(request);

                for (Reservation reservation : response.reservations()) {
                    for (Instance instance : reservation.instances()) {
                        if (!instance.state().name().toString().equals("terminated")) {
                            ec2.terminateInstances(TerminateInstancesRequest.builder().instanceIds(instance.instanceId()).build());
                            System.out.println("TERMINATED INSTANCE:" + instance.tags().get(0).value());
                        }
                    }
                }
                nextToken = response.nextToken();
            } while (nextToken != null);

        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            return;
        }
    }

    public static void terminateAll(String tag) {
        String nextToken = null;

        try {

            do {
                DescribeInstancesRequest request = DescribeInstancesRequest.builder().nextToken(nextToken)
                        .build();
                DescribeInstancesResponse response = ec2.describeInstances(request);

                for (Reservation reservation : response.reservations()) {
                    for (Instance instance : reservation.instances()) {
                        if (instance.tags().size() != 0)
                            if ((!instance.state().name().toString().equals("terminated")) && instance.tags().get(0).value().equals(tag)) {
                                ec2.terminateInstances(TerminateInstancesRequest.builder().instanceIds(instance.instanceId()).build());
                                System.out.println("TERMINATED INSTANCE:" + instance.tags().get(0).value());
                            }
                    }
                }
                nextToken = response.nextToken();
            } while (nextToken != null);

        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            return;
        }
    }

    public static void terminateEc2(String instanceId) {
        ec2.terminateInstances(TerminateInstancesRequest.builder().instanceIds(instanceId).build());
        AwsSessionCredentials a = AwsSessionCredentials.create("A", "A", "A");
        StaticCredentialsProvider f = StaticCredentialsProvider.create(a);
    }
}
