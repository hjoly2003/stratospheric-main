#!/bin/sh

# [N]:local]:init_script - The awslocal binary is a wrapper around the real AWS CLI that is part of the LocalStack container. This tiny wrapper is already configured to point to the mocked AWS services inside the container.

# [N]:local]:init_script - Creates an AWS SQS queue. 
awslocal sqs create-queue --queue-name stratospheric-todo-sharing

awslocal ses verify-email-identity --email-address noreply@hjolystratos.net
awslocal ses verify-email-identity --email-address info@hjolystratos.net
awslocal ses verify-email-identity --email-address tom@hjolystratos.net
awslocal ses verify-email-identity --email-address bjoern@hjolystratos.net
awslocal ses verify-email-identity --email-address philip@hjolystratos.net

# [N]:local]:init_script - Creates a dynamo DB
awslocal dynamodb create-table \
    --table-name local-todo-app-breadcrumb \
    --attribute-definitions AttributeName=id,AttributeType=S \
    --key-schema AttributeName=id,KeyType=HASH \
    --provisioned-throughput ReadCapacityUnits=10,WriteCapacityUnits=10 \

echo "Initialized."
