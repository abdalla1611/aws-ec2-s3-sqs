import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class S3Utils {
    public static final S3Client s3 = S3Client.builder().region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(Credentials.getCredentials())).build();

    public static void createBucket(String bucket) {
        s3.createBucket(CreateBucketRequest
                .builder()
                .bucket(bucket)
                .createBucketConfiguration(
                        CreateBucketConfiguration.builder()
                                .build())
                .build());

        System.out.println("Created bucket:"+bucket);
    }
    public static void deleteBucket(String bucket) {
        listBucketObjects(s3,bucket); // delete all objects in the bucket !!
        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucket).build();
        s3.deleteBucket(deleteBucketRequest);
    }
    public static void listBucketObjects(S3Client s3, String bucketName ) {

        try {
            ListObjectsRequest listObjects = ListObjectsRequest
                    .builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsResponse res = s3.listObjects(listObjects);
            List<S3Object> objects = res.contents();

            for (S3Object myValue : objects) {
                deleteBucketObjects(s3,bucketName,myValue.key());
            }

        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }
    public static void deleteBucketObjects(S3Client s3, String bucketName, String objectName) {

        ArrayList<ObjectIdentifier> toDelete = new ArrayList<ObjectIdentifier>();
        toDelete.add(ObjectIdentifier.builder().key(objectName).build());

        try {
            DeleteObjectsRequest dor = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(Delete.builder().objects(toDelete).build())
                    .build();
            s3.deleteObjects(dor);
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

    public static boolean checkIfBucketExistsAndHasAccessToIt(String bucket)
    {
        HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                .bucket(bucket)
                .build();

        try {
            s3.headBucket(headBucketRequest);
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        }
    }
    public static void putObject(String data, String key, String bucketName)
    {
        s3.putObject(PutObjectRequest.builder().key(key).bucket(bucketName).build()
        , RequestBody.fromBytes(data.getBytes(StandardCharsets.UTF_8)));
    }
    public static void putObject(Path path, String key, String bucketName)
    {
        s3.putObject(PutObjectRequest.builder().key(key).bucket(bucketName).build(),
                RequestBody.fromFile(path));
    }
    public static String getObject(String key, String bucketName)
    {
        BufferedReader breader;
        ResponseInputStream<GetObjectResponse> s3Obj = s3.getObject(GetObjectRequest.builder().key(key).bucket(bucketName).build());
        breader = new BufferedReader(new InputStreamReader(s3Obj));
        String line;
        StringBuilder ret = new StringBuilder();
        try {

            while ((line = breader.readLine()) != null) {
                ret.append(line).append("\n");
            }
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
        return ret.toString();
    }
    public static ResponseInputStream getObjectS3(String key, String bucketName)
    {
        return s3.getObject(GetObjectRequest.builder().key(key).bucket(bucketName).build());
    }
    public static void deleteObject(String key, String bucketName)
    {
        s3.deleteObject(DeleteObjectRequest.builder().key(key).bucket(bucketName).build());
    }
}
