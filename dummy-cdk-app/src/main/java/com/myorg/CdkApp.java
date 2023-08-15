package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcProps;

public class CdkApp {
  public static void main(final String[] args) {
    App app = new App();

    new CdkStack(app, "CdkStack", StackProps.builder()
      .env(Environment.builder()
        .account("254857894179")
        .region("us-east-1")
        .build())
      .build());

    app.synth();
  }
}

