#!/bin/sh

# [N]:local]:init_script - The awslocal binary is a wrapper around the real AWS CLI that is part of the LocalStack container. This tiny wrapper is already configured to point to the mocked AWS services inside the container.

# [N]:local]:sqs - Creates an AWS SQS queue. 
awslocal sqs create-queue --queue-name stratospheric-todo-sharing

# [N]:local]:ses - Verifies several email identities for Amazon SES
awslocal ses verify-email-identity --email-address noreply@hjolystratos.net
awslocal ses verify-email-identity --email-address info@hjolystratos.net
awslocal ses verify-email-identity --email-address tom@hjolystratos.net
awslocal ses verify-email-identity --email-address bjoern@hjolystratos.net
awslocal ses verify-email-identity --email-address philip@hjolystratos.net

echo "Initialized."
