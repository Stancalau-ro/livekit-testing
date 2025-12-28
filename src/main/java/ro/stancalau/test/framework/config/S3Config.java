package ro.stancalau.test.framework.config;

import lombok.Getter;

@Getter
public class S3Config {

  public final String endpoint;
  public final String accessKey;
  public final String secretKey;
  public final String bucket;
  public final String region;

  public S3Config(String endpoint, String accessKey, String secretKey, String bucket) {
    this.endpoint = endpoint;
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.bucket = bucket;
    this.region = "us-east-1";
  }

  public S3Config(
      String endpoint, String accessKey, String secretKey, String bucket, String region) {
    this.endpoint = endpoint;
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.bucket = bucket;
    this.region = region != null ? region : "us-east-1";
  }
}
