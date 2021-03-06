/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 *
 * Source file modified from the original ASF source; all changes made
 * are also under Apache License.
 */
package org.tightblog.pojos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.tightblog.util.I18nMessages;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Pattern;
import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "weblogger_user")
@NamedQueries({
        @NamedQuery(name = "User.getByEnabled&EndDateOrderByStartDateDesc",
                query = "SELECT u FROM User u WHERE u.status = ?1 AND u.dateCreated < ?2 ORDER BY u.dateCreated DESC"),
        @NamedQuery(name = "User.getByEndDateOrderByStartDateDesc",
                query = "SELECT u FROM User u WHERE u.dateCreated < ?1 ORDER BY u.dateCreated DESC"),
        @NamedQuery(name = "User.getByUserName&Enabled",
                query = "SELECT u FROM User u WHERE u.userName= ?1 AND u.status = org.tightblog.pojos.UserStatus.ENABLED"),
        @NamedQuery(name = "User.getByEndDate&StartDateOrderByStartDateDesc",
                query = "SELECT u FROM User u WHERE u.dateCreated < ?1 AND u.dateCreated > ?2 ORDER BY u.dateCreated DESC"),
        @NamedQuery(name = "User.getGlobalRole",
                query = "SELECT u.globalRole FROM User u WHERE u.userName = ?1"),
        @NamedQuery(name = "User.getCountEnabledDistinct",
                query = "SELECT COUNT(u) FROM User u WHERE u.status = org.tightblog.pojos.UserStatus.ENABLED")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {
    private String id;

    @NotBlank(message = "{error.add.user.missingUserName}")
    @Pattern(regexp = "[a-z0-9]*", message = "{error.add.user.badUserName}")
    private String userName;
    private GlobalRole globalRole;

    @NotBlank(message = "{Register.error.screenNameNull}")
    private String screenName;
    private UserStatus status = UserStatus.DISABLED;

    @NotBlank(message = "{Register.error.emailAddressNull}")
    @Email(message = "{error.add.user.badEmail}")
    private String emailAddress;
    private Instant dateCreated;
    private String locale;
    private String activationCode;
    private Instant lastLogin;

    private I18nMessages i18NMessages;

    public User() {
    }

    @Id
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Basic(optional = false)
    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Column(name = "global_role", nullable = false)
    @Enumerated(EnumType.STRING)
    public GlobalRole getGlobalRole() {
        return this.globalRole;
    }

    public void setGlobalRole(GlobalRole globalRole) {
        this.globalRole = globalRole;
    }

    @Basic(optional = false)
    @Enumerated(EnumType.STRING)
    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public boolean hasEffectiveGlobalRole(GlobalRole roleToCheck) {
        return globalRole.getWeight() >= roleToCheck.getWeight();
    }

    @Basic(optional = false)
    public String getScreenName() {
        return this.screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    @Basic(optional = false)
    public String getEmailAddress() {
        return this.emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @Basic(optional = false)
    public Instant getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Instant dateTime) {
        this.dateCreated = dateTime;
    }

    public Instant getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Instant lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getLocale() {
        return this.locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getActivationCode() {
        return activationCode;
    }

    public void setActivationCode(String activationCode) {
        this.activationCode = activationCode;
    }

    @Transient
    @JsonIgnore
    public I18nMessages getI18NMessages() {
        if (i18NMessages == null) {
            Locale userLocale = (getLocale() == null) ? Locale.getDefault() : Locale.forLanguageTag(getLocale());
            i18NMessages = I18nMessages.getMessages(userLocale);
        }
        return i18NMessages;
    }

    //------------------------------------------------------- Good citizenship

    public String toString() {
        String stringVal = "{" + getId();
        stringVal += ", " + getUserName();
        stringVal += ", " + getScreenName();
        stringVal += ", " + getGlobalRole();
        stringVal += ", " + getDateCreated();
        stringVal += ", " + status;
        stringVal += "}";
        return stringVal;
    }

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof User)) {
            return false;
        }
        User o = (User) other;
        return new EqualsBuilder().append(getId(), o.getId()).isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder().append(getId()).toHashCode();
    }

}
