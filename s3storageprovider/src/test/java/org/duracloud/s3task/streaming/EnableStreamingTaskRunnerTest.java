/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.s3task.streaming;

import com.amazonaws.services.cloudfront.AmazonCloudFrontClient;
import com.amazonaws.services.s3.AmazonS3Client;
import org.duracloud.s3storage.S3StorageProvider;
import org.duracloud.storage.provider.StorageProvider;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author: Bill Branan
 * Date: Jun 3, 2010
 */
public class EnableStreamingTaskRunnerTest extends StreamingTaskRunnerTestBase {

    protected EnableStreamingTaskRunner createRunner(StorageProvider s3Provider,
                                                     S3StorageProvider unwrappedS3Provider,
                                                     AmazonS3Client s3Client,
                                                     AmazonCloudFrontClient cfClient,
                                                     String cfAccountId) {
        this.s3Provider = s3Provider;
        this.unwrappedS3Provider = unwrappedS3Provider;
        this.s3Client = s3Client;
        this.cfClient = cfClient;
        this.cfAccountId = cfAccountId;
        return new EnableStreamingTaskRunner(s3Provider, unwrappedS3Provider,
                                             s3Client, cfClient, cfAccountId);
    }

    @Test
    public void testGetName() throws Exception {
        EnableStreamingTaskRunner runner =
            createRunner(createMockStorageProvider(),
                         createMockUnwrappedS3StorageProvider(),
                         createMockS3ClientV1(),
                         createMockCFClientV1(),
                         cfAccountId);

        String name = runner.getName();
        assertEquals("enable-streaming", name);
    }

//    /*
//     * Testing the case where no streaming distribution exists for the given
//     * bucket and no origin access id exists. Both should be created.
//     */
//    @Test
//    public void testPerformTask1() throws Exception {
//        EnableStreamingTaskRunner runner =
//            createRunner(createMockStorageProviderV2(false),
//                         createMockUnwrappedS3StorageProviderV2(),
//                         createMockS3ClientV3(),
//                         createMockCFServiceV3(),
//                         cfAccountId);
//
//        try {
//            runner.performTask(null);
//            fail("Exception expected");
//        } catch(Exception expected) {
//            assertNotNull(expected);
//        }
//
//        EnableStreamingTaskParameters taskParams = new EnableStreamingTaskParameters();
//        taskParams.setSpaceId(spaceId);
//        taskParams.setSecure(secure);
//
//        String results = runner.performTask(taskParams.serialize());
//        assertNotNull(results);
//        testResults(results);
//        testCapturedProps();
//    }
//
//    /*
//     * For testing the case where a distribution and origin access id do not
//     * exist and are created.
//     * In short, these are the calls that are expected:
//     *
//     * createDistribution (1) - returns valid dist
//     * createOriginAccessIdentity (1) - returns valid oaid
//     * getOriginAccessIdentityList (1) - returns null (or empty list)
//     * getOriginAccessIdentity (1) - returns valid oaid
//     * listStreamingDistributions (1) - returns null
//     */
//    private CloudFrontService createMockCFServiceV3() throws Exception {
//        CloudFrontService service =
//            EasyMock.createMock(CloudFrontService.class);
//
//        Map<String, String> signers = new HashMap<>();
//        Distribution dist =
//            new StreamingDistribution("id", "status", null, domainName,
//                                      signers, null);
//
//        EasyMock
//            .expect(service.createDistribution(
//                EasyMock.isA(StreamingDistributionConfig.class)))
//            .andReturn(dist)
//            .times(1);
//
//        OriginAccessIdentity oaIdentity =
//            new OriginAccessIdentity("id", "s3CanonicalUserId", "comment");
//
//        EasyMock
//            .expect(service.createOriginAccessIdentity(
//                EasyMock.<String>isNull(),
//                EasyMock.isA(String.class)))
//            .andReturn(oaIdentity)
//            .times(1);
//
//        EasyMock
//            .expect(service.getOriginAccessIdentityList())
//            .andReturn(null)
//            .times(1);
//
//        EasyMock
//            .expect(service.getOriginAccessIdentity(EasyMock.isA(String.class)))
//            .andReturn(oaIdentity)
//            .times(1);
//
//        EasyMock
//            .expect(service.listStreamingDistributions())
//            .andReturn(null)
//            .times(1);
//
//        EasyMock.replay(service);
//        return service;
//    }
//
//    /*
//     * Testing the case where a streaming distribution exists for the given
//     * bucket and an origin access id exists. Nothing should be created.
//     */
//    @Test
//    public void testPerformTask2() throws Exception {
//        EnableStreamingTaskRunner runner =
//            createRunner(createMockStorageProviderV2(false),
//                         createMockUnwrappedS3StorageProviderV2(),
//                         createMockS3ClientV3(),
//                         createMockCFServiceV2(),
//                         cfAccountId);
//
//        EnableStreamingTaskParameters taskParams = new EnableStreamingTaskParameters();
//        taskParams.setSpaceId(spaceId);
//        taskParams.setSecure(secure);
//
//        String results = runner.performTask(taskParams.serialize());
//        assertNotNull(results);
//        testResults(results);
//        testCapturedProps();
//    }
//
//    protected AmazonS3Client createMockS3ClientV3() throws Exception {
//        AmazonS3Client service = EasyMock.createMock(AmazonS3Client.class);
//
//        service.setBucketPolicy(EasyMock.isA(String.class),
//                                EasyMock.isA(String.class));
//        EasyMock.expectLastCall();
//
//        EasyMock.replay(service);
//        return service;
//    }
//
//    private void testResults(String results) {
//        EnableStreamingTaskResult taskResult =
//            EnableStreamingTaskResult.deserialize(results);
//        assertEquals(taskResult.getStreamingHost(), domainName);
//        assertTrue(taskResult.getResult().contains("completed"));
//    }
//
//    private void testCapturedProps() {
//        Map<String, String> spaceProps = spacePropsCapture.getValue();
//        String propName = EnableStreamingTaskRunner.STREAMING_HOST_PROP;
//        assertEquals(spaceProps.get(propName), domainName);
//    }
//
//    /*
//     * For testing the case where a distribution and origin access identity
//     * already exist and are used as is.
//     * In short, these are the calls that are expected:
//     *
//     * getStreamingDistributionConfig (1) - returns valid config (includes oaid, enabled)
//     * getOriginAccessIdentity (1) - returns valid oaid
//     * listStreamingDistributions (1) - returns a list with a valid dist (matching bucket name)
//     */
//    protected CloudFrontService createMockCFServiceV2() throws Exception {
//        CloudFrontService service =
//            EasyMock.createMock(CloudFrontService.class);
//
//        S3Origin origin = new S3Origin("origin", "originAccessId");
//        StreamingDistributionConfig config =
//            new StreamingDistributionConfig(origin, "callerReference",
//                                            new String[0], "comment", true,
//                                            null, false, null, null);
//
//        EasyMock
//            .expect(service.getStreamingDistributionConfig(
//                EasyMock.isA(String.class)))
//            .andReturn(config)
//            .times(1);
//
//        OriginAccessIdentity oaIdentity =
//            new OriginAccessIdentity("id", "s3CanonicalUserId", "comment");
//
//        EasyMock
//            .expect(service.getOriginAccessIdentity(EasyMock.isA(String.class)))
//            .andReturn(oaIdentity)
//            .times(1);
//
//        S3Origin origin2 = new S3Origin(bucketName);
//        StreamingDistribution dist =
//            new StreamingDistribution("id", "status", null, domainName,
//                                      origin2, null, "comment", true);
//        StreamingDistribution[] distributions = {dist};
//
//        EasyMock.expect(service.listStreamingDistributions())
//                .andReturn(distributions)
//                .times(1);
//        EasyMock.expect(service.getStreamingDistributionInfo(EasyMock.anyString()))
//                .andReturn(dist)
//                .times(1);
//
//        EasyMock.replay(service);
//        return service;
//    }

}
