package kr.co.lokit.api.config.file

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@Configuration
@ConditionalOnProperty(name = ["aws.s3.enabled"], havingValue = "true", matchIfMissing = true)
class FileConfig(
    @Value("\${aws.s3.region}") private val region: String,
    @Value("\${aws.s3.bucket}") private val bucket: String,
) {
    @Bean
    fun s3Client(): S3Client = S3Client.builder()
        .region(Region.of(region))
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build()

    @Bean
    fun s3Presigner(): S3Presigner = S3Presigner.builder()
        .region(Region.of(region))
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build()

    @Bean
    fun bucketName(): String = bucket
}
