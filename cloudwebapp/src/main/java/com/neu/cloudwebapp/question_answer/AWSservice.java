package com.neu.cloudwebapp.question_answer;

import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AWSservice {

    @Value("${aws.region:us-east-1}")
    String region;

    @Value("${aws.profile:#{null}}")
    String profile;

    @Bean
    public AmazonSNS amazonSNS() {
        AWSCredentialsProviderChain awsCredentialsProviderChain = new AWSCredentialsProviderChain(
                new InstanceProfileCredentialsProvider(true),
                new ProfileCredentialsProvider(profile)
        );
        return AmazonSNSClientBuilder.standard()
                .withCredentials(awsCredentialsProviderChain)
                .withRegion(region)
                .build();
    }
}
