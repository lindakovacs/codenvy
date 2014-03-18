/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2013] Codenvy, S.A. 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.factory;

import com.codenvy.api.factory.*;
import com.codenvy.api.factory.dto.Factory;
import com.codenvy.api.factory.dto.Restriction;
import com.codenvy.api.organization.server.dao.OrganizationDao;
import com.codenvy.api.organization.server.exception.OrganizationException;
import com.codenvy.api.organization.shared.dto.Organization;
import com.codenvy.api.organization.shared.dto.Subscription;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.server.exception.UserException;
import com.codenvy.api.user.server.exception.UserProfileException;
import com.codenvy.api.user.shared.dto.Attribute;
import com.codenvy.api.user.shared.dto.Profile;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.commons.lang.URLEncodedUtils;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Validates values of factory parameters.
 *
 * @author Alexander Garagatyi
 */
public class FactoryUrlBaseValidator implements FactoryUrlValidator {

    private static final String PARAMETRIZED_ILLEGAL_ORGID_PARAMETER_MESSAGE =
            "You have provided an invalid orgId %s. You could have provided the wrong code, " +
            "your subscription has expired, or you do not have a valid subscription account. Please contact " +
            "info@codenvy.com with any questions.";

    private static final String PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE =
            "You have provided a Tracked Factory parameter %s, and you do not have a valid orgId %s. You could have " +
            "provided the wrong code, your subscription has expired, or you do not have a valid subscription account." +
            " Please contact info@codenvy.com with any questions.";

    private static final String ILLEGAL_HOSTNAME_MESSAGE =
            "This Factory has its access restricted by certain hostname. Your client does not match the specified " +
            "policy. Please contact the owner of this Factory for more information.";

    private static final String ILLEGAL_VALIDSINCE_MESSAGE =
            "This Factory is not yet valid due to time restrictions applied by its owner.  Please, " +
            "contact owner for more information.";

    private static final String ILLEGAL_VALIDUNTIL_MESSAGE =
            "This Factory has expired due to time restrictions applied by its owner.  Please, " +
            "contact owner for more information.";

    private static final Pattern PROJECT_NAME_VALIDATOR = Pattern.compile("^[\\\\\\w\\\\\\d]+[\\\\\\w\\\\\\d_.-]*$");

    private OrganizationDao organizationDao;

    private UserDao userDao;

    private UserProfileDao profileDao;

    private FactoryBuilder factoryBuilder;

    @Inject
    public FactoryUrlBaseValidator(OrganizationDao organizationDao, UserDao userDao, UserProfileDao profileDao,
                                   FactoryBuilder factoryBuilder) {
        this.organizationDao = organizationDao;
        this.userDao = userDao;
        this.profileDao = profileDao;
        this.factoryBuilder = factoryBuilder;
    }

    @Override
    public void validateUrl(URI factoryUrl, HttpServletRequest request) throws FactoryUrlException {
        Map<String, Set<String>> params = URLEncodedUtils.parse(factoryUrl, "UTF-8");

        if (params.get("id") != null) {
            if (params.get("id").size() != 1) {
                throw new FactoryUrlException("Parameter 'id' has illegal value.");
            }
            FactoryClient factoryClient =
                    new HttpFactoryClient(factoryUrl.getScheme(), factoryUrl.getHost(), factoryUrl.getPort());
            Factory factory = factoryClient.getFactory(params.get("id").iterator().next());
            factory = factoryBuilder.convertToLatest(factory);
            this.validateObject(factoryBuilder.convertToLatest(factory), true, request);
        } else {
            Factory factory = factoryBuilder.buildNonEncoded(factoryUrl);
            this.validateObject(factoryBuilder.convertToLatest(factory), false, request);
        }
    }

    @Override
    public void validateObject(Factory factory, boolean encoded, HttpServletRequest request) throws FactoryUrlException {
        // check that vcs value is correct (only git is supported for now)
        if (!"git".equals(factory.getVcs())) {
            throw new FactoryUrlException("Parameter 'vcs' has illegal value. Only 'git' is supported for now.");
        }
        if (factory.getVcsurl() == null || factory.getVcsurl().isEmpty()) {
            throw new FactoryUrlException("Parameter 'vcsurl' has illegal value.");
        } else {
            try {
                URLDecoder.decode(factory.getVcsurl(), "UTF-8");
            } catch (IllegalArgumentException | UnsupportedEncodingException e) {
                throw new FactoryUrlException("Parameter 'vcsurl' has illegal value.");
            }
        }

        // validate project name
        String pname = null;
        if (factory.getV().equals("1.0")) {
            pname = factory.getPname();
        } else if (factory.getProjectattributes() != null) {
            pname = factory.getProjectattributes().getPname();
        }
        if (null != pname && !PROJECT_NAME_VALIDATOR.matcher(pname).matches()) {
            throw new FactoryUrlException(
                    "Project name must contain only Latin letters, digits or these following special characters -._.");
        }

        // validate orgid
        String orgid = "".equals(factory.getOrgid()) ? null : factory.getOrgid();
        if (null != orgid) {
            try {
                Organization account = organizationDao.getById(orgid);

                if (factory.getUserid() != null) {
                    User user = userDao.getById(factory.getUserid());
                    Profile profile = profileDao.getById(factory.getUserid());
                    for (Attribute attribute : profile.getAttributes()) {
                        if (attribute.getName().equals("temporary") && Boolean.parseBoolean(attribute.getValue()))
                            throw new FactoryUrlException("Current user is not allowed for using this method.");
                    }
                    if (!account.getOwner().equals(user.getId())) {
                        throw new FactoryUrlException("You are not authorized to use this orgid.");
                    }
                }

                try {
                    organizationDao.getById(factory.getOrgid());
                    List<Subscription> subscriptions = organizationDao.getSubscriptions(factory.getOrgid());
                    for (Subscription one : subscriptions) {
                        String endTime;
                        if ("TF".equals(one.getServiceId()) &&
                            (endTime = one.getEndDate()) != null) {
                            Date endTimeDate = new Date(Long.valueOf(endTime));
                            if (!endTimeDate.after(new Date())) {
                                throw new FactoryUrlException(String.format(PARAMETRIZED_ILLEGAL_ORGID_PARAMETER_MESSAGE, factory.getOrgid()));
                            }
                        } else {
                    throw new FactoryUrlException(String.format(PARAMETRIZED_ILLEGAL_ORGID_PARAMETER_MESSAGE, factory.getOrgid()));
                }
                    }
                } catch (OrganizationException | NumberFormatException e) {
                    throw new FactoryUrlException(String.format(PARAMETRIZED_ILLEGAL_ORGID_PARAMETER_MESSAGE, factory.getOrgid()));
                }

            } catch (UserException | UserProfileException | OrganizationException e) {
                throw new FactoryUrlException(String.format(PARAMETRIZED_ILLEGAL_ORGID_PARAMETER_MESSAGE, factory.getOrgid()));
            }
        }

        // validate tracked parameters
        Restriction restriction = factory.getRestriction();
        if (restriction != null) {
            if (0 != restriction.getValidsince()) {
                if (null == orgid) {
                    throw new FactoryUrlException(String.format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, "validsince", null));
                }

                if (new Date().before(new Date(restriction.getValidsince()))) {
                    throw new FactoryUrlException(ILLEGAL_VALIDSINCE_MESSAGE);
                }
            }

            if (0 != restriction.getValiduntil()) {
                if (null == orgid) {
                    throw new FactoryUrlException(String.format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, "validuntil", null));
                }

                if (new Date().after(new Date(restriction.getValiduntil()))) {
                    throw new FactoryUrlException(ILLEGAL_VALIDUNTIL_MESSAGE);
                }
            }

            if (null != restriction.getRefererhostname()) {
                if (null == orgid) {
                    throw new FactoryUrlException(String.format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, "refererhostname", null));
                }

                String host = null;
                if (null != request.getHeader("Referer")) {
                    try {
                        URI referer = new URI(request.getHeader("Referer"));

                        // relative url
                        if (null == referer.getHost()) {
                            host = request.getServerName();
                        } else {
                            host = referer.getHost();
                        }
                    } catch (URISyntaxException ignored) {
                    }
                }

                if (!restriction.getRefererhostname().equals(host)) {
                    throw new FactoryUrlException(ILLEGAL_HOSTNAME_MESSAGE);
                }
            }

            if (restriction.getRestrictbypassword()) {
                if (null == orgid) {
                    throw new FactoryUrlException(
                            String.format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, "restrictbypassword", null));
                }

                // TODO implement
            }

            if (null != restriction.getPassword()) {
                if (null == orgid) {
                    throw new FactoryUrlException(String.format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, "password", null));
                }

                // TODO implement
            }

            if (0 != restriction.getMaxsessioncount()) {
                if (null == orgid) {
                    throw new FactoryUrlException(String.format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, "maxsessioncount", null));
                }

                // TODO implement
            }
        }

        if (null != factory.getWelcome()) {
            if (null == orgid) {
                throw new FactoryUrlException(String.format(PARAMETRIZED_ILLEGAL_TRACKED_PARAMETER_MESSAGE, "welcome", null));
            }
        }
    }
}
