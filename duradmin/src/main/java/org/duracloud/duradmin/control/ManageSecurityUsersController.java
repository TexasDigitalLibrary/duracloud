/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.duradmin.control;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.duracloud.appconfig.domain.Application;
import org.duracloud.duradmin.config.DuradminConfig;
import org.duracloud.duradmin.domain.SecurityUserCommand;
import org.duracloud.security.DuracloudUserDetailsService;
import org.duracloud.security.domain.SecurityUserBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author Andrew Woods Date: Apr 23, 2010
 * @deprecated This class may no longer be needed as the "Administration" tab is now read-only.
 */
@Deprecated
@Controller
public class ManageSecurityUsersController {

    private final Logger log = LoggerFactory.getLogger(ManageSecurityUsersController.class);

    private DuracloudUserDetailsService userDetailsService;

    private PasswordEncoder passwordEncoder;

    @Autowired
    public ManageSecurityUsersController(DuracloudUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @RequestMapping(value = "/admin")
    public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, SecurityUserCommand cmd, BindingResult result) throws Exception {
        cmd.setUsers(this.userDetailsService.getUsers());
        String verb = cmd.getVerb();
        String username = cmd.getUsername();
        String password = cmd.getPassword();
        if (password.length() > 0) {
            password = passwordEncoder.encodePassword(cmd.getPassword(), null);
        }
        String email = cmd.getEmail();
        Boolean enabled = cmd.getEnabled();
        Boolean accountNonExpired = cmd.getAccountNonExpired();
        Boolean credentialsNonExpired = cmd.getCredentialsNonExpired();
        Boolean accountNonLocked = cmd.getAccountNonLocked();
        List<String> grantedAuthorities = cmd.getGrantedAuthorities();
        List<String> groups = cmd.getGroups();

        if (verb.equalsIgnoreCase("add")) {
            // default to adding new user as regular user if no grantedAuthorities are passed in
            if (grantedAuthorities.size() == 0) {
                grantedAuthorities.add("ROLE_USER");
            }
            SecurityUserBean user = new SecurityUserBean(username, password, email, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, grantedAuthorities, groups);
            cmd.addUser(user);
            log.info("added user {}", user.getUsername());
            return saveAndReturnModel(cmd);

        } else if (verb.equalsIgnoreCase("remove")) {
            SecurityUserBean user = getUser(cmd);
            cmd.removeUser(user.getUsername());
            log.info("removed user {}", username);
            return saveAndReturnModel(cmd);

        } else if (verb.equalsIgnoreCase("modify")) {
            SecurityUserBean user = getUser(cmd);
            // only modify password, email, grantedAuthorities and groups if they were passed in
            if (password.length() > 0) {
                user.setPassword(password);
            }
            if (email.length() > 0) {
                user.setEmail(email);
            }
            if (grantedAuthorities.size() > 0) {
                user.setGrantedAuthorities(grantedAuthorities);
            }
            if (groups.size() > 0) {
                user.setGroups(groups);
            }
            // will always re-enable an account when modified
            user.setAccountNonExpired(accountNonExpired);
            user.setCredentialsNonExpired(credentialsNonExpired);
            user.setAccountNonLocked(accountNonLocked);
            log.info("updated password for user {}", username);
            return saveAndReturnModel(cmd);

        } else if (verb.equalsIgnoreCase("get")) {
            return new ModelAndView("jsonView", "users", cmd.getUsers());
        } else {
            return new ModelAndView("admin-manager", "users", cmd.getUsers());
        }
    }

    private SecurityUserBean getUser(SecurityUserCommand cmd) {
        for (SecurityUserBean user : cmd.getUsers()) {
            if (user.getUsername().equals(cmd.getUsername())) {
                return user;
            }
        }
        return null;
    }

    private ModelAndView saveAndReturnModel(SecurityUserCommand cmd) throws Exception {
        pushUpdates(cmd.getUsers());
        cmd.setPassword("*********");
        return new ModelAndView("jsonView", "users", cmd.getUsers());
    }

    private void pushUpdates(List<SecurityUserBean> users) throws Exception {
        // update duradmin.
        userDetailsService.setUsers(users);
        log.debug("pushed updates to user details service");

        // update durastore.
        Application durastore = getDuraStoreApp();
        durastore.setSecurityUsers(users);
        log.debug("pushed updates to durastore");

        // update duraboss
        Application duraboss = getDuraBossApp();
        duraboss.setSecurityUsers(users);
        log.debug("pushed updates to duraboss");
    }

    private Application getDuraStoreApp() {
        String host = DuradminConfig.getDuraStoreHost();
        String port = DuradminConfig.getDuraStorePort();
        String ctxt = DuradminConfig.getDuraStoreContext();
        return new Application(host, port, ctxt);
    }

    private Application getDuraBossApp() {
        String host = DuradminConfig.getDuraBossHost();
        String port = DuradminConfig.getDuraBossPort();
        String ctxt = DuradminConfig.getDuraBossContext();
        return new Application(host, port, ctxt);
    }

    public DuracloudUserDetailsService getUserDetailsService() {
        return userDetailsService;
    }

    public void setUserDetailsService(DuracloudUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }
}
