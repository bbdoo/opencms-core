/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/db/generic/CmsUserDriver.java,v $
 * Date   : $Date: 2007/01/19 16:53:51 $
 * Version: $Revision: 1.110.2.7 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (c) 2005 Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.db.generic;

import org.opencms.configuration.CmsConfigurationManager;
import org.opencms.db.CmsDbContext;
import org.opencms.db.CmsDbEntryAlreadyExistsException;
import org.opencms.db.CmsDbEntryNotFoundException;
import org.opencms.db.CmsDbIoException;
import org.opencms.db.CmsDbSqlException;
import org.opencms.db.CmsDbUtil;
import org.opencms.db.CmsDriverManager;
import org.opencms.db.I_CmsDriver;
import org.opencms.db.I_CmsProjectDriver;
import org.opencms.db.I_CmsUserDriver;
import org.opencms.file.CmsDataAccessException;
import org.opencms.file.CmsFolder;
import org.opencms.file.CmsGroup;
import org.opencms.file.CmsProject;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.file.CmsUser;
import org.opencms.file.types.CmsResourceTypeFolder;
import org.opencms.i18n.CmsEncoder;
import org.opencms.i18n.CmsMessageContainer;
import org.opencms.main.CmsEvent;
import org.opencms.main.CmsException;
import org.opencms.main.CmsInitException;
import org.opencms.main.CmsLog;
import org.opencms.main.I_CmsEventListener;
import org.opencms.main.OpenCms;
import org.opencms.relations.CmsRelation;
import org.opencms.relations.CmsRelationFilter;
import org.opencms.relations.CmsRelationType;
import org.opencms.security.CmsAccessControlEntry;
import org.opencms.security.CmsOrganizationalUnit;
import org.opencms.security.CmsPasswordEncryptionException;
import org.opencms.security.CmsRole;
import org.opencms.security.I_CmsPrincipal;
import org.opencms.util.CmsStringUtil;
import org.opencms.util.CmsUUID;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.logging.Log;

/**
 * Generic (ANSI-SQL) database server implementation of the user driver methods.<p>
 * 
 * @author Thomas Weckert 
 * @author Carsten Weinholz 
 * @author Michael Emmerich 
 * @author Michael Moossen  
 * 
 * @version $Revision: 1.110.2.7 $
 * 
 * @since 6.0.0 
 */
public class CmsUserDriver implements I_CmsDriver, I_CmsUserDriver {

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(org.opencms.db.generic.CmsUserDriver.class);

    /** The root path for organizational units. */
    private static final String ORGUNIT_BASE_FOLDER = "/system/orgunits";

    /** Property for the organizational unit description. */
    private static final String ORGUNIT_PROPERTY_DESCRIPTION = CmsPropertyDefinition.PROPERTY_DESCRIPTION;

    /** A digest to encrypt the passwords. */
    protected MessageDigest m_digest;

    /** The algorithm used to encode passwords. */
    protected String m_digestAlgorithm;

    /** The file.encoding to code passwords after encryption with digest. */
    protected String m_digestFileEncoding;

    /** The driver manager. */
    protected CmsDriverManager m_driverManager;

    /** The SQL manager. */
    protected org.opencms.db.generic.CmsSqlManager m_sqlManager;

    /**
     * @see org.opencms.db.I_CmsUserDriver#addResourceToOrganizationalUnit(org.opencms.db.CmsDbContext, org.opencms.security.CmsOrganizationalUnit, org.opencms.file.CmsResource)
     */
    public void addResourceToOrganizationalUnit(CmsDbContext dbc, CmsOrganizationalUnit orgUnit, CmsResource resource)
    throws CmsDataAccessException {

        try {
            // check if the resource is a folder
            if (resource.isFile()) {
                throw new CmsDataAccessException(Messages.get().container(
                    Messages.ERR_ORGUNIT_RESOURCE_IS_NOT_FOLDER_2,
                    orgUnit.getName(),
                    dbc.removeSiteRoot(resource.getRootPath())));
            }

            // check resource scope for non root ous
            if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(orgUnit.getParentFqn())) {
                // get the parent ou
                CmsOrganizationalUnit parentOu = m_driverManager.readOrganizationalUnit(dbc, orgUnit.getParentFqn());
                // validate
                internalValidateResourceForOrgUnit(dbc, parentOu, resource.getRootPath());
            }

            // read the resource representing the organizational unit
            CmsResource ouResource = m_driverManager.readResource(dbc, orgUnit.getId(), CmsResourceFilter.ALL);

            // get the associated resources
            List vfsPaths = new ArrayList(internalResourcesForOrgUnit(dbc, ouResource));

            // check if already associated
            Iterator itPaths = vfsPaths.iterator();
            while (itPaths.hasNext()) {
                String path = (String)itPaths.next();
                if (resource.getRootPath().startsWith(path)) {
                    throw new CmsDataAccessException(Messages.get().container(
                        Messages.ERR_ORGUNIT_ALREADY_CONTAINS_RESOURCE_2,
                        orgUnit.getName(),
                        dbc.removeSiteRoot(resource.getRootPath())));
                }
            }

            // add the new resource
            CmsRelation relation = new CmsRelation(ouResource, resource, CmsRelationType.OU_RESOURCE);
            m_driverManager.getVfsDriver().createRelation(dbc, dbc.currentProject().getId(), relation);
            m_driverManager.getVfsDriver().createRelation(dbc, CmsProject.ONLINE_PROJECT_ID, relation);
        } catch (CmsException e) {
            throw new CmsDataAccessException(e.getMessageContainer(), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#createAccessControlEntry(org.opencms.db.CmsDbContext, org.opencms.file.CmsProject, org.opencms.util.CmsUUID, org.opencms.util.CmsUUID, int, int, int)
     */
    public void createAccessControlEntry(
        CmsDbContext dbc,
        CmsProject project,
        CmsUUID resource,
        CmsUUID principal,
        int allowed,
        int denied,
        int flags) throws CmsDataAccessException {

        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection(dbc, project.getId());
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_ACCESS_CREATE_5");

            stmt.setString(1, resource.toString());
            stmt.setString(2, principal.toString());
            stmt.setInt(3, allowed);
            stmt.setInt(4, denied);
            stmt.setInt(5, flags);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, null);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#createGroup(org.opencms.db.CmsDbContext, org.opencms.util.CmsUUID, java.lang.String, java.lang.String, int, java.lang.String)
     */
    public CmsGroup createGroup(
        CmsDbContext dbc,
        CmsUUID groupId,
        String groupFqn,
        String description,
        int flags,
        String parentGroupFqn) throws CmsDataAccessException {

        CmsUUID parentId = CmsUUID.getNullUUID();
        CmsGroup group = null;
        Connection conn = null;
        PreparedStatement stmt = null;

        groupFqn = internalAppendRootOrgUnit(dbc, groupFqn);
        parentGroupFqn = internalAppendRootOrgUnit(dbc, parentGroupFqn);

        if (existsGroup(dbc, groupFqn)) {
            CmsMessageContainer message = Messages.get().container(
                Messages.ERR_GROUP_WITH_NAME_ALREADY_EXISTS_1,
                groupFqn);
            if (LOG.isErrorEnabled()) {
                LOG.error(message.key());
            }
            throw new CmsDbEntryAlreadyExistsException(message);
        }

        try {
            // get the id of the parent group if necessary
            if (CmsStringUtil.isNotEmpty(parentGroupFqn)) {
                CmsGroup parentGroup = readGroup(dbc, parentGroupFqn);
                if (!parentGroup.isRole()
                    && !CmsOrganizationalUnit.getParentFqn(parentGroupFqn).equals(
                        CmsOrganizationalUnit.getParentFqn(groupFqn))) {
                    throw new CmsDataAccessException(Messages.get().container(
                        Messages.ERR_PARENT_GROUP_MUST_BE_IN_SAME_OU_3,
                        CmsOrganizationalUnit.getSimpleName(groupFqn),
                        CmsOrganizationalUnit.getParentFqn(groupFqn),
                        parentGroupFqn));
                }
                parentId = parentGroup.getId();
            }

            conn = getSqlManager().getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatement(conn, "C_GROUPS_CREATE_GROUP_6");

            // write new group to the database
            stmt.setString(1, groupId.toString());
            stmt.setString(2, parentId.toString());
            stmt.setString(3, CmsOrganizationalUnit.getSimpleName(groupFqn));
            stmt.setString(4, m_sqlManager.validateEmpty(description));
            stmt.setInt(5, flags);
            stmt.setString(6, CmsOrganizationalUnit.getParentFqn(groupFqn));
            stmt.executeUpdate();

            group = new CmsGroup(groupId, parentId, groupFqn, description, flags);
        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, null);
        }

        return group;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#createOrganizationalUnit(org.opencms.db.CmsDbContext, java.lang.String, java.lang.String, int, org.opencms.security.CmsOrganizationalUnit, String)
     */
    public CmsOrganizationalUnit createOrganizationalUnit(
        CmsDbContext dbc,
        String name,
        String description,
        int flags,
        CmsOrganizationalUnit parent,
        String associatedResource) throws CmsDataAccessException {

        // check the parent
        if ((parent == null) && !name.equals("/")) {
            throw new CmsDataAccessException(org.opencms.db.Messages.get().container(
                org.opencms.db.Messages.ERR_PARENT_ORGUNIT_NULL_0));
        }
        try {
            // get the parent ou folder
            CmsResource parentFolder = internalOrgUnitFolder(dbc, parent);

            // check that the associated resource exists and if is a folder
            CmsResource resource = m_driverManager.readFolder(dbc, associatedResource, CmsResourceFilter.ALL);

            String ouPath = ORGUNIT_BASE_FOLDER;
            // validate resource
            if (parentFolder != null) {
                internalValidateResourceForOrgUnit(
                    dbc,
                    internalCreateOrgUnitFromResource(dbc, parentFolder),
                    resource.getRootPath());
                ouPath = parentFolder.getRootPath();
            }
            // create the resource
            CmsResource ouFolder = internalCreateResourceForOrgUnit(dbc, ouPath + name, flags);

            // write description property
            internalWriteOrgUnitProperty(
                dbc,
                ouFolder,
                new CmsProperty(ORGUNIT_PROPERTY_DESCRIPTION, description, null));

            // create the ou object
            CmsOrganizationalUnit ou = internalCreateOrgUnitFromResource(dbc, ouFolder);

            if (ou.getParentFqn() != null) {
                // if not the root ou, create roles
                // the roles of the root ou, are created in #fillDefaults
                Iterator itRoles = CmsRole.getSystemRoles().iterator();
                while (itRoles.hasNext()) {
                    CmsRole role = (CmsRole)itRoles.next();
                    if (!role.isOrganizationalUnitIndependent()) {
                        String groupName = ou.getName() + role.getGroupName();
                        flags = I_CmsPrincipal.FLAG_ENABLED | I_CmsPrincipal.FLAG_GROUP_ROLE;
                        if ((role == CmsRole.WORKPLACE_USER) || (role == CmsRole.PROJECT_MANAGER)) {
                            flags |= I_CmsPrincipal.FLAG_GROUP_PROJECT_USER;
                        }
                        if (role == CmsRole.PROJECT_MANAGER) {
                            flags |= I_CmsPrincipal.FLAG_GROUP_PROJECT_MANAGER;
                        }
                        createGroup(
                            dbc,
                            CmsUUID.getConstantUUID(groupName),
                            groupName,
                            "A system role group",
                            flags,
                            null);
                    }
                }
            }
            m_driverManager.addResourceToOrgUnit(dbc, ou, resource);
            return ou;
        } catch (CmsException e) {
            throw new CmsDataAccessException(e.getMessageContainer(), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#createRootOrganizationalUnit(org.opencms.db.CmsDbContext)
     */
    public void createRootOrganizationalUnit(CmsDbContext dbc) {

        try {
            readOrganizationalUnit(dbc, "/");
        } catch (CmsException e) {
            try {
                CmsProject onlineProject = dbc.currentProject();
                CmsProject setupProject = onlineProject;
                // get the right offline project
                try {
                    // this if setting up OpenCms
                    setupProject = m_driverManager.readProject(
                        new CmsDbContext(),
                        I_CmsProjectDriver.SETUP_PROJECT_NAME);
                } catch (CmsException exc) {
                    // this if updating OpenCms
                    try {
                        setupProject = m_driverManager.readProject(new CmsDbContext(), "Offline");
                    } catch (CmsException exc2) {
                        // if no offline project found
                    }
                }
                dbc.getRequestContext().setCurrentProject(setupProject);
                try {
                    createOrganizationalUnit(dbc, "/", "The root organizational unit", 0, null, "/");
                } finally {
                    dbc.getRequestContext().setCurrentProject(onlineProject);
                }
                if (CmsLog.INIT.isInfoEnabled()) {
                    CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_ROOT_ORGUNIT_DEFAULTS_INITIALIZED_0));
                }
            } catch (CmsException exc) {
                if (CmsLog.INIT.isErrorEnabled()) {
                    CmsLog.INIT.error(
                        Messages.get().getBundle().key(Messages.INIT_ROOT_ORGUNIT_INITIALIZATION_FAILED_0),
                        exc);
                }
                throw new CmsInitException(Messages.get().container(Messages.ERR_INITIALIZING_USER_DRIVER_0), exc);
            }
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#createUser(CmsDbContext, CmsUUID, String, String, String, String, String, String, long, int, Map, String)
     */
    public CmsUser createUser(
        CmsDbContext dbc,
        CmsUUID id,
        String userFqn,
        String password,
        String description,
        String firstname,
        String lastname,
        String email,
        long lastlogin,
        int flags,
        Map additionalInfos,
        String address) throws CmsDataAccessException {

        userFqn = internalAppendRootOrgUnit(dbc, userFqn);

        Connection conn = null;
        PreparedStatement stmt = null;

        if (existsUser(dbc, userFqn)) {
            CmsMessageContainer message = Messages.get().container(
                Messages.ERR_USER_WITH_NAME_ALREADY_EXISTS_1,
                userFqn);
            if (LOG.isErrorEnabled()) {
                LOG.error(message.key());
            }
            throw new CmsDbEntryAlreadyExistsException(message);
        }

        try {
            conn = m_sqlManager.getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_ADD_12");

            stmt.setString(1, id.toString());
            stmt.setString(2, CmsOrganizationalUnit.getSimpleName(userFqn));
            stmt.setString(3, password);
            stmt.setString(4, m_sqlManager.validateEmpty(description));
            stmt.setString(5, m_sqlManager.validateEmpty(firstname));
            stmt.setString(6, m_sqlManager.validateEmpty(lastname));
            stmt.setString(7, m_sqlManager.validateEmpty(email));
            stmt.setLong(8, lastlogin);
            stmt.setInt(9, flags);
            m_sqlManager.setBytes(stmt, 10, internalSerializeAdditionalUserInfo(additionalInfos));
            stmt.setString(11, m_sqlManager.validateEmpty(address));
            stmt.setString(12, CmsOrganizationalUnit.getParentFqn(userFqn));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } catch (IOException e) {
            throw new CmsDbIoException(Messages.get().container(Messages.ERR_SERIALIZING_USER_DATA_1, userFqn), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, null);
        }

        return readUser(dbc, id);
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#createUserInGroup(org.opencms.db.CmsDbContext, org.opencms.util.CmsUUID, org.opencms.util.CmsUUID)
     */
    public void createUserInGroup(CmsDbContext dbc, CmsUUID userId, CmsUUID groupId) throws CmsDataAccessException {

        Connection conn = null;
        PreparedStatement stmt = null;

        // check if user is already in group
        if (!internalValidateUserInGroup(dbc, userId, groupId)) {
            // if not, add this user to the group
            try {
                conn = getSqlManager().getConnection(dbc);
                stmt = m_sqlManager.getPreparedStatement(conn, "C_GROUPS_ADD_USER_TO_GROUP_3");

                // write the new assingment to the database
                stmt.setString(1, groupId.toString());
                stmt.setString(2, userId.toString());
                // flag field is not used yet
                stmt.setInt(3, CmsDbUtil.UNKNOWN_ID);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new CmsDbSqlException(Messages.get().container(
                    Messages.ERR_GENERIC_SQL_1,
                    CmsDbSqlException.getErrorQuery(stmt)), e);
            } finally {
                m_sqlManager.closeAll(dbc, conn, stmt, null);
            }
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#deleteAccessControlEntries(org.opencms.db.CmsDbContext, org.opencms.file.CmsProject, org.opencms.util.CmsUUID)
     */
    public void deleteAccessControlEntries(CmsDbContext dbc, CmsProject project, CmsUUID resource)
    throws CmsDataAccessException {

        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection(dbc, project.getId());
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_ACCESS_SET_FLAGS_ALL_2");

            stmt.setInt(1, CmsAccessControlEntry.ACCESS_FLAGS_DELETED);
            stmt.setString(2, resource.toString());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, null);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#deleteGroup(org.opencms.db.CmsDbContext, java.lang.String)
     */
    public void deleteGroup(CmsDbContext dbc, String groupFqn) throws CmsDataAccessException {

        groupFqn = internalAppendRootOrgUnit(dbc, groupFqn);

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getSqlManager().getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatement(conn, "C_GROUPS_DELETE_GROUP_2");

            stmt.setString(1, CmsOrganizationalUnit.getSimpleName(groupFqn));
            stmt.setString(2, CmsOrganizationalUnit.getParentFqn(groupFqn));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, null);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#deleteOrganizationalUnit(org.opencms.db.CmsDbContext, org.opencms.security.CmsOrganizationalUnit)
     */
    public void deleteOrganizationalUnit(CmsDbContext dbc, CmsOrganizationalUnit organizationalUnit)
    throws CmsDataAccessException {

        try {
            CmsResource resource = m_driverManager.readResource(
                dbc,
                organizationalUnit.getId(),
                CmsResourceFilter.DEFAULT);
            internalDeleteOrgUnitResource(dbc, resource);
        } catch (CmsException e) {
            throw new CmsDataAccessException(e.getMessageContainer(), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#deleteUser(org.opencms.db.CmsDbContext, java.lang.String)
     */
    public void deleteUser(CmsDbContext dbc, String userFqn) throws CmsDataAccessException {

        userFqn = internalAppendRootOrgUnit(dbc, userFqn);

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = getSqlManager().getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_DELETE_2");

            stmt.setString(1, CmsOrganizationalUnit.getSimpleName(userFqn));
            stmt.setString(2, CmsOrganizationalUnit.getParentFqn(userFqn));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, null);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#deleteUserInGroup(org.opencms.db.CmsDbContext, org.opencms.util.CmsUUID, org.opencms.util.CmsUUID)
     */
    public void deleteUserInGroup(CmsDbContext dbc, CmsUUID userId, CmsUUID groupId) throws CmsDataAccessException {

        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getSqlManager().getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatement(conn, "C_GROUPS_REMOVE_USER_FROM_GROUP_2");

            stmt.setString(1, groupId.toString());
            stmt.setString(2, userId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, null);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#destroy()
     */
    public void destroy() throws Throwable {

        m_sqlManager = null;
        m_driverManager = null;

        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_SHUTDOWN_DRIVER_1, getClass().getName()));
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#existsGroup(org.opencms.db.CmsDbContext, java.lang.String)
     */
    public boolean existsGroup(CmsDbContext dbc, String groupFqn) throws CmsDataAccessException {

        groupFqn = internalAppendRootOrgUnit(dbc, groupFqn);

        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        boolean result = false;

        try {
            conn = getSqlManager().getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatement(conn, "C_GROUPS_READ_BY_NAME_2");

            stmt.setString(1, CmsOrganizationalUnit.getSimpleName(groupFqn));
            stmt.setString(2, CmsOrganizationalUnit.getParentFqn(groupFqn));
            res = stmt.executeQuery();

            // create new Cms group object
            if (res.next()) {
                result = true;
            } else {
                result = false;
            }

        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, res);
        }

        return result;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#existsUser(org.opencms.db.CmsDbContext, java.lang.String)
     */
    public boolean existsUser(CmsDbContext dbc, String userFqn) throws CmsDataAccessException {

        userFqn = internalAppendRootOrgUnit(dbc, userFqn);

        PreparedStatement stmt = null;
        ResultSet res = null;
        Connection conn = null;
        boolean result = false;

        try {
            conn = getSqlManager().getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_READ_BY_NAME_2");
            stmt.setString(1, CmsOrganizationalUnit.getSimpleName(userFqn));
            stmt.setString(2, CmsOrganizationalUnit.getParentFqn(userFqn));

            res = stmt.executeQuery();

            if (res.next()) {
                result = true;
            } else {
                result = false;
            }
        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, res);
        }

        return result;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#fillDefaults(org.opencms.db.CmsDbContext)
     */
    public void fillDefaults(CmsDbContext dbc) throws CmsInitException {

        String rootAdminRole = CmsRole.ROOT_ADMIN.getGroupName();
        try {
            // only do something if really needed
            if (!existsGroup(dbc, rootAdminRole)) {
                Iterator itRoles = CmsRole.getSystemRoles().iterator();
                while (itRoles.hasNext()) {
                    CmsRole role = (CmsRole)itRoles.next();
                    String groupName = role.getGroupName();
                    if (!role.isOrganizationalUnitIndependent()) {
                        groupName = "/" + groupName;
                    }
                    int flags = I_CmsPrincipal.FLAG_ENABLED | I_CmsPrincipal.FLAG_GROUP_ROLE;
                    if ((role == CmsRole.WORKPLACE_USER) || (role == CmsRole.PROJECT_MANAGER)) {
                        flags |= I_CmsPrincipal.FLAG_GROUP_PROJECT_USER;
                    }
                    if (role == CmsRole.PROJECT_MANAGER) {
                        flags |= I_CmsPrincipal.FLAG_GROUP_PROJECT_MANAGER;
                    }
                    createGroup(dbc, CmsUUID.getConstantUUID(groupName), groupName, "A system role group", flags, null);
                }
                if (CmsLog.INIT.isInfoEnabled()) {
                    CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_SYSTEM_ROLES_CREATED_0));
                }
            }
        } catch (CmsException e) {
            if (CmsLog.INIT.isErrorEnabled()) {
                CmsLog.INIT.error(Messages.get().getBundle().key(Messages.INIT_SYSTEM_ROLES_CREATION_FAILED_0), e);
            }
            throw new CmsInitException(Messages.get().container(Messages.ERR_INITIALIZING_USER_DRIVER_0), e);
        }

        try {
            String administratorsGroup = OpenCms.getDefaultUsers().getGroupAdministrators();
            String guestGroup = OpenCms.getDefaultUsers().getGroupGuests();
            String usersGroup = OpenCms.getDefaultUsers().getGroupUsers();
            String projectmanagersGroup = OpenCms.getDefaultUsers().getGroupProjectmanagers();
            String guestUser = OpenCms.getDefaultUsers().getUserGuest();
            String adminUser = OpenCms.getDefaultUsers().getUserAdmin();
            String exportUser = OpenCms.getDefaultUsers().getUserExport();
            String deleteUser = OpenCms.getDefaultUsers().getUserDeletedResource();

            if (!existsGroup(dbc, administratorsGroup)) {
                CmsGroup guests = createGroup(
                    dbc,
                    CmsUUID.getConstantUUID(guestGroup),
                    guestGroup,
                    "The guest group",
                    I_CmsPrincipal.FLAG_ENABLED,
                    null);
                CmsGroup administrators = createGroup(
                    dbc,
                    CmsUUID.getConstantUUID(administratorsGroup),
                    administratorsGroup,
                    "The administrators group",
                    I_CmsPrincipal.FLAG_ENABLED | I_CmsPrincipal.FLAG_GROUP_PROJECT_MANAGER,
                    null);
                CmsGroup users = createGroup(
                    dbc,
                    CmsUUID.getConstantUUID(usersGroup),
                    usersGroup,
                    "The users group",
                    I_CmsPrincipal.FLAG_ENABLED | I_CmsPrincipal.FLAG_GROUP_PROJECT_USER,
                    null);
                createGroup(
                    dbc,
                    CmsUUID.getConstantUUID(projectmanagersGroup),
                    projectmanagersGroup,
                    "The projectmanager group",
                    I_CmsPrincipal.FLAG_ENABLED
                        | I_CmsPrincipal.FLAG_GROUP_PROJECT_MANAGER
                        | I_CmsPrincipal.FLAG_GROUP_PROJECT_USER,
                    users.getName());

                CmsUser guest = createUser(
                    dbc,
                    CmsUUID.getConstantUUID(guestUser),
                    guestUser,
                    OpenCms.getPasswordHandler().digest(""),
                    "The guest user",
                    " ",
                    " ",
                    " ",
                    0,
                    I_CmsPrincipal.FLAG_ENABLED,
                    new Hashtable(),
                    " ");
                CmsUser admin = createUser(
                    dbc,
                    CmsUUID.getConstantUUID(adminUser),
                    adminUser,
                    OpenCms.getPasswordHandler().digest("admin"),
                    "The admin user",
                    " ",
                    " ",
                    " ",
                    0,
                    I_CmsPrincipal.FLAG_ENABLED,
                    new Hashtable(),
                    " ");

                createUserInGroup(dbc, guest.getId(), guests.getId());
                createUserInGroup(dbc, admin.getId(), administrators.getId());

                if (!exportUser.equals(OpenCms.getDefaultUsers().getUserAdmin())
                    && !exportUser.equals(OpenCms.getDefaultUsers().getUserGuest())) {

                    CmsUser export = createUser(
                        dbc,
                        CmsUUID.getConstantUUID(exportUser),
                        exportUser,
                        OpenCms.getPasswordHandler().digest((new CmsUUID()).toString()),
                        "The static export user",
                        " ",
                        " ",
                        " ",
                        0,
                        I_CmsPrincipal.FLAG_ENABLED,
                        Collections.EMPTY_MAP,
                        " ");
                    createUserInGroup(dbc, export.getId(), guests.getId());
                }

                if (!deleteUser.equals(OpenCms.getDefaultUsers().getUserAdmin())
                    && !deleteUser.equals(OpenCms.getDefaultUsers().getUserGuest())
                    && !deleteUser.equals(OpenCms.getDefaultUsers().getUserExport())) {

                    createUser(
                        dbc,
                        CmsUUID.getConstantUUID(deleteUser),
                        deleteUser,
                        OpenCms.getPasswordHandler().digest((new CmsUUID()).toString()),
                        "The default user for deleted resources",
                        " ",
                        " ",
                        " ",
                        0,
                        I_CmsPrincipal.FLAG_ENABLED,
                        Collections.EMPTY_MAP,
                        " ");
                }

                if (CmsLog.INIT.isInfoEnabled()) {
                    CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_DEFAULT_USERS_CREATED_0));
                }
            }
            try {
                createUserInGroup(dbc, CmsUUID.getConstantUUID(adminUser), CmsUUID.getConstantUUID(rootAdminRole));
            } catch (Exception e) {
                // ignore
            }

        } catch (CmsException e) {
            if (CmsLog.INIT.isErrorEnabled()) {
                CmsLog.INIT.error(Messages.get().getBundle().key(Messages.INIT_DEFAULT_USERS_CREATION_FAILED_0), e);
            }
            throw new CmsInitException(Messages.get().container(Messages.ERR_INITIALIZING_USER_DRIVER_0), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#getGroups(org.opencms.db.CmsDbContext, org.opencms.security.CmsOrganizationalUnit, boolean, boolean)
     */
    public List getGroups(CmsDbContext dbc, CmsOrganizationalUnit orgUnit, boolean includeSubOus, boolean readRoles)
    throws CmsDataAccessException {

        // compose the query
        String sqlQuery = createRoleQuery("C_GROUPS_GET_GROUPS_0", includeSubOus, readRoles);
        // adjust parameter to use with LIKE
        String ouFqn = orgUnit.getName();
        if (includeSubOus) {
            ouFqn += "%";
        }

        // execute it
        List groups = new ArrayList();
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            // create statement
            conn = m_sqlManager.getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatementForSql(conn, sqlQuery);

            stmt.setString(1, ouFqn);
            stmt.setInt(2, I_CmsPrincipal.FLAG_GROUP_ROLE);

            res = stmt.executeQuery();

            // create new Cms group objects
            while (res.next()) {
                groups.add(internalCreateGroup(res));
            }
        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, res);
        }
        return groups;
    }

    /**
     * Returns a sql query to select groups.<p>
     * 
     * @param mainQuery the main select sql query
     * @param includeSubOus if groups in sub-ous should be included in the selection
     * @param readRoles if groups or roles whould be selected
     * 
     * @return a sql query to select groups
     */
    private String createRoleQuery(String mainQuery, boolean includeSubOus, boolean readRoles) {

        String sqlQuery = m_sqlManager.readQuery(mainQuery);
        sqlQuery += " ";
        if (includeSubOus) {
            sqlQuery += m_sqlManager.readQuery("C_GROUPS_GROUP_OU_LIKE_1");
        } else {
            sqlQuery += m_sqlManager.readQuery("C_GROUPS_GROUP_OU_EQUALS_1");
        }
        sqlQuery += AND_CONDITION;
        if (readRoles) {
            sqlQuery += m_sqlManager.readQuery("C_GROUPS_SELECT_ROLES_1");
        } else {
            sqlQuery += m_sqlManager.readQuery("C_GROUPS_SELECT_GROUPS_1");
        }
        sqlQuery += " ";
        sqlQuery += m_sqlManager.readQuery("C_GROUPS_ORDER_0");
        return sqlQuery;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#getOrganizationalUnits(org.opencms.db.CmsDbContext, org.opencms.security.CmsOrganizationalUnit, boolean)
     */
    public List getOrganizationalUnits(CmsDbContext dbc, CmsOrganizationalUnit parent, boolean includeChilds)
    throws CmsDataAccessException {

        List orgUnits = new ArrayList();
        try {
            CmsResource parentFolder = internalOrgUnitFolder(dbc, parent);
            Iterator itResources = m_driverManager.readResources(
                dbc,
                parentFolder,
                CmsResourceFilter.DEFAULT,
                includeChilds).iterator();
            while (itResources.hasNext()) {
                CmsResource resource = (CmsResource)itResources.next();
                orgUnits.add(internalCreateOrgUnitFromResource(dbc, resource));
            }
        } catch (CmsException e) {
            throw new CmsDataAccessException(e.getMessageContainer(), e);
        }
        return orgUnits;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#getOrganizationalUnitsForFolder(CmsDbContext, CmsFolder)
     */
    public List getOrganizationalUnitsForFolder(CmsDbContext dbc, CmsFolder folder) throws CmsDataAccessException {

        List orgUnits = new ArrayList();
        CmsRelationFilter filter = CmsRelationFilter.SOURCES.filterType(CmsRelationType.OU_RESOURCE);

        // get the starting folder
        CmsFolder parent = folder;
        while (parent != null) {
            Iterator itRelations = m_driverManager.getVfsDriver().readRelations(
                dbc,
                dbc.currentProject().getId(),
                filter.filterResource(parent)).iterator();
            while (itRelations.hasNext()) {
                CmsRelation relation = (CmsRelation)itRelations.next();
                String ouFqn = relation.getSourcePath().substring(ORGUNIT_BASE_FOLDER.length());
                orgUnits.add(ouFqn);
            }
            parent = m_driverManager.getVfsDriver().readParentFolder(
                dbc,
                dbc.currentProject().getId(),
                parent.getStructureId());
        }
        return orgUnits;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#getResourcesForOrganizationalUnit(org.opencms.db.CmsDbContext, org.opencms.security.CmsOrganizationalUnit)
     */
    public List getResourcesForOrganizationalUnit(CmsDbContext dbc, CmsOrganizationalUnit orgUnit)
    throws CmsDataAccessException {

        List result = new ArrayList();
        try {
            CmsResource ouResource = m_driverManager.readResource(dbc, orgUnit.getId(), CmsResourceFilter.ALL);
            Iterator itPaths = internalResourcesForOrgUnit(dbc, ouResource).iterator();
            while (itPaths.hasNext()) {
                String path = (String)itPaths.next();
                result.add(m_driverManager.readResource(dbc, path, CmsResourceFilter.ALL));
            }
        } catch (CmsException e) {
            throw new CmsDataAccessException(e.getMessageContainer(), e);
        }
        return result;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#getSqlManager()
     */
    public CmsSqlManager getSqlManager() {

        return m_sqlManager;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#getUsers(org.opencms.db.CmsDbContext, org.opencms.security.CmsOrganizationalUnit, boolean)
     */
    public List getUsers(CmsDbContext dbc, CmsOrganizationalUnit orgUnit, boolean recursive)
    throws CmsDataAccessException {

        List users = new ArrayList();
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            // create statement
            conn = m_sqlManager.getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_GET_USERS_FOR_ORGUNIT_1");

            String param = orgUnit.getName();
            if (recursive) {
                param += "%";
            }
            stmt.setString(1, param);

            res = stmt.executeQuery();

            // create new Cms group objects
            while (res.next()) {
                users.add(internalCreateUser(res));
            }

        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } catch (IOException e) {
            throw new CmsDbIoException(Messages.get().container(Messages.ERR_READING_USERS_0), e);
        } catch (ClassNotFoundException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_READING_USERS_0), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, res);
        }
        return users;
    }

    /**
     * @see org.opencms.db.I_CmsDriver#init(org.opencms.db.CmsDbContext, org.opencms.configuration.CmsConfigurationManager, java.util.List, org.opencms.db.CmsDriverManager)
     */
    public void init(
        CmsDbContext dbc,
        CmsConfigurationManager configurationManager,
        List successiveDrivers,
        CmsDriverManager driverManager) {

        Map configuration = configurationManager.getConfiguration();

        ExtendedProperties config;
        if (configuration instanceof ExtendedProperties) {
            config = (ExtendedProperties)configuration;
        } else {
            config = new ExtendedProperties();
            config.putAll(configuration);
        }

        String poolUrl = config.get("db.user.pool").toString();
        String classname = config.get("db.user.sqlmanager").toString();
        m_sqlManager = this.initSqlManager(classname);
        m_sqlManager.init(I_CmsUserDriver.DRIVER_TYPE_ID, poolUrl);

        m_driverManager = driverManager;

        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_ASSIGNED_POOL_1, poolUrl));
        }

        m_digestAlgorithm = config.getString(CmsDriverManager.CONFIGURATION_DB + ".user.digest.type", "MD5");
        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_DIGEST_ALGORITHM_1, m_digestAlgorithm));
        }

        m_digestFileEncoding = config.getString(
            CmsDriverManager.CONFIGURATION_DB + ".user.digest.encoding",
            CmsEncoder.ENCODING_UTF_8);
        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_DIGEST_ENCODING_1, m_digestFileEncoding));
        }

        // create the digest
        try {
            m_digest = MessageDigest.getInstance(m_digestAlgorithm);
            if (CmsLog.INIT.isInfoEnabled()) {
                CmsLog.INIT.info(Messages.get().getBundle().key(
                    Messages.INIT_DIGEST_ENC_3,
                    m_digest.getAlgorithm(),
                    m_digest.getProvider().getName(),
                    String.valueOf(m_digest.getProvider().getVersion())));
            }
        } catch (NoSuchAlgorithmException e) {
            if (CmsLog.INIT.isInfoEnabled()) {
                CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_SET_DIGEST_ERROR_0), e);
            }
        }

        if ((successiveDrivers != null) && !successiveDrivers.isEmpty()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(Messages.get().getBundle().key(
                    Messages.LOG_SUCCESSIVE_DRIVERS_UNSUPPORTED_1,
                    getClass().getName()));
            }
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#initSqlManager(String)
     */
    public org.opencms.db.generic.CmsSqlManager initSqlManager(String classname) {

        return CmsSqlManager.getInstance(classname);
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#publishAccessControlEntries(org.opencms.db.CmsDbContext, org.opencms.file.CmsProject, org.opencms.file.CmsProject, org.opencms.util.CmsUUID, org.opencms.util.CmsUUID)
     */
    public void publishAccessControlEntries(
        CmsDbContext dbc,
        CmsProject offlineProject,
        CmsProject onlineProject,
        CmsUUID offlineId,
        CmsUUID onlineId) throws CmsDataAccessException {

        PreparedStatement stmt = null;
        Connection conn = null;
        ResultSet res = null;

        // at first, we remove all access contries of this resource in the online project
        m_driverManager.getUserDriver().removeAccessControlEntries(dbc, onlineProject, onlineId);

        // then, we copy the access control entries from the offline project into the online project
        try {
            conn = m_sqlManager.getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatement(conn, offlineProject, "C_ACCESS_READ_ENTRIES_1");

            stmt.setString(1, offlineId.toString());

            res = stmt.executeQuery();

            while (res.next()) {
                CmsAccessControlEntry ace = internalCreateAce(res, onlineId);
                if ((ace.getFlags() & CmsAccessControlEntry.ACCESS_FLAGS_DELETED) == 0) {
                    m_driverManager.getUserDriver().writeAccessControlEntry(dbc, onlineProject, ace);
                }
            }

        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, res);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#readAccessControlEntries(org.opencms.db.CmsDbContext, org.opencms.file.CmsProject, org.opencms.util.CmsUUID, boolean)
     */
    public List readAccessControlEntries(CmsDbContext dbc, CmsProject project, CmsUUID resource, boolean inheritedOnly)
    throws CmsDataAccessException {

        List aceList = new ArrayList();
        PreparedStatement stmt = null;
        Connection conn = null;
        ResultSet res = null;

        try {
            conn = m_sqlManager.getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_ACCESS_READ_ENTRIES_1");

            String resId = resource.toString();
            stmt.setString(1, resId);

            res = stmt.executeQuery();

            // create new CmsAccessControlEntry and add to list
            while (res.next()) {
                CmsAccessControlEntry ace = internalCreateAce(res);
                if ((ace.getFlags() & CmsAccessControlEntry.ACCESS_FLAGS_DELETED) > 0) {
                    continue;
                }

                if (inheritedOnly && !ace.isInheriting()) {
                    continue;
                }

                if (inheritedOnly && ace.isInheriting()) {
                    ace.setFlags(CmsAccessControlEntry.ACCESS_FLAGS_INHERITED);
                }

                aceList.add(ace);
            }

            return aceList;

        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, res);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#readAccessControlEntry(org.opencms.db.CmsDbContext, org.opencms.file.CmsProject, org.opencms.util.CmsUUID, org.opencms.util.CmsUUID)
     */
    public CmsAccessControlEntry readAccessControlEntry(
        CmsDbContext dbc,
        CmsProject project,
        CmsUUID resource,
        CmsUUID principal) throws CmsDataAccessException {

        CmsAccessControlEntry ace = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        ResultSet res = null;

        try {
            conn = m_sqlManager.getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_ACCESS_READ_ENTRY_2");

            stmt.setString(1, resource.toString());
            stmt.setString(2, principal.toString());

            res = stmt.executeQuery();

            // create new CmsAccessControlEntry
            if (res.next()) {
                ace = internalCreateAce(res);
            } else {
                res.close();
                res = null;
                throw new CmsDbEntryNotFoundException(Messages.get().container(
                    Messages.ERR_NO_ACE_FOUND_2,
                    resource,
                    principal));
            }

            return ace;

        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, res);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#readChildGroups(org.opencms.db.CmsDbContext, java.lang.String)
     */
    public List readChildGroups(CmsDbContext dbc, String parentGroupFqn) throws CmsDataAccessException {

        parentGroupFqn = internalAppendRootOrgUnit(dbc, parentGroupFqn);

        List childs = new ArrayList();
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            // get parent group
            CmsGroup parent = m_driverManager.getUserDriver().readGroup(dbc, parentGroupFqn);
            // parent group exists, so get all childs
            if (parent != null) {
                // create statement
                conn = m_sqlManager.getConnection(dbc);
                stmt = m_sqlManager.getPreparedStatement(conn, "C_GROUPS_GET_CHILD_1");
                stmt.setString(1, parent.getId().toString());
                res = stmt.executeQuery();
                // create new Cms group objects
                while (res.next()) {
                    childs.add(internalCreateGroup(res));
                }
            }
        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            // close all db-resources
            m_sqlManager.closeAll(dbc, conn, stmt, res);
        }
        return childs;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#readGroup(org.opencms.db.CmsDbContext, org.opencms.util.CmsUUID)
     */
    public CmsGroup readGroup(CmsDbContext dbc, CmsUUID groupId) throws CmsDataAccessException {

        CmsGroup group = null;
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatement(conn, "C_GROUPS_READ_BY_ID_1");

            // read the group from the database
            stmt.setString(1, groupId.toString());
            res = stmt.executeQuery();
            // create new Cms group object
            if (res.next()) {
                group = internalCreateGroup(res);
            } else {
                CmsMessageContainer message = Messages.get().container(Messages.ERR_NO_GROUP_WITH_ID_1, groupId);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(message.key());
                }
                throw new CmsDbEntryNotFoundException(message);
            }

        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, res);
        }

        return group;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#readGroup(org.opencms.db.CmsDbContext, java.lang.String)
     */
    public CmsGroup readGroup(CmsDbContext dbc, String groupFqn) throws CmsDataAccessException {

        groupFqn = internalAppendRootOrgUnit(dbc, groupFqn);

        CmsGroup group = null;
        ResultSet res = null;
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatement(conn, "C_GROUPS_READ_BY_NAME_2");

            // read the group from the database
            stmt.setString(1, CmsOrganizationalUnit.getSimpleName(groupFqn));
            stmt.setString(2, CmsOrganizationalUnit.getParentFqn(groupFqn));
            res = stmt.executeQuery();

            // create new Cms group object
            if (res.next()) {
                group = internalCreateGroup(res);
            } else {
                CmsMessageContainer message = org.opencms.db.Messages.get().container(
                    org.opencms.db.Messages.ERR_UNKNOWN_GROUP_1,
                    groupFqn);
                if (LOG.isWarnEnabled()) {
                    LOG.warn(message.key());
                }
                throw new CmsDbEntryNotFoundException(message);
            }

        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, res);
        }
        return group;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#readGroupsOfUser(CmsDbContext, CmsUUID, String, boolean, String, boolean)
     */
    public List readGroupsOfUser(
        CmsDbContext dbc,
        CmsUUID userId,
        String ouFqn,
        boolean includeChildOus,
        String remoteAddress,
        boolean readRoles) throws CmsDataAccessException {

        // compose the query
        String sqlQuery = createRoleQuery("C_GROUPS_GET_GROUPS_OF_USER_1", includeChildOus, readRoles);
        // adjust parameter to use with LIKE
        String ouFqnParam = ouFqn;
        if (includeChildOus) {
            ouFqnParam = "%";
        }

        // execute it
        List groups = new ArrayList();
        // if recursive first get the given ou, and then the rest
        // this is to correctly handle the case of having 2 ous: /a and /ab (just a like op does not work!)
        if (includeChildOus) {
            groups.addAll(m_driverManager.getUserDriver().readGroupsOfUser(
                dbc,
                userId,
                ouFqn,
                false,
                remoteAddress,
                readRoles));
        }

        PreparedStatement stmt = null;
        ResultSet res = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatementForSql(conn, sqlQuery);

            //  get all all groups of the user
            stmt.setString(1, userId.toString());
            stmt.setString(2, ouFqnParam);
            stmt.setInt(3, I_CmsPrincipal.FLAG_GROUP_ROLE);

            res = stmt.executeQuery();

            while (res.next()) {
                groups.add(internalCreateGroup(res));
            }
        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, res);
        }
        return groups;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#readOrganizationalUnit(org.opencms.db.CmsDbContext, String)
     */
    public CmsOrganizationalUnit readOrganizationalUnit(CmsDbContext dbc, String ouFqn) throws CmsDataAccessException {

        try {
            CmsResource resource = m_driverManager.readResource(
                dbc,
                ORGUNIT_BASE_FOLDER + ouFqn,
                CmsResourceFilter.DEFAULT);
            return internalCreateOrgUnitFromResource(dbc, resource);
        } catch (CmsException e) {
            throw new CmsDataAccessException(e.getMessageContainer(), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#readUser(org.opencms.db.CmsDbContext, org.opencms.util.CmsUUID)
     */
    public CmsUser readUser(CmsDbContext dbc, CmsUUID id) throws CmsDataAccessException {

        PreparedStatement stmt = null;
        ResultSet res = null;
        CmsUser user = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_READ_BY_ID_1");

            stmt.setString(1, id.toString());
            res = stmt.executeQuery();

            // create new Cms user object
            if (res.next()) {
                user = internalCreateUser(res);
            } else {
                CmsMessageContainer message = Messages.get().container(Messages.ERR_NO_USER_WITH_ID_1, id);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(message.key());
                }
                throw new CmsDbEntryNotFoundException(message);
            }

            return user;
        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } catch (IOException e) {
            throw new CmsDbIoException(Messages.get().container(Messages.ERR_READING_USER_0), e);
        } catch (ClassNotFoundException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_READING_USER_0), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, res);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#readUser(org.opencms.db.CmsDbContext, java.lang.String)
     */
    public CmsUser readUser(CmsDbContext dbc, String userFqn) throws CmsDataAccessException {

        userFqn = internalAppendRootOrgUnit(dbc, userFqn);

        PreparedStatement stmt = null;
        ResultSet res = null;
        CmsUser user = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_READ_BY_NAME_2");
            stmt.setString(1, CmsOrganizationalUnit.getSimpleName(userFqn));
            stmt.setString(2, CmsOrganizationalUnit.getParentFqn(userFqn));

            res = stmt.executeQuery();

            if (res.next()) {
                user = internalCreateUser(res);
            } else {
                CmsMessageContainer message = org.opencms.db.Messages.get().container(
                    org.opencms.db.Messages.ERR_UNKNOWN_USER_1,
                    userFqn);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(message.key());
                }
                throw new CmsDbEntryNotFoundException(message);
            }
        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } catch (IOException e) {
            throw new CmsDbIoException(Messages.get().container(Messages.ERR_READING_USER_0), e);
        } catch (ClassNotFoundException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_READING_USER_0), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, res);
        }

        return user;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#readUser(org.opencms.db.CmsDbContext, java.lang.String, java.lang.String, String)
     */
    public CmsUser readUser(CmsDbContext dbc, String userFqn, String password, String remoteAddress)
    throws CmsDataAccessException, CmsPasswordEncryptionException {

        userFqn = internalAppendRootOrgUnit(dbc, userFqn);

        PreparedStatement stmt = null;
        ResultSet res = null;
        CmsUser user = null;
        Connection conn = null;
        try {
            conn = m_sqlManager.getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_READ_WITH_PWD_3");
            stmt.setString(1, CmsOrganizationalUnit.getSimpleName(userFqn));
            stmt.setString(2, CmsOrganizationalUnit.getParentFqn(userFqn));
            stmt.setString(3, OpenCms.getPasswordHandler().digest(password));
            res = stmt.executeQuery();

            // create new Cms user object
            if (res.next()) {
                user = internalCreateUser(res);
            } else {
                res.close();
                res = null;
                CmsMessageContainer message = org.opencms.db.Messages.get().container(
                    org.opencms.db.Messages.ERR_UNKNOWN_USER_1,
                    userFqn);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(message.key());
                }
                throw new CmsDbEntryNotFoundException(message);
            }

            return user;
        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } catch (IOException e) {
            throw new CmsDbIoException(Messages.get().container(Messages.ERR_READING_USER_0), e);
        } catch (ClassNotFoundException e) {
            throw new CmsDataAccessException(Messages.get().container(Messages.ERR_READING_USER_0), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, res);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#readUsersOfGroup(CmsDbContext, String, boolean)
     */
    public List readUsersOfGroup(CmsDbContext dbc, String groupFqn, boolean includeOtherOuUsers)
    throws CmsDataAccessException {

        groupFqn = internalAppendRootOrgUnit(dbc, groupFqn);
        String sqlQuery = "C_GROUPS_GET_USERS_OF_GROUP_2";
        if (!includeOtherOuUsers) {
            sqlQuery = "C_GROUPS_GET_ALL_USERS_OF_GROUP_2";
        }

        List users = new ArrayList();

        PreparedStatement stmt = null;
        ResultSet res = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatement(conn, sqlQuery);
            stmt.setString(1, CmsOrganizationalUnit.getSimpleName(groupFqn));
            stmt.setString(2, CmsOrganizationalUnit.getParentFqn(groupFqn));

            res = stmt.executeQuery();

            while (res.next()) {
                users.add(internalCreateUser(res));
            }
        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } catch (IOException e) {
            throw new CmsDbIoException(org.opencms.db.Messages.get().container(
                org.opencms.db.Messages.ERR_GET_USERS_OF_GROUP_1,
                groupFqn), e);
        } catch (ClassNotFoundException e) {
            throw new CmsDataAccessException(org.opencms.db.Messages.get().container(
                org.opencms.db.Messages.ERR_GET_USERS_OF_GROUP_1,
                groupFqn), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, res);
        }

        return users;
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#removeAccessControlEntries(org.opencms.db.CmsDbContext, org.opencms.file.CmsProject, org.opencms.util.CmsUUID)
     */
    public void removeAccessControlEntries(CmsDbContext dbc, CmsProject project, CmsUUID resource)
    throws CmsDataAccessException {

        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection(dbc, project.getId());
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_ACCESS_REMOVE_ALL_1");

            stmt.setString(1, resource.toString());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, null);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#removeAccessControlEntriesForPrincipal(org.opencms.db.CmsDbContext, org.opencms.file.CmsProject, org.opencms.file.CmsProject, org.opencms.util.CmsUUID)
     */
    public void removeAccessControlEntriesForPrincipal(
        CmsDbContext dbc,
        CmsProject project,
        CmsProject onlineProject,
        CmsUUID principal) throws CmsDataAccessException {

        PreparedStatement stmt = null;
        Connection conn = null;

        try {

            conn = m_sqlManager.getConnection(dbc, project.getId());
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_ACCESS_REMOVE_ALL_FOR_PRINCIPAL_1");

            stmt.setString(1, principal.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, null);
        }

        try {
            conn = m_sqlManager.getConnection(dbc, project.getId());
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_ACCESS_REMOVE_ALL_FOR_PRINCIPAL_ONLINE_1");

            stmt.setString(1, principal.toString());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(Messages.ERR_GENERIC_SQL_1, stmt), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, null);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#removeAccessControlEntry(org.opencms.db.CmsDbContext, org.opencms.file.CmsProject, org.opencms.util.CmsUUID, org.opencms.util.CmsUUID)
     */
    public void removeAccessControlEntry(CmsDbContext dbc, CmsProject project, CmsUUID resource, CmsUUID principal)
    throws CmsDataAccessException {

        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection(dbc, project.getId());
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_ACCESS_REMOVE_2");

            stmt.setString(1, resource.toString());
            stmt.setString(2, principal.toString());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, null);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#removeResourceFromOrganizationalUnit(org.opencms.db.CmsDbContext, org.opencms.security.CmsOrganizationalUnit, String)
     */
    public void removeResourceFromOrganizationalUnit(
        CmsDbContext dbc,
        CmsOrganizationalUnit orgUnit,
        String resourceName) throws CmsDataAccessException {

        try {
            // read the representing resource
            CmsResource ouResource = m_driverManager.readResource(dbc, orgUnit.getId(), CmsResourceFilter.ALL);

            // get the associated resources
            List vfsPaths = new ArrayList(internalResourcesForOrgUnit(dbc, ouResource));

            if (!resourceName.endsWith("/")) {
                resourceName += "/";
            }

            // check if associated
            if (!vfsPaths.contains(resourceName)) {
                throw new CmsDataAccessException(Messages.get().container(
                    Messages.ERR_ORGUNIT_DOESNOT_CONTAINS_RESOURCE_2,
                    orgUnit.getName(),
                    dbc.removeSiteRoot(resourceName)));
            }
            if (vfsPaths.size() == 1) {
                throw new CmsDataAccessException(Messages.get().container(
                    Messages.ERR_ORGUNIT_REMOVE_LAST_RESOURCE_2,
                    orgUnit.getName(),
                    dbc.removeSiteRoot(resourceName)));
            }

            // remove the resource
            CmsRelationFilter filter = CmsRelationFilter.SOURCES.filterPath(resourceName);
            m_driverManager.getVfsDriver().deleteRelations(dbc, dbc.currentProject().getId(), ouResource, filter);
            m_driverManager.getVfsDriver().deleteRelations(dbc, CmsProject.ONLINE_PROJECT_ID, ouResource, filter);
        } catch (CmsException e) {
            throw new CmsDataAccessException(e.getMessageContainer(), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#setUsersOrganizationalUnit(org.opencms.db.CmsDbContext, org.opencms.security.CmsOrganizationalUnit, org.opencms.file.CmsUser)
     */
    public void setUsersOrganizationalUnit(CmsDbContext dbc, CmsOrganizationalUnit orgUnit, CmsUser user)
    throws CmsDataAccessException {

        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = m_sqlManager.getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_SET_ORGUNIT_2");

            if (orgUnit == null) {
                stmt.setString(1, null);
            } else {
                stmt.setString(1, orgUnit.getName());
            }
            stmt.setString(2, user.getId().toString());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, null);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#writeAccessControlEntry(org.opencms.db.CmsDbContext, org.opencms.file.CmsProject, org.opencms.security.CmsAccessControlEntry)
     */
    public void writeAccessControlEntry(CmsDbContext dbc, CmsProject project, CmsAccessControlEntry acEntry)
    throws CmsDataAccessException {

        PreparedStatement stmt = null;
        Connection conn = null;
        ResultSet res = null;

        try {
            conn = m_sqlManager.getConnection(dbc, project.getId());
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_ACCESS_READ_ENTRY_2");

            stmt.setString(1, acEntry.getResource().toString());
            stmt.setString(2, acEntry.getPrincipal().toString());

            res = stmt.executeQuery();
            if (!res.next()) {
                m_driverManager.getUserDriver().createAccessControlEntry(
                    dbc,
                    project,
                    acEntry.getResource(),
                    acEntry.getPrincipal(),
                    acEntry.getAllowedPermissions(),
                    acEntry.getDeniedPermissions(),
                    acEntry.getFlags());
                return;
            }

            // otherwise update the already existing entry
            stmt = m_sqlManager.getPreparedStatement(conn, project, "C_ACCESS_UPDATE_5");

            stmt.setInt(1, acEntry.getAllowedPermissions());
            stmt.setInt(2, acEntry.getDeniedPermissions());
            stmt.setInt(3, acEntry.getFlags());
            stmt.setString(4, acEntry.getResource().toString());
            stmt.setString(5, acEntry.getPrincipal().toString());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, res);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#writeGroup(org.opencms.db.CmsDbContext, org.opencms.file.CmsGroup)
     */
    public void writeGroup(CmsDbContext dbc, CmsGroup group) throws CmsDataAccessException {

        PreparedStatement stmt = null;
        Connection conn = null;
        if (group != null) {
            try {
                conn = getSqlManager().getConnection(dbc);
                stmt = m_sqlManager.getPreparedStatement(conn, "C_GROUPS_WRITE_GROUP_4");

                stmt.setString(1, m_sqlManager.validateEmpty(group.getDescription()));
                stmt.setInt(2, group.getFlags());
                stmt.setString(3, group.getParentId().toString());
                stmt.setString(4, group.getId().toString());
                stmt.executeUpdate();

            } catch (SQLException e) {
                throw new CmsDbSqlException(Messages.get().container(
                    Messages.ERR_GENERIC_SQL_1,
                    CmsDbSqlException.getErrorQuery(stmt)), e);
            } finally {
                m_sqlManager.closeAll(dbc, conn, stmt, null);
            }
        } else {
            throw new CmsDbEntryNotFoundException(org.opencms.db.Messages.get().container(
                org.opencms.db.Messages.ERR_UNKNOWN_GROUP_1,
                "null"));
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#writeOrganizationalUnit(org.opencms.db.CmsDbContext, org.opencms.security.CmsOrganizationalUnit)
     */
    public void writeOrganizationalUnit(CmsDbContext dbc, CmsOrganizationalUnit organizationalUnit)
    throws CmsDataAccessException {

        // TODO: handle changed name/fqn
        int todo;

        // check if new parent/name is valid
        // move all user/group ous
        // move offline/online relations
        // move offline/online resource

        try {
            CmsResource resource = m_driverManager.readResource(
                dbc,
                organizationalUnit.getId(),
                CmsResourceFilter.DEFAULT);

            // write the properties
            internalWriteOrgUnitProperty(dbc, resource, new CmsProperty(
                ORGUNIT_PROPERTY_DESCRIPTION,
                organizationalUnit.getDescription(),
                null));

            // update the resource flags
            resource.setFlags(organizationalUnit.getFlags() | CmsResource.FLAG_INTERNAL);
            m_driverManager.writeResource(dbc, resource);
            resource.setState(CmsResource.STATE_UNCHANGED);
            m_driverManager.getVfsDriver().writeResource(
                dbc,
                dbc.currentProject(),
                resource,
                CmsDriverManager.NOTHING_CHANGED);

            if (!dbc.currentProject().isOnlineProject()) {
                // online persistence
                CmsProject onlineProject = m_driverManager.readProject(dbc, CmsProject.ONLINE_PROJECT_ID);
                resource.setState(CmsResource.STATE_UNCHANGED);
                m_driverManager.getVfsDriver().writeResource(
                    dbc,
                    onlineProject,
                    resource,
                    CmsDriverManager.NOTHING_CHANGED);
            }
        } catch (CmsException e) {
            throw new CmsDataAccessException(e.getMessageContainer(), e);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#writePassword(org.opencms.db.CmsDbContext, java.lang.String, java.lang.String, java.lang.String)
     */
    public void writePassword(CmsDbContext dbc, String userFqn, String oldPassword, String newPassword)
    throws CmsDataAccessException, CmsPasswordEncryptionException {

        userFqn = internalAppendRootOrgUnit(dbc, userFqn);

        PreparedStatement stmt = null;
        Connection conn = null;

        // TODO: if old password is not null, check if it is valid
        int todo;
        try {
            conn = getSqlManager().getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_SET_PWD_3");
            stmt.setString(1, OpenCms.getPasswordHandler().digest(newPassword));
            stmt.setString(2, CmsOrganizationalUnit.getSimpleName(userFqn));
            stmt.setString(3, CmsOrganizationalUnit.getParentFqn(userFqn));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, null);
        }
    }

    /**
     * @see org.opencms.db.I_CmsUserDriver#writeUser(org.opencms.db.CmsDbContext, org.opencms.file.CmsUser)
     */
    public void writeUser(CmsDbContext dbc, CmsUser user) throws CmsDataAccessException {

        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = getSqlManager().getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatement(conn, "C_USERS_WRITE_9");
            // write data to database
            stmt.setString(1, m_sqlManager.validateEmpty(user.getDescription()));
            stmt.setString(2, m_sqlManager.validateEmpty(user.getFirstname()));
            stmt.setString(3, m_sqlManager.validateEmpty(user.getLastname()));
            stmt.setString(4, m_sqlManager.validateEmpty(user.getEmail()));
            stmt.setLong(5, user.getLastlogin());
            stmt.setInt(6, user.getFlags());
            m_sqlManager.setBytes(stmt, 7, internalSerializeAdditionalUserInfo(user.getAdditionalInfo()));
            stmt.setString(8, m_sqlManager.validateEmpty(user.getAddress()));
            stmt.setString(9, user.getId().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } catch (IOException e) {
            throw new CmsDbIoException(
                Messages.get().container(Messages.ERR_SERIALIZING_USER_DATA_1, user.getName()),
                e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, null);
        }
    }

    /**
     * @see java.lang.Object#finalize()
     */
    protected void finalize() throws Throwable {

        destroy();
        super.finalize();
    }

    /**
     * This method appends the root organizational unit to the name if the
     * name does not contains any organizational unit name.<p>
     * 
     * This is needed to ensure backward compatibility.<p>
     * 
     * This method also checks that the organizational unit exists.<p> 
     * 
     * @param dbc the current database context
     * @param name the group/user name to check
     * 
     * @return the new name
     * 
     * @throws CmsDataAccessException 
     */
    protected String internalAppendRootOrgUnit(CmsDbContext dbc, String name) throws CmsDataAccessException {

        if (CmsStringUtil.isEmptyOrWhitespaceOnly(name)) {
            return null;
        }
        // if no ou given, append root
        if (!name.startsWith("/")) {
            return "/" + name;
        }
        // check the ou, but not during initialization
        if (dbc.getRequestContext() != null) {
            // check the ou, but not during set up
            boolean check = false;
            try {
                m_driverManager.readResource(dbc, ORGUNIT_BASE_FOLDER, CmsResourceFilter.ALL);
                check = true;
            } catch (CmsException e) {
                // ignore
            }
            if (check) {
                readOrganizationalUnit(dbc, CmsOrganizationalUnit.getParentFqn(name)).getName();
            }
        }
        return name;
    }

    /**
     * Internal helper method to create an access control entry from a database record.<p>
     * 
     * @param res resultset of the current query
     * 
     * @return a new {@link CmsAccessControlEntry} initialized with the values from the current database record
     * 
     * @throws SQLException if something goes wrong
     */
    protected CmsAccessControlEntry internalCreateAce(ResultSet res) throws SQLException {

        return internalCreateAce(res, new CmsUUID(res.getString(m_sqlManager.readQuery("C_ACCESS_RESOURCE_ID_0"))));
    }

    /**
     * Internal helper method to create an access control entry from a database record.<p>
     * 
     * @param res resultset of the current query
     * @param newId the id of the new access control entry
     * 
     * @return a new {@link CmsAccessControlEntry} initialized with the values from the current database record
     * 
     * @throws SQLException if something goes wrong
     */
    protected CmsAccessControlEntry internalCreateAce(ResultSet res, CmsUUID newId) throws SQLException {

        return new CmsAccessControlEntry(
            newId,
            new CmsUUID(res.getString(m_sqlManager.readQuery("C_ACCESS_PRINCIPAL_ID_0"))),
            res.getInt(m_sqlManager.readQuery("C_ACCESS_ACCESS_ALLOWED_0")),
            res.getInt(m_sqlManager.readQuery("C_ACCESS_ACCESS_DENIED_0")),
            res.getInt(m_sqlManager.readQuery("C_ACCESS_ACCESS_FLAGS_0")));
    }

    /**
     * Semi-constructor to create a {@link CmsGroup} instance from a JDBC result set.
     * 
     * @param res the JDBC ResultSet
     * 
     * @return CmsGroup the new CmsGroup object
     * 
     * @throws SQLException in case the result set does not include a requested table attribute
     */
    protected CmsGroup internalCreateGroup(ResultSet res) throws SQLException {

        String ou = res.getString(m_sqlManager.readQuery("C_GROUPS_GROUP_OU_0"));
        if (CmsStringUtil.isEmptyOrWhitespaceOnly(ou)) {
            // backward compatibility
            ou = "/";
        }
        return new CmsGroup(
            new CmsUUID(res.getString(m_sqlManager.readQuery("C_GROUPS_GROUP_ID_0"))),
            new CmsUUID(res.getString(m_sqlManager.readQuery("C_GROUPS_PARENT_GROUP_ID_0"))),
            ou + res.getString(m_sqlManager.readQuery("C_GROUPS_GROUP_NAME_0")),
            res.getString(m_sqlManager.readQuery("C_GROUPS_GROUP_DESCRIPTION_0")),
            res.getInt(m_sqlManager.readQuery("C_GROUPS_GROUP_FLAGS_0")));
    }

    /**
     * Returns the organizational unit represented by the given resource.<p>
     * 
     * @param dbc the current db context
     * @param resource the resource that represents an organizational unit
     * 
     * @return the organizational unit represented by the given resource
     * 
     * @throws CmsException if soemthing goes wrong
     */
    protected CmsOrganizationalUnit internalCreateOrgUnitFromResource(CmsDbContext dbc, CmsResource resource)
    throws CmsException {

        if (!resource.getRootPath().startsWith(ORGUNIT_BASE_FOLDER)) {
            throw new CmsDataAccessException(Messages.get().container(
                Messages.ERR_READ_ORGUNIT_1,
                resource.getRootPath()));
        }
        // get the data
        String name = resource.getRootPath().substring(ORGUNIT_BASE_FOLDER.length());
        if (!name.endsWith("/")) {
            name += "/";
        }
        String description = m_driverManager.readPropertyObject(dbc, resource, ORGUNIT_PROPERTY_DESCRIPTION, false).getStructureValue();
        int flags = (resource.getFlags() & ~CmsResource.FLAG_INTERNAL); // remove the internal flag
        // create the object
        return new CmsOrganizationalUnit(resource.getStructureId(), name, description, flags);
    }

    /**
     * Creates a folder with the given path an properties, offline AND online.<p>
     * 
     * @param dbc the current database context
     * @param path the path to create the folder
     * @param flags the resource flags
     * 
     * @return the new created offline folder
     * 
     * @throws CmsException if something goes wrong
     */
    protected CmsResource internalCreateResourceForOrgUnit(CmsDbContext dbc, String path, int flags)
    throws CmsException {

        // create the offline folder 
        CmsResource resource = new CmsFolder(
            new CmsUUID(),
            new CmsUUID(),
            path,
            CmsResourceTypeFolder.RESOURCE_TYPE_ID,
            (CmsResource.FLAG_INTERNAL | flags),
            dbc.currentProject().getId(),
            CmsResource.STATE_NEW,
            0,
            dbc.currentUser().getId(),
            0,
            dbc.currentUser().getId(),
            1,
            CmsResource.DATE_RELEASED_DEFAULT,
            CmsResource.DATE_EXPIRED_DEFAULT);

        m_driverManager.getVfsDriver().createResource(dbc, dbc.currentProject(), resource, null);
        resource.setState(CmsResource.STATE_UNCHANGED);
        m_driverManager.getVfsDriver().writeResource(
            dbc,
            dbc.currentProject(),
            resource,
            CmsDriverManager.NOTHING_CHANGED);

        if (!dbc.currentProject().isOnlineProject()) {
            // online persistence
            CmsProject onlineProject = m_driverManager.readProject(dbc, CmsProject.ONLINE_PROJECT_ID);
            m_driverManager.getVfsDriver().createResource(dbc, onlineProject, resource, null);
            resource.setState(CmsResource.STATE_UNCHANGED);
            m_driverManager.getVfsDriver().writeResource(dbc, onlineProject, resource, CmsDriverManager.NOTHING_CHANGED);
        }
        return resource;
    }

    /**
     * Semi-constructor to create a {@link CmsUser} instance from a JDBC result set.
     * 
     * @param res the JDBC ResultSet
     * 
     * @return CmsUser the new CmsUser object
     * 
     * @throws SQLException in case the result set does not include a requested table attribute
     * @throws IOException if there is an error in deserializing the user info
     * @throws ClassNotFoundException if there is an error in deserializing the user info
     */
    protected CmsUser internalCreateUser(ResultSet res) throws SQLException, IOException, ClassNotFoundException {

        String userName = res.getString(m_sqlManager.readQuery("C_USERS_USER_NAME_0"));

        // deserialize the additional userinfo hash
        ByteArrayInputStream bin = new ByteArrayInputStream(m_sqlManager.getBytes(
            res,
            m_sqlManager.readQuery("C_USERS_USER_INFO_0")));
        ObjectInputStream oin = new ObjectInputStream(bin);

        Map info;
        // ensure the user is read even if it's additional infos are defect
        try {
            info = (Map)oin.readObject();
        } catch (IOException e) {
            CmsMessageContainer message = Messages.get().container(Messages.ERR_READING_ADDITIONAL_INFO_1, userName);
            LOG.error(message.key(), e);

            info = new HashMap();
        }

        String ou = res.getString(m_sqlManager.readQuery("C_USERS_USER_OU_0"));
        if (CmsStringUtil.isEmptyOrWhitespaceOnly(ou)) {
            // backward compatibility
            ou = "/";
        }
        return new CmsUser(
            new CmsUUID(res.getString(m_sqlManager.readQuery("C_USERS_USER_ID_0"))),
            ou + userName,
            res.getString(m_sqlManager.readQuery("C_USERS_USER_PASSWORD_0")),
            res.getString(m_sqlManager.readQuery("C_USERS_USER_DESCRIPTION_0")),
            res.getString(m_sqlManager.readQuery("C_USERS_USER_FIRSTNAME_0")),
            res.getString(m_sqlManager.readQuery("C_USERS_USER_LASTNAME_0")),
            res.getString(m_sqlManager.readQuery("C_USERS_USER_EMAIL_0")),
            res.getLong(m_sqlManager.readQuery("C_USERS_USER_LASTLOGIN_0")),
            res.getInt(m_sqlManager.readQuery("C_USERS_USER_FLAGS_0")),
            info,
            res.getString(m_sqlManager.readQuery("C_USERS_USER_ADDRESS_0")));
    }

    /**
     * Deletes a resource representing a organizational unit, offline AND online.<p>
     * 
     * @param dbc the current database context
     * @param resource the resource to delete
     * 
     * @throws CmsException if soemthing goes wrong
     */
    protected void internalDeleteOrgUnitResource(CmsDbContext dbc, CmsResource resource) throws CmsException {

        CmsRelationFilter filter = CmsRelationFilter.TARGETS.filterResource(resource);

        // first the online version
        if (!dbc.currentProject().isOnlineProject()) {
            // online persistence
            CmsProject project = dbc.currentProject();
            dbc.getRequestContext().setCurrentProject(m_driverManager.readProject(dbc, CmsProject.ONLINE_PROJECT_ID));

            try {
                // delete properties
                m_driverManager.getVfsDriver().deletePropertyObjects(
                    dbc,
                    CmsProject.ONLINE_PROJECT_ID,
                    resource,
                    CmsProperty.DELETE_OPTION_DELETE_STRUCTURE_AND_RESOURCE_VALUES);

                // remove the online folder only if it is really deleted offline
                m_driverManager.getVfsDriver().removeFolder(dbc, dbc.currentProject(), resource);

                // remove ACL
                m_driverManager.getUserDriver().removeAccessControlEntries(
                    dbc,
                    dbc.currentProject(),
                    resource.getResourceId());

                // delete relations
                m_driverManager.getVfsDriver().deleteRelations(
                    dbc,
                    dbc.getRequestContext().currentProject().getId(),
                    null,
                    filter);
            } finally {
                dbc.getRequestContext().setCurrentProject(project);
            }
        }
        // delete properties
        m_driverManager.getVfsDriver().deletePropertyObjects(
            dbc,
            CmsProject.ONLINE_PROJECT_ID,
            resource,
            CmsProperty.DELETE_OPTION_DELETE_STRUCTURE_AND_RESOURCE_VALUES);

        // remove the online folder only if it is really deleted offline
        m_driverManager.getVfsDriver().removeFolder(dbc, dbc.currentProject(), resource);

        // remove ACL
        m_driverManager.getUserDriver().removeAccessControlEntries(dbc, dbc.currentProject(), resource.getResourceId());

        // delete relations
        m_driverManager.getVfsDriver().deleteRelations(
            dbc,
            dbc.getRequestContext().currentProject().getId(),
            null,
            filter);

        // fire event
        OpenCms.fireCmsEvent(new CmsEvent(I_CmsEventListener.EVENT_RESOURCE_DELETED, Collections.singletonMap(
            "resources",
            Collections.singletonList(resource))));
    }

    /**
     * Returns the folder for the given organizational units, or the base folder if <code>null</code>.<p>
     * 
     * The base folder will be created if it does not exist.<p> 
     * 
     * @param dbc the current db context
     * @param orgUnit the organizational unit to get the folder for
     * 
     * @return the base folder for organizational units
     *  
     * @throws CmsException if something goes wrong
     */
    protected CmsResource internalOrgUnitFolder(CmsDbContext dbc, CmsOrganizationalUnit orgUnit) throws CmsException {

        if (orgUnit != null) {
            return m_driverManager.readResource(dbc, orgUnit.getId(), CmsResourceFilter.DEFAULT);
        } else {
            return null;
        }
    }

    /**
     * Returns the list of root paths associated to the organizational unit represented by the given resource.<p>
     * 
     * @param dbc the current db context
     * @param ouResource the resource that represents the organizational unit to get the resources for
     * 
     * @return the list of associated resource names
     * 
     * @throws CmsException if something goes wrong
     */
    protected List internalResourcesForOrgUnit(CmsDbContext dbc, CmsResource ouResource) throws CmsException {

        List relations = m_driverManager.getRelationsForResource(dbc, ouResource, CmsRelationFilter.TARGETS);
        List paths = new ArrayList();
        Iterator it = relations.iterator();
        while (it.hasNext()) {
            CmsRelation relation = (CmsRelation)it.next();
            paths.add(relation.getTargetPath());
        }
        return paths;
    }

    /**
     * Serialize additional user information to write it as byte array in the database.<p>
     * 
     * @param additionalUserInfo the HashTable with additional information
     * @return byte[] the byte array which is written to the db
     * @throws IOException if something goes wrong
     */
    protected byte[] internalSerializeAdditionalUserInfo(Map additionalUserInfo) throws IOException {

        // serialize the hashtable
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(additionalUserInfo != null ? new Hashtable(additionalUserInfo) : null);
        oout.close();

        return bout.toByteArray();
    }

    /**
     * Validates the given root path to be in the scope of the resources of the given organizational unit.<p>
     * 
     * @param dbc the current db context
     * @param orgUnit the organizational unit
     * @param rootPath the root path to check
     * 
     * @throws CmsException if something goes wrong
     */
    protected void internalValidateResourceForOrgUnit(CmsDbContext dbc, CmsOrganizationalUnit orgUnit, String rootPath)
    throws CmsException {

        CmsResource parentResource = m_driverManager.readResource(dbc, orgUnit.getId(), CmsResourceFilter.ALL);
        // assume not in scope
        boolean found = false;
        // iterate parent paths
        Iterator itParentPaths = internalResourcesForOrgUnit(dbc, parentResource).iterator();
        // until the given resource is found in scope
        while (!found && itParentPaths.hasNext()) {
            String parentPath = (String)itParentPaths.next();
            // check the scope
            if (rootPath.startsWith(parentPath)) {
                found = true;
            }
        }
        // if not in scope throw exception
        if (!found) {
            throw new CmsException(Messages.get().container(
                Messages.ERR_PATH_NOT_IN_PARENT_ORGUNIT_SCOPE_2,
                orgUnit.getName(),
                dbc.removeSiteRoot(rootPath)));
        }
    }

    /**
     * Checks if a user is member of a group.<p>
     * 
     * @param dbc the database context
     * @param userId the id of the user to check
     * @param groupId the id of the group to check
     *
     * @return true if user is member of group
     * 
     * @throws CmsDataAccessException if operation was not succesful
     */
    protected boolean internalValidateUserInGroup(CmsDbContext dbc, CmsUUID userId, CmsUUID groupId)
    throws CmsDataAccessException {

        boolean userInGroup = false;
        PreparedStatement stmt = null;
        ResultSet res = null;
        Connection conn = null;

        try {
            conn = getSqlManager().getConnection(dbc);
            stmt = m_sqlManager.getPreparedStatement(conn, "C_GROUPS_USER_IN_GROUP_2");

            stmt.setString(1, groupId.toString());
            stmt.setString(2, userId.toString());
            res = stmt.executeQuery();
            if (res.next()) {
                userInGroup = true;
            }
        } catch (SQLException e) {
            throw new CmsDbSqlException(Messages.get().container(
                Messages.ERR_GENERIC_SQL_1,
                CmsDbSqlException.getErrorQuery(stmt)), e);
        } finally {
            m_sqlManager.closeAll(dbc, conn, stmt, res);
        }

        return userInGroup;
    }

    /**
     * Writes a property for an orgnaizational unit resource, online AND offline.<p>
     * 
     * @param dbc the current database context
     * @param resource the resource representing the organizational unit
     * @param property the property to write
     * 
     * @throws CmsException if something goes wrong
     */
    protected void internalWriteOrgUnitProperty(CmsDbContext dbc, CmsResource resource, CmsProperty property)
    throws CmsException {

        // write the property
        m_driverManager.writePropertyObject(dbc, resource, property);
        resource.setState(CmsResource.STATE_UNCHANGED);
        m_driverManager.getVfsDriver().writeResource(
            dbc,
            dbc.currentProject(),
            resource,
            CmsDriverManager.NOTHING_CHANGED);

        // online persistence
        CmsProject project = dbc.currentProject();
        dbc.getRequestContext().setCurrentProject(m_driverManager.readProject(dbc, CmsProject.ONLINE_PROJECT_ID));
        try {
            m_driverManager.writePropertyObject(dbc, resource, property); // assume the resource is identical in both projects
            resource.setState(CmsResource.STATE_UNCHANGED);
            m_driverManager.getVfsDriver().writeResource(
                dbc,
                dbc.currentProject(),
                resource,
                CmsDriverManager.NOTHING_CHANGED);
        } finally {
            dbc.getRequestContext().setCurrentProject(project);
        }
    }
}
