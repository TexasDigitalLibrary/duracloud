/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.appconfig.domain;

import org.duracloud.appconfig.ApplicationInitializer;
import org.duracloud.common.model.Credential;
import org.duracloud.common.model.DuraCloudUserType;
import org.duracloud.common.web.RestHttpHelper;
import org.duracloud.storage.domain.StorageProviderType;
import org.duracloud.unittestdb.domain.ResourceType;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * This test class is placed in the 'domain' package here to have access to
 * the 'protected' data fields of the various config classes.
 *
 * @author Andrew Woods
 *         Date: Apr 22, 2010
 */
public class TestApplicationInitializer extends BaseTestUtil {

    private static File propsFile;
    private static File incompletePropsFile;

    @BeforeClass
    public static void beforeClass() throws IOException {
        Properties props = new Properties();
        Properties appProps = createAppProps();
        Properties storeProps = createDurastoreProps();
        Properties serviceProps = createDuraserviceProps();
        Properties adminProps = createDuradminProps();
        Properties securityProps = createSecurityProps();

        props.putAll(appProps);
        props.putAll(storeProps);
        props.putAll(serviceProps);
        props.putAll(adminProps);
        props.putAll(securityProps);

        propsFile = File.createTempFile("app-init-full-", ".props");
        FileOutputStream output = new FileOutputStream(propsFile);
        props.store(output, "no comment");
        output.close();

        Properties incompleteProps = createAppProps();
        incompletePropsFile = File.createTempFile("app-init-bad-", ".props");
        FileOutputStream outputBad = new FileOutputStream(incompletePropsFile);
        incompleteProps.store(outputBad, "no comment");
        outputBad.close();
    }

    private static Properties createAppProps() {
        Properties props = new Properties();

        String dot = ".";
        String prefix = ApplicationInitializer.QUALIFIER + dot;
        String pWild = prefix + "*" + dot;
        String pAdmin = prefix + DuradminConfig.QUALIFIER + dot;
        String pStore = prefix + DurastoreConfig.QUALIFIER + dot;
        String pService = prefix + DuraserviceConfig.QUALIFIER + dot;

        props.put(pWild + DuraserviceConfig.hostKey, "localhost");
        props.put(pWild + DuraserviceConfig.portKey, BaseTestUtil.getPort());
        props.put(pAdmin + DuraserviceConfig.contextKey, "duradmin");
        props.put(pStore + DuraserviceConfig.contextKey, "durastore");
        props.put(pService + DuraserviceConfig.contextKey, "duraservice");

        return props;
    }

    private static Properties createDurastoreProps() {
        Properties props = new Properties();

        String dot = ".";
        String prefix = DurastoreConfig.QUALIFIER + dot;
        String p = prefix + DurastoreConfig.storageAccountKey + dot;
        String p0 = p + "0" + dot;
        String p1 = p + "1" + dot;

        Credential amazonCred = BaseTestUtil.getCredential(ResourceType.fromStorageProviderType(
            StorageProviderType.AMAZON_S3));

        Credential rackspaceCred = BaseTestUtil.getCredential(ResourceType.fromStorageProviderType(
            StorageProviderType.RACKSPACE));

        props.put(p0 + DurastoreConfig.ownerIdKey, "0");
        props.put(p0 + DurastoreConfig.isPrimaryKey, "true");
        props.put(p0 + DurastoreConfig.idKey, "1");
        props.put(p0 + DurastoreConfig.providerTypeKey,
                  StorageProviderType.AMAZON_S3.name());
        props.put(p0 + DurastoreConfig.usernameKey, amazonCred.getUsername());
        props.put(p0 + DurastoreConfig.passwordKey, amazonCred.getPassword());

        props.put(p1 + DurastoreConfig.ownerIdKey, "0");
        props.put(p1 + DurastoreConfig.isPrimaryKey, "false");
        props.put(p1 + DurastoreConfig.idKey, "2");
        props.put(p1 + DurastoreConfig.providerTypeKey,
                  StorageProviderType.RACKSPACE.name());
        props.put(p1 + DurastoreConfig.usernameKey,
                  rackspaceCred.getUsername());
        props.put(p1 + DurastoreConfig.passwordKey,
                  rackspaceCred.getPassword());
        return props;

    }

    private static Properties createDuraserviceProps() {
        Properties props = new Properties();

        String dot = ".";
        String prefix = DuraserviceConfig.QUALIFIER + dot;

        String p0 = prefix + DuraserviceConfig.primaryInstanceKey + dot;
        String p1 = prefix + DuraserviceConfig.userStoreKey + dot;
        String p2 = prefix + DuraserviceConfig.serviceStoreKey + dot;

        props.put(p0 + DuraserviceConfig.hostKey, "localhost");
        props.put(p0 + DuraserviceConfig.servicesAdminPortKey,
                  BaseTestUtil.getServicesAdminPort());
        props.put(p0 + DuraserviceConfig.servicesAdminContextKey,
                  BaseTestUtil.getServicesAdminContext());

        props.put(p1 + DuraserviceConfig.hostKey, "localhost");
        props.put(p1 + DuraserviceConfig.portKey, BaseTestUtil.getPort());
        props.put(p1 + DuraserviceConfig.contextKey, "durastore");
        props.put(p1 + DuraserviceConfig.msgBrokerUrlKey,
                  "tcp://localhost:61617");

        Credential credential =
            BaseTestUtil.getCredential(ResourceType.fromDuraCloudUserType(
                DuraCloudUserType.ROOT));
        props.put(p2 + DuraserviceConfig.hostKey, "localhost");
        props.put(p2 + DuraserviceConfig.portKey, BaseTestUtil.getPort());
        props.put(p2 + DuraserviceConfig.contextKey, "durastore");
        props.put(p2 + DuraserviceConfig.usernameKey, credential.getUsername());
        props.put(p2 + DuraserviceConfig.passwordKey, credential.getPassword());
        props.put(p2 + DuraserviceConfig.spaceIdKey,
                  "duracloud-service-repository");
        props.put(p2 + DuraserviceConfig.serviceXmlIdKey,
                  "duracloud-service-repository.xml");

        return props;
    }

    private static Properties createDuradminProps() {
        Properties props = new Properties();

        String prefix = DuradminConfig.QUALIFIER + ".";

        props.put(prefix + DuradminConfig.duraStoreHostKey, "localhost");
        props.put(prefix + DuradminConfig.duraStorePortKey, BaseTestUtil.getPort());
        props.put(prefix + DuradminConfig.duraStoreContextKey, "durastore");

        props.put(prefix + DuradminConfig.duraServiceHostKey, "localhost");
        props.put(prefix + DuradminConfig.duraServicePortKey, BaseTestUtil.getPort());
        props.put(prefix + DuradminConfig.duraServiceContextKey, "duraservice");

        return props;
    }

    private static Properties createSecurityProps() {
        final int NUM_ACCTS = 3;

        String basedn = "basedn";
        String userdn = "userdn";
        String password = "password";
        String url = "url";

        Set<Integer> acctIds = new HashSet<>();
        for (int i = 20; i < 20 + NUM_ACCTS; ++i) {
            acctIds.add(i);
        }

        Properties props = new Properties();
        String dot = ".";
        String prefix = SecurityConfig.QUALIFIER + dot;

        String ldapPre = prefix + SecurityConfig.ldapKey + dot;
        props.put(ldapPre + SecurityConfig.ldapBaseDnKey, basedn);
        props.put(ldapPre + SecurityConfig.ldapUserDnKey, userdn);
        props.put(ldapPre + SecurityConfig.ldapPasswordKey, password);
        props.put(ldapPre + SecurityConfig.ldapUrlKey, url);

        String acctPre = prefix + SecurityConfig.acctIdKey + dot;
        int x = 0;
        for (int acctId : acctIds) {
            props.put(acctPre + x++, Integer.toString(acctId));
        }

        return props;
    }

    @Test
    public void testInitialize() throws IOException {
        ApplicationInitializer appInit = new ApplicationInitializer(
            incompletePropsFile);

        boolean expected = false;
        doTest(appInit, expected);

        appInit = new ApplicationInitializer(propsFile);
        expected = true;
        doTest(appInit, expected);
    }

    private void doTest(ApplicationInitializer config, boolean expected) {
        boolean success;
        RestHttpHelper.HttpResponse response = null;
        try {
            response = config.initialize();
            success = true;
        } catch (Exception e) {
            success = false;
            System.out.println("^^ Expected error built into unit-test. ^^");
        }

        Assert.assertEquals(expected, success);
        if (success) {
            Assert.assertNotNull(response);
            Assert.assertEquals(200, response.getStatusCode());
        }
    }
}
