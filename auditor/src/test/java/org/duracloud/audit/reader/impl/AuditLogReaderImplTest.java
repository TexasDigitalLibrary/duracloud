/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.audit.reader.impl;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Iterator;

import org.duracloud.audit.AuditConfig;
import org.duracloud.audit.reader.AuditLogEmptyException;
import org.duracloud.error.ContentStoreException;
import org.duracloud.mill.test.AbstractTestBase;
import org.duracloud.storage.error.StorageException;
import org.duracloud.storage.provider.StorageProvider;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Test;
/**
 * 
 * @author Daniel Bernstein
 *         Date: Sept. 17, 2014
 *
 */
public class AuditLogReaderImplTest extends AbstractTestBase {

    private String spaceId  = "space-id";
    private String storeId = "store-id";
    private String account = "account";
    private String globalAuditSpaceId = "global-audit-space-id";


    @Mock
    private StorageProvider storageProvider;
    
    @Mock
    private AuditConfig config;
    
    @Before
    public void setUp() throws Exception {
    }


    @Test
    public void testGetAuditLog() throws IOException, ContentStoreException, AuditLogEmptyException {
        
        String[]  file1Lines = {"a", "b", "c", "d"};
        String[]  file2Lines = {"e", "f", "g", "h"};

        String prefix = getPrefix();
        final StorageProvider storageProvider = createMock(StorageProvider.class);
        String contentId1 = "log1";
        String contentId2 = "log2";
        Iterator<String> it =
            Arrays.asList(new String[] { prefix + "/" + contentId1,
                                        prefix + "/" + contentId2 }).iterator();
        expect(storageProvider.getSpaceContents(eq(globalAuditSpaceId), eq(prefix))).andReturn(it);
        AuditConfig config = createMock(AuditConfig.class);
        expect(config.getLogSpaceId()).andReturn(globalAuditSpaceId );
        
        setupGetContentCall(prefix, storageProvider, contentId1, file1Lines);
        setupGetContentCall(prefix, storageProvider, contentId2, file2Lines);

        replayAll();

        AuditLogReaderImpl auditReader = createAuditLogReader(storageProvider, config);

        InputStream is = auditReader.gitAuditLog(account, storeId, spaceId);
        
        assertNotNull(is);
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        
        String line = reader.readLine();
        
        
        assertEquals(file1Lines[0], line);
        
        int count = 1;
        int totalCount = 1;
        String[] lines = file1Lines;
        while(true){
            line = reader.readLine();
            assertEquals(lines[count], line);
            count++;
            totalCount++;
            
            if(totalCount >= file1Lines.length + file2Lines.length-1){
                break;
            }else if(lines == file1Lines && count >= file1Lines.length){
                count = 1;
                lines = file2Lines;
            }
        }
        
        assertNull(reader.readLine());
        
        assertEquals(file1Lines.length+file2Lines.length-1, totalCount);
    }


    
    @Test
    public void testGetEmptyLog() throws IOException, StorageException {
        
        String prefix = getPrefix();
        Iterator<String> it =
            Arrays.asList(new String[] { }).iterator();
        expect(storageProvider.getSpaceContents(eq(globalAuditSpaceId), eq(prefix))).andReturn(it);
        
        expect(config.getLogSpaceId()).andReturn(globalAuditSpaceId );
        
        replayAll();
        AuditLogReaderImpl auditReader =
            createAuditLogReader(storageProvider, config);

        
        try{
            auditReader.gitAuditLog(account, storeId, spaceId);
            fail("expected to fail with empty log exception");
        }catch(AuditLogEmptyException e){}
    }

    
    @Test
    public void testContentFailure() throws IOException, ContentStoreException {
        
        String[]  file1Lines = {"a","b"};

        String prefix = getPrefix();
        String contentId1 = "log1";
        String contentId2 = "log2";

        Iterator<String> it =
            Arrays.asList(new String[] { prefix + "/" + contentId1,  prefix + "/" + contentId2,
            }).iterator();
        expect(storageProvider.getSpaceContents(eq(globalAuditSpaceId), eq(prefix))).andReturn(it);
        expect(config.getLogSpaceId()).andReturn(globalAuditSpaceId );
        
        setupGetContentCall(prefix, storageProvider, contentId1, file1Lines);
        setupGetContentCallFailure(prefix, storageProvider, contentId2, null);

        replayAll();

        AuditLogReaderImpl auditReader = createAuditLogReader(storageProvider, config);

        try {
            InputStream is = auditReader.gitAuditLog(account, storeId, spaceId);
            sleep();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            assertNotNull(reader.readLine());
            assertNotNull(reader.readLine());
            reader.readLine();
            fail("expected reader exception");
        }catch( AuditLogEmptyException ex){
            fail("did not expect empty log exception");
        }catch(IOException ex){
            //all good.
        }
        
    }


    protected String getPrefix() {
        String prefix = account + "/" + storeId+"/"+spaceId +"/";
        return prefix;
    }


    protected void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setupGetContentCallFailure(String prefix,
                                            StorageProvider storageProvider,
                                            String contentId,
                                            String[] file1Lines) throws IOException, ContentStoreException {
        InputStream is = createMock(InputStream.class);
        expect(is.read(isA(byte[].class), anyInt(), anyInt())).andThrow(new IOException("test"));
        expect(storageProvider.getContent(eq(globalAuditSpaceId),eq(prefix + "/" + contentId))).andReturn(is);
    }


    protected AuditLogReaderImpl
        createAuditLogReader(final StorageProvider storageProvider, AuditConfig config) {
        AuditLogReaderImpl auditReader = new AuditLogReaderImpl(config){
            @Override
            protected StorageProvider getStorageProvider() {
                return storageProvider;
            }
        };
        
        return auditReader;
    }
    
    protected void setupGetContentCall(String prefix, final StorageProvider storageProvider,
                                       String contentId,
                                       String[] fileLines)
        throws IOException,
            FileNotFoundException,
            ContentStoreException {
        File file = File.createTempFile(contentId, "txt");
        file.deleteOnExit();
        FileWriter writer = new FileWriter(file);
        for(String line : fileLines){
            writer.write(line+"\n");
        }
        writer.close();
        
        expect(storageProvider.getContent(eq(globalAuditSpaceId),eq(prefix + "/" + contentId))).andReturn(new FileInputStream(file));
    }

}