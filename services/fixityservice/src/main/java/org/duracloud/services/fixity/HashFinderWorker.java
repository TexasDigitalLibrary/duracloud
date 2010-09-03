/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.services.fixity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.duracloud.client.ContentStore;
import org.duracloud.client.StoreCaller;
import org.duracloud.common.error.DuraCloudRuntimeException;
import org.duracloud.common.util.ChecksumUtil;
import org.duracloud.common.util.ChecksumUtil.Algorithm;
import org.duracloud.domain.Content;
import org.duracloud.error.ContentStoreException;
import org.duracloud.services.fixity.domain.ContentLocation;
import org.duracloud.services.fixity.domain.FixityServiceOptions;
import org.duracloud.services.fixity.results.HashFinderResult;
import org.duracloud.services.fixity.results.ServiceResultListener;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class finds the hash associated with a single content-item.
 * It is able to find/generate hashes in one of three modes:
 * - hash already stored in metadata
 * - hash generated by streaming content
 * - hash generated by streaming content with additional salt
 *
 * @author Andrew Woods
 *         Date: Aug 5, 2010
 */
public class HashFinderWorker implements Runnable {

    private final Logger log = LoggerFactory.getLogger(HashFinderWorker.class);

    private FixityServiceOptions serviceOptions;
    private ContentStore contentStore;
    private ContentLocation workitemLocation;
    private ServiceResultListener resultListener;

    private ChecksumUtil checksumUtil = new ChecksumUtil(Algorithm.MD5);

    public HashFinderWorker(FixityServiceOptions serviceOptions,
                            ContentStore contentStore,
                            ContentLocation workitemLocation,
                            ServiceResultListener resultListener) {
        this.serviceOptions = serviceOptions;
        this.contentStore = contentStore;
        this.workitemLocation = workitemLocation;
        this.resultListener = resultListener;
    }

    @Override
    public void run() {
        String hash = null;
        try {
            hash = getHash();
            sendResult(true, getSpaceId(), getContentId(), hash);

        } catch (Exception e) {
            sendResult(false, getSpaceId(), getContentId(), e.getMessage());
        }
    }

    private void sendResult(boolean success,
                            String spaceId,
                            String contentId,
                            String hash) {
        HashFinderResult result = new HashFinderResult(success,
                                                       spaceId,
                                                       contentId,
                                                       hash);
        log.debug("Sending result: " + result);
        resultListener.processServiceResult(result);
    }

    private String getHash() {
        FixityServiceOptions.HashApproach hashApproach = serviceOptions.getHashApproach();
        if (null == hashApproach) {
            throwRuntime("hash-approach is null");
        }

        String hash = null;
        switch (hashApproach) {
            case STORED: {
                hash = getStoredHash();
                break;
            }
            case GENERATED: {
                hash = getGeneratedHash();
                break;
            }
            case SALTED: {
                hash = getSaltedHash();
                break;
            }
            default: {
                throwRuntime("Unexpected hashApproach: " + hashApproach.name());
            }
        }

        return hash;
    }

    private String getStoredHash() {
        Map<String, String> metadata = getContentMetadata();
        if (null == metadata) {
            throwRuntime("metadata is null");
        }

        String hash = metadata.get(StorageProvider.METADATA_CONTENT_MD5);
        if (null == hash) {
            hash = metadata.get(StorageProvider.METADATA_CONTENT_CHECKSUM);
        }
        if (null == hash) {
            throwRuntime("hash metadata element not found");
        }
        return hash;
    }

    /**
     * This method leverages the StoreCaller abstract class to loop on failed
     * contentStore calls.
     *
     * @return
     */
    private Map<String, String> getContentMetadata() {
        StoreCaller<Map<String, String>> caller = new StoreCaller<Map<String, String>>() {
            protected Map<String, String> doCall()
                throws ContentStoreException {
                return contentStore.getContentMetadata(getSpaceId(),
                                                       getContentId());
            }

            public String getLogMessage() {
                return "Error calling contentStore.getContentMetadata() for: " +
                    getSpaceId() + "/" + getContentId();
            }
        };
        return caller.call();
    }

    private String getGeneratedHash() {
        Content content = getContent();
        if (null == content) {
            throwRuntime("content is null");
        }

        InputStream contentStream = content.getStream();
        if (null == contentStream) {
            throwRuntime("contentStream is null");
        }

        String hash = checksumUtil.generateChecksum(contentStream);
        if (null == hash) {
            closeQuietly(contentStream);
            throwRuntime("generated hash is null");
        }

        closeQuietly(contentStream);
        return hash;
    }

    private String getSaltedHash() {
        Content content = getContent();
        if (null == content) {
            throwRuntime("content is null");
        }
        log.debug("contentId: '" + content.getId() + "'");

        InputStream contentStream = content.getStream();
        if (null == contentStream) {
            throwRuntime("contentStream is null");
        }

        String salt = serviceOptions.getSalt();
        if (null == salt) {
            closeQuietly(contentStream);
            throwRuntime("salt is null");
        }
        log.debug("salt: '" + salt + "'");

        InputStream saltStream = new ByteArrayInputStream(salt.getBytes());
        InputStream saltedContentStream = new SequenceInputStream(contentStream,
                                                                  saltStream);

        String hash = checksumUtil.generateChecksum(saltedContentStream);
        if (null == hash) {
            closeQuietly(contentStream, saltStream, saltedContentStream);
            throwRuntime("salted hash is null");
        }
        log.debug("hash: '" + hash + "'");

        closeQuietly(contentStream, saltStream, saltedContentStream);
        return hash;
    }

    /**
     * This method leverages the StoreCaller abstract class to loop on failed
     * contentStore calls.
     *
     * @return
     */
    private Content getContent() {
        StoreCaller<Content> caller = new StoreCaller<Content>() {
            protected Content doCall() throws ContentStoreException {
                return contentStore.getContent(getSpaceId(), getContentId());
            }

            public String getLogMessage() {
                return "Error calling contentStore.getContent() for: " +
                    getSpaceId() + "/" + getContentId();
            }
        };
        return caller.call();
    }

    private String getContentId() {
        return workitemLocation.getContentId();
    }

    private String getSpaceId() {
        return workitemLocation.getSpaceId();
    }

    private void closeQuietly(InputStream... streams) {
        for (InputStream stream : streams) {
            IOUtils.closeQuietly(stream);
        }
    }

    private void throwRuntime(String msg) {
        StringBuilder sb = new StringBuilder("Error: ");
        sb.append(msg);
        sb.append(", for: ");
        sb.append(getSpaceId());
        sb.append("/");
        sb.append(getContentId());
        log.error(sb.toString());
        throw new DuraCloudRuntimeException(sb.toString());
    }

}
