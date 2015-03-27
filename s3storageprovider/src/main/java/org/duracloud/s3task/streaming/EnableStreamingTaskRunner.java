/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.s3task.streaming;

import com.amazonaws.services.cloudfront.AmazonCloudFrontClient;
import com.amazonaws.services.cloudfront.model.CloudFrontOriginAccessIdentity;
import com.amazonaws.services.cloudfront.model.CloudFrontOriginAccessIdentityConfig;
import com.amazonaws.services.cloudfront.model.CloudFrontOriginAccessIdentitySummary;
import com.amazonaws.services.cloudfront.model.CreateCloudFrontOriginAccessIdentityRequest;
import com.amazonaws.services.cloudfront.model.CreateStreamingDistributionRequest;
import com.amazonaws.services.cloudfront.model.GetCloudFrontOriginAccessIdentityRequest;
import com.amazonaws.services.cloudfront.model.ListCloudFrontOriginAccessIdentitiesRequest;
import com.amazonaws.services.cloudfront.model.S3Origin;
import com.amazonaws.services.cloudfront.model.StreamingDistribution;
import com.amazonaws.services.cloudfront.model.StreamingDistributionConfig;
import com.amazonaws.services.cloudfront.model.StreamingDistributionSummary;
import com.amazonaws.services.cloudfront.model.TrustedSigners;
import com.amazonaws.services.s3.AmazonS3Client;
import org.duracloud.StorageTaskConstants;
import org.duracloud.s3storage.S3StorageProvider;
import org.duracloud.s3storageprovider.dto.EnableStreamingTaskParameters;
import org.duracloud.s3storageprovider.dto.EnableStreamingTaskResult;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author: Bill Branan
 * Date: May 21, 2010
 */
public class EnableStreamingTaskRunner extends BaseStreamingTaskRunner  {

    private final Logger log =
        LoggerFactory.getLogger(EnableStreamingTaskRunner.class);

    private static final String TASK_NAME =
        StorageTaskConstants.ENABLE_STREAMING_TASK_NAME;

    public EnableStreamingTaskRunner(StorageProvider s3Provider,
                                     S3StorageProvider unwrappedS3Provider,
                                     AmazonS3Client s3Client,
                                     AmazonCloudFrontClient cfClient,
                                     String cfAccountId) {
        this.s3Provider = s3Provider;
        this.unwrappedS3Provider = unwrappedS3Provider;
        this.s3Client = s3Client;
        this.cfClient = cfClient;
        this.cfAccountId = cfAccountId;
    }

    public String getName() {
        return TASK_NAME;
    }

    // Enable streaming
    public String performTask(String taskParameters) {
        EnableStreamingTaskParameters taskParams =
            EnableStreamingTaskParameters.deserialize(taskParameters);

        String spaceId = taskParams.getSpaceId();
        boolean secure = taskParams.isSecure();

        log.info("Performing " + TASK_NAME + " task on space " + spaceId +
                 ". Secure streaming set to " + secure);

        // Will throw if bucket does not exist
        String bucketName = unwrappedS3Provider.getBucketName(spaceId);

        String domainName = null;
        String distId = null;
        String oaIdentityId = null;
        EnableStreamingTaskResult taskResult = new EnableStreamingTaskResult();

        try {
            StreamingDistributionSummary existingDist =
                getExistingDistribution(bucketName);

            if(existingDist != null) { // There is an existing distribution
                distId = existingDist.getId();
                if (!existingDist.isEnabled()) { // Distribution is disabled, enable it
                    setDistributionState(distId, true);
                }
                domainName = existingDist.getDomainName();
            } else { // No existing distribution, need to create one
                oaIdentityId = getOriginAccessId();
                S3Origin origin = new S3Origin(bucketName + S3_ORIGIN_SUFFIX,
                                               S3_ORIGIN_OAI_PREFIX + oaIdentityId);
                // Set trusted signers to null if this is not a secure distribution
                TrustedSigners signers = null;
                if(secure) {
                    signers = new TrustedSigners().withItems(cfAccountId)
                                                  .withEnabled(true)
                                                  .withQuantity(1);
                }

                StreamingDistribution dist =
                    cfClient.createStreamingDistribution(
                        new CreateStreamingDistributionRequest(
                            new StreamingDistributionConfig()
                                .withCallerReference(""+System.currentTimeMillis())
                                .withS3Origin(origin)
                                .withEnabled(true)
                                .withComment("Streaming space: " + spaceId)
                                .withTrustedSigners(signers)))
                            .getStreamingDistribution();
                domainName = dist.getDomainName();
            }

            // Set bucket policy to accept origin access identity
            setBucketAccessPolicy(bucketName, oaIdentityId);

            // Update bucket tags to include streaming host
            Map<String, String> spaceProps =
                s3Provider.getSpaceProperties(spaceId);
            spaceProps.put(STREAMING_HOST_PROP, domainName);
            unwrappedS3Provider.setNewSpaceProperties(spaceId, spaceProps);

            taskResult.setResult("Enable Streaming Task completed successfully");
        } catch(Exception e) {
            log.warn("Error encountered running " + TASK_NAME + " task: " +
                     e.getMessage(), e);
            taskResult.setResult("Enable Streaming Task failed due to: " +
                                 e.getMessage());
        }

        // Return results
        taskResult.setStreamingHost(domainName);
        String toReturn = taskResult.serialize();
        log.info("Result of " + TASK_NAME + " task: " + toReturn);
        return toReturn;
    }

    /*
     * Retrieves an origin access ID, which may be either pre-existing or new
     */
    private String getOriginAccessId() {
        String oaId = getExistingOriginAccessId();
        if(oaId != null) { // Use existing ID
            return oaId;
        } else { // Create a new ID
            return cfClient.createCloudFrontOriginAccessIdentity(
                    new CreateCloudFrontOriginAccessIdentityRequest(
                        new CloudFrontOriginAccessIdentityConfig()
                            .withComment("DuraCloud Origin Access ID")))
                        .getCloudFrontOriginAccessIdentity().getId();
        }
    }

    /*
     * Attempts to get an existing origin access ID
     */
    private String getExistingOriginAccessId() {
        List<CloudFrontOriginAccessIdentitySummary> oaiList =
            cfClient.listCloudFrontOriginAccessIdentities(
                new ListCloudFrontOriginAccessIdentitiesRequest())
                    .getCloudFrontOriginAccessIdentityList().getItems();
        if(oaiList != null && oaiList.size() > 0) {
            return oaiList.iterator().next().getId();
        }
        return null;
    }

    /*
     * Updates the bucket policy to allow GET access to the cloudfront origin
     * access identity. This allows Cloudfront to access content in S3
     *
     * @return results of the ACL setting activity
     */
    private void setBucketAccessPolicy(String bucketName, String oaIdentityId) {
        CloudFrontOriginAccessIdentity cfOAIdentity =
            cfClient.getCloudFrontOriginAccessIdentity(
                new GetCloudFrontOriginAccessIdentityRequest(oaIdentityId))
                    .getCloudFrontOriginAccessIdentity();
        String s3UserId = cfOAIdentity.getS3CanonicalUserId();

        StringBuilder policyText = new StringBuilder();
        policyText.append("{\"Version\":\"2012-10-17\",");
	    policyText.append("\"Id\":\"PolicyForCloudFrontPrivateContent\",");
	    policyText.append("\"Statement\":[{");
        policyText.append("\"Sid\":\"Grant CloudFront access to private content\",");
		policyText.append("\"Effect\":\"Allow\",");
		policyText.append("\"Principal\":{\"CanonicalUser\":\"" + s3UserId + "\"},");
        policyText.append("\"Action\":\"s3:GetObject\",");
        policyText.append("\"Resource\":\"arn:aws:s3:::" + bucketName + "/*\"");
        policyText.append("}]}");
        s3Client.setBucketPolicy(bucketName, policyText.toString());
    }

}
