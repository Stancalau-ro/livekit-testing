package ro.stancalau.test.framework.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

/**
 * S3 client wrapper specifically designed for MinIO integration testing. Provides S3-compatible
 * operations for bucket management, object operations, and content validation in test environments.
 */
@Slf4j
public class MinioS3Client {

  private final S3Client s3Client;
  private final String bucketName;

  /**
   * Creates a MinIO S3 client for testing operations.
   *
   * @param endpoint The MinIO server endpoint URL
   * @param accessKey The access key for authentication
   * @param secretKey The secret key for authentication
   * @param bucketName The bucket name to operate on
   */
  public MinioS3Client(String endpoint, String accessKey, String secretKey, String bucketName) {
    this.bucketName = bucketName;

    AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

    this.s3Client =
        S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(Region.US_EAST_1)
            .forcePathStyle(true)
            .build();

    log.info("Created S3 client for endpoint: {} with bucket: {}", endpoint, bucketName);
  }

  public void createBucket() {
    try {
      s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
      log.info("Bucket {} already exists", bucketName);
    } catch (NoSuchBucketException e) {
      s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
      log.info("Created bucket: {}", bucketName);
    }
  }

  public List<String> listObjects(String prefix) {
    ListObjectsV2Request request =
        ListObjectsV2Request.builder().bucket(bucketName).prefix(prefix).build();

    ListObjectsV2Response response = s3Client.listObjectsV2(request);

    return response.contents().stream().map(S3Object::key).collect(Collectors.toList());
  }

  public boolean objectExists(String key) {
    try {
      s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build());
      return true;
    } catch (NoSuchKeyException e) {
      return false;
    }
  }

  public long getObjectSize(String key) {
    HeadObjectResponse response =
        s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build());

    return response.contentLength();
  }

  public void downloadObject(String key, String localPath) throws IOException {
    GetObjectRequest request = GetObjectRequest.builder().bucket(bucketName).key(key).build();

    Path path = Paths.get(localPath);
    Files.createDirectories(path.getParent());

    try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request)) {
      Files.copy(response, path);
      log.info("Downloaded object {} to {}", key, localPath);
    }
  }

  public void uploadObject(String key, byte[] content) {
    PutObjectRequest request = PutObjectRequest.builder().bucket(bucketName).key(key).build();

    s3Client.putObject(request, RequestBody.fromBytes(content));
    log.info("Uploaded object {} to bucket {}", key, bucketName);
  }

  public void deleteObject(String key) {
    DeleteObjectRequest request = DeleteObjectRequest.builder().bucket(bucketName).key(key).build();

    s3Client.deleteObject(request);
    log.info("Deleted object {} from bucket {}", key, bucketName);
  }

  public void close() {
    if (s3Client != null) {
      s3Client.close();
    }
  }

  public void exportBucketContents(String exportDirectory) {
    try {
      List<String> objects = listObjects("");
      File exportDir = new File(exportDirectory, bucketName);
      exportDir.mkdirs();

      for (String key : objects) {
        String localPath = new File(exportDir, key).getAbsolutePath();
        downloadObject(key, localPath);
        log.info("Exported {} from bucket {} to {}", key, bucketName, localPath);
      }

      log.info(
          "Exported {} objects from bucket {} to {}",
          objects.size(),
          bucketName,
          exportDir.getAbsolutePath());
    } catch (Exception e) {
      log.error("Failed to export bucket contents for bucket {}: {}", bucketName, e.getMessage());
    }
  }
}
