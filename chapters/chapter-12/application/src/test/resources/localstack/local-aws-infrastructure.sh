#!/bin/sh

# [N]:local]:init_script - The awslocal binary is a wrapper around the real AWS CLI that is part of the LocalStack container. This tiny wrapper is already configured to point to the mocked AWS services inside the container.

# [N]:local]:init_script - Creates an AWS SQS queue. 
awslocal sqs create-queue --queue-name stratospheric-todo-sharing

awslocal ses verify-email-identity --email-address noreply@stratospheric.dev
awslocal ses verify-email-identity --email-address info@stratospheric.dev
awslocal ses verify-email-identity --email-address tom@stratospheric.dev
awslocal ses verify-email-identity --email-address bjoern@stratospheric.dev
awslocal ses verify-email-identity --email-address philip@stratospheric.dev

echo "Initialized."
