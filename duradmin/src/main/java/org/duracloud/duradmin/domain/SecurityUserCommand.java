/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.duradmin.domain;

import java.util.ArrayList;
import java.util.List;

import org.duracloud.security.domain.SecurityUserBean;

/**
 * @author Andrew Woods Date: Apr 23, 2010
 */
public class SecurityUserCommand {
    private List<SecurityUserBean> users = new ArrayList<SecurityUserBean>();
    private String username = "";
    private String password = "";
    private String email = "";
    private Boolean enabled = true;
    private Boolean accountNonExpired = true;
    private Boolean credentialsNonExpired = true;
    private Boolean accountNonLocked = true;
    private List<String> grantedAuthorities = new ArrayList<String>();
    private List<String> groups = new ArrayList<String>();
    private String verb = "none"; // add or delete or modify

    public SecurityUserCommand() {
    }

    public SecurityUserCommand(List<SecurityUserBean> users) {
        this.users = users;
    }

    public void addUser(SecurityUserBean user) {
        users.add(user);
    }

    public void removeUser(String username) {
        if (username != null) {
            List<SecurityUserBean> readOnlyUserList = new ArrayList<SecurityUserBean>(users);
            for (SecurityUserBean user : readOnlyUserList) {
                if (username.equalsIgnoreCase(user.getUsername())) {
                    users.remove(user);
                }
            }
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public List<String> getGrantedAuthorities() {
        return grantedAuthorities;
    }

    public void setGrantedAuthorities(List<String> grantedAuthorities) {
        this.grantedAuthorities = grantedAuthorities;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getAccountNonExpired() {
        return accountNonExpired;
    }

    public void setAccountNonExpired(Boolean accountNonExpired) {
        this.accountNonExpired = accountNonExpired;
    }

    public Boolean getAccountNonLocked() {
        return accountNonLocked;
    }

    public void setAccountNonLocked(Boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }

    public Boolean getCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    public void setCredentialsNonExpired(Boolean credentialsNonExpired) {
        this.credentialsNonExpired = credentialsNonExpired;
    }

    public List<SecurityUserBean> getUsers() {
        return users;
    }

    public void setUsers(List<SecurityUserBean> users) {
        this.users = users;
    }

    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }
}
