package com.opencms.core;

/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/core/Attic/CmsShellCommands.java,v $
 * Date   : $Date: 2000/11/01 18:15:32 $
 * Version: $Revision: 1.14 $
 *
 * Copyright (C) 2000  The OpenCms Group 
 * 
 * This File is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * For further information about OpenCms, please see the
 * OpenCms Website: http://www.opencms.com
 * 
 * You should have received a copy of the GNU General Public License
 * long with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import java.util.*;
import java.io.*;
import com.opencms.file.*;
import java.lang.reflect.*;
import source.org.apache.java.util.*;

/**
 * This class is a commadnlineinterface for the opencms. It can be used to test
 * the opencms, and for the initial setup. It uses the OpenCms-Object.
 * 
 * @author Andreas Schouten
 * @author Anders Fugmann
 * @version $Revision: 1.14 $ $Date: 2000/11/01 18:15:32 $
 */
public class CmsShellCommands implements I_CmsConstants {

	/**
	 * The resource broker to get access to the cms.
	 */
	private CmsObject m_cms;

	/**
	 * The open-cms.
	 */
	private A_OpenCms m_openCms;

/**
 * Insert the method's description here.
 * Creation date: (10/05/00 %r)
 * @author: 
 */
public CmsShellCommands(String[] args, A_OpenCms openCms,CmsObject cms) throws Exception
{
	m_openCms = openCms;
	m_cms = cms;

	m_openCms.initUser(m_cms, null, null, C_USER_GUEST, C_GROUP_GUEST, C_PROJECT_ONLINE_ID);
	// print the version-string
	version();
	copyright();
	printHelpText();

}
	/**
	 * Tests if the user can access the project.
	 * 
	 * @param id the id of the project.
	 */
	public void accessProject(String id) {
		try {
			int projectId = Integer.parseInt(id);
			System.out.println( m_cms.accessProject(projectId) );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}		
	}
	/**
	 * Tests if the user can write the resource.
	 * 
	 * @param resource the name of the resource.
	 */
	public void accessWrite(String resource) {
		try {
				CmsResource res = m_cms.readFileHeader(resource);
				System.out.println( m_cms.accessWrite(res) );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}		
	}
	/**
	* adds a file extension.
	* @param extension a file extension, e.g. 'html'
	* @param resourceType, name of a resource type like 'page'
	*/
	
	public void addFileExtension(String extension, String resourceType){
		try {
			m_cms.addFileExtension(extension, resourceType);
		} catch (Exception exc) {
			CmsShell.printException(exc);	
		}
	}
	/**
	 * Adds a Group to the cms.
	 * 
	 * @param name The name of the new group.
	 * @param description The description for the new group.
	 */
	public void addGroup(String name, String description) {
		try {
			m_cms.addGroup( name, description, C_FLAG_ENABLED, null );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Adds a Group to the cms.
	 * 
	 * @param name The name of the new group.
	 * @param description The description for the new group.
	 * @int flags The flags for the new group.
	 * @param name The name of the parent group (or null).
	 */
	public void addGroup(String name, String description, String flags, String parent) {
		try {
			m_cms.addGroup( name, description, Integer.parseInt(flags), parent );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Adds a CmsResourceType.
	 * 
	 * @param resourceType the name of the resource to get.
	 * @param launcherType the launcherType-id
	 * @param launcherClass the name of the launcher-class normaly ""
	 */
	public void addResourceType(String resourceType, String launcherType, 
								String launcherClass) {		
		try {
			System.out.println( m_cms.addResourceType(resourceType, 
													  Integer.parseInt(launcherType), 
													  launcherClass) );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}		
	}
	/**
	 * Adds a user to the cms.
	 * 
	 * @param name The new name for the user.
	 * @param password The new password for the user.
	 * @param group The default groupname for the user.
	 * @param description The description for the user.
	 */
	public void addUser( String name, String password, 
						 String group, String description) {
		try {
			System.out.println(m_cms.addUser( name, password, group, 
											  description, new Hashtable(), 
											  C_FLAG_ENABLED) );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Adds a user to the cms.
	 * 
	 * @param name The new name for the user.
	 * @param password The new password for the user.
	 * @param group The default groupname for the user.
	 * @param description The description for the user.
	 * @param flags The flags for the user.
	 */
	public void addUser( String name, String password, 
						 String group, String description, String flags) {
		try {
			System.out.println(m_cms.addUser( name, password, group, 
											  description, new Hashtable(), 
											  Integer.parseInt(flags)) );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Adds a user to the cms.
	 * 
	 * @param name The new name for the user.
	 * @param password The new password for the user.
	 * @param group The default groupname for the user.
	 * @param description The description for the user.
	 * @param flags The flags for the user.
	 */
	public void addUser( String name, String password, 
						 String group, String description,
						 String firstname, String lastname, String email) {
		try {
			CmsUser user = m_cms.addUser( name, password, group, 
											description, new Hashtable(), C_FLAG_ENABLED);
			user.setEmail(email);
			user.setFirstname(firstname);
			user.setLastname(lastname);
			m_cms.writeUser(user);
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Adds a user to a group.
	 *
	 * @param username The name of the user that is to be added to the group.
	 * @param groupname The name of the group.
	 */	
	public void addUserToGroup(String username, String groupname) {
		try {
			m_cms.addUserToGroup( username, groupname );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Returns the anonymous user object.
	 */
	public void anonymousUser() {
		try {
			System.out.println( m_cms.anonymousUser() );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Changes the group for this resource<BR/>
	 * 
	 * The user may change this, if he is admin of the resource.
	 * 
	 * @param filename The complete path to the resource.
	 * @param newGroup The new of the new group for this resource.
	 */
	public void chgrp(String filename, String newGroup) {
		try {
			m_cms.chgrp(filename, newGroup);
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Changes the flags for this resource<BR/>
	 * 
	 * The user may change the flags, if he is admin of the resource.
	 * 
	 * @param filename The complete path to the resource.
	 * @param flags The new flags for the resource.
	 */
	public void chmod(String filename, String flags) {
		try {
			m_cms.chmod(filename, Integer.parseInt(flags));
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Changes the owner for this resource<BR/>
	 * 
	 * The user may change this, if he is admin of the resource.
	 * 
	 * @param filename The complete path to the resource.
	 * @param newOwner The name of the new owner for this resource.
	 */
	public void chown(String filename, String newOwner) {
		try {
			m_cms.chown(filename, newOwner);
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Changes the resourcetype for this resource<BR/>
	 * 
	 * The user may change this, if he is admin of the resource.
	 * 
	 * @param filename The complete path to the resource.
	 * @param newType The name of the new resourcetype for this resource.
	 */
	public void chtype(String filename, String newType) {
		try {
			m_cms.chtype(filename, newType);
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Copies the file.
	 * 
	 * @param source The complete path of the sourcefile.
	 * @param destination The complete path of the destination.
	 * 
	 * @exception CmsException will be thrown, if the file couldn't be copied. 
	 * The CmsException will also be thrown, if the user has not the rights 
	 * for this resource.
	 */	
	public void copyFile(String source, String destination) {
		try {
			m_cms.copyFile(source, destination);
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Copies a resource from the online project to a new, specified project.<br>
	 * Copying a resource will copy the file header or folder into the specified 
	 * offline project and set its state to UNCHANGED.
	 * 
	 * @param resource The name of the resource.
	 */
	 public void copyResourceToProject(String resource) {
		try {
			m_cms.copyResourceToProject(resource);
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
/**
 * Insert the method's description here.
 * Creation date: (06-10-2000 08:58:47)
 * @param fromProjectId java.lang.String
 * @param resource java.lang.String
 * @author Martin Langelund
 */
public void copyResourceToProject(String fromProjectId, String resource)
{
	try
	{
		m_cms.copyResourceToProject(m_cms.readProject(Integer.parseInt(fromProjectId)), resource);
	}
	catch (Exception e)
	{
		CmsShell.printException(e);
	}
}
	/**
	 * Returns a copyright-string for this OpenCms.
	 */
	 public void copyright() {
		 String[] copy = m_cms.copyright();
		 for(int i = 0; i < copy.length; i++) {
			 System.out.println(copy[i]);
		 }
	 }
	/**
	 * Creates a new folder.
	 * 
	 * @param folder The complete path to the folder in which the new folder 
	 * will be created.
	 * @param newFolderName The name of the new folder (No pathinformation allowed).
	 */
	public void createFolder(String folder, String newFolderName) {
		try {
			System.out.println( m_cms.createFolder(folder, newFolderName) );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}		
	}
/**
 * This method creates a new module in the repository.
 *
 * @param String modulename the name of the module.
 * @param String niceModulename another name of the module.
 * @param String description the description of the module.
 * @param String author the name of the author.
 * @param String createDate the creation date of the module
 * @param String version the version number of the module.
 */
public void createModule(String modulename, String niceModulename, String description, String author, String createDate, String version) {
	// create the module
	try {
		I_CmsRegistry reg = m_cms.getRegistry();
		int ver = Integer.parseInt(version);
		long date = Long.parseLong(createDate);
		reg.createModule(modulename, niceModulename, description, author, date, ver);
	} catch (Exception exc) {
		CmsShell.printException(exc);
	}
}
	/**
	 * Creates a project.
	 * 
	 * @param name The name of the project to read.
	 * @param description The description for the new project.
	 * @param groupname the name of the group to be set.
	 */
	public void createProject(String name, String description, String groupname,
							  String managergroupname) {
		try {
			System.out.println( m_cms.createProject(name, description, groupname, managergroupname).toString() );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}		
	}
	/**
	 * Creates the propertydefinition for the resource type.<BR/>
	 * 
	 * @param name The name of the propertydefinition to overwrite.
	 * @param resourcetype The name of the resource-type for the propertydefinition.
	 * @param type The type of the propertydefinition (normal|mandatory|optional)
	 * 
	 * @exception CmsException Throws CmsException if something goes wrong.
	 */
	public void createPropertydefinition(String name, String resourcetype, String type)
		throws CmsException {
		try {
			System.out.println( m_cms.createPropertydefinition(name, resourcetype, 
														   Integer.parseInt(type)) );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}		
	}
	/**
	 * Deletes all propertyinformation for a file or folder.
	 * 
	 * @param resource The name of the resource of which the propertyinformations 
	 * have to be deleted.
	 */
	public void deleteAllProperties(String resource) {
		try {
			m_cms.deleteAllProperties(resource);
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Deletes the file.
	 * 
	 * @param filename The complete path of the file.
	 */	
	public void deleteFile(String filename) {
		try {
			m_cms.deleteFile(filename);
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Deletes the folder.
	 * 
	 * @param foldername The complete path of the folder.
	 */	
	 public void deleteFolder(String foldername) {
		try {
			m_cms.deleteFolder(foldername);
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	 }
	/**
	 * Delete a group from the Cms.<BR/>
	 * 
	 * @param delgroup The name of the group that is to be deleted.
	 */	
	public void deleteGroup(String delgroup) {
		try {
			m_cms.deleteGroup( delgroup );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Deletes a module from the cms.
	 * 
	 * @param module The name of module to delete.
	 */
	public void deleteModule(String module) {
		try {
			I_CmsRegistry reg = m_cms.getRegistry();
			reg.deleteModule(module, new Vector());
		} catch (Exception exc) {
			CmsShell.printException(exc);
		}
	}
/**
 * This method deletes the view for an module.
 *
 * @param String the name of the module.
 */
public void deleteModuleView(String modulename) {
	try {
		I_CmsRegistry reg = m_cms.getRegistry();
		reg.deleteModuleView(modulename);
	} catch (Exception exc) {
		CmsShell.printException(exc);
	}
}
	/**
	 * Deletes a project.
	 * 
	 * @param id The id of the project to delete.
	 */
	public void deleteProject(String id) {
		try {
			m_cms.deleteProject(Integer.parseInt(id));
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}		
	}
	/**
	 * Deletes a propertyinformation for a file or folder.
	 * 
	 * @param resourcename The resource-name of which the propertyinformation has to be delteted.
	 * @param property The propertydefinition-name of which the propertyinformation has to be set.
	 */
	public void deleteProperty(String resourcename, String property) {
		try {
			m_cms.deleteProperty(resourcename, property);
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Delete the propertydefinition for the resource type.<BR/>
	 * 
	 * @param name The name of the propertydefinition to overwrite.
	 * @param resourcetype The name of the resource-type for the propertydefinition.
	 */
	public void deletepropertydefinition(String name, String resourcetype) {
		try {
			m_cms.deletePropertydefinition(name, resourcetype);
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/** 
	 * Deletes a user from the Cms.
	 * 
	 * @param name The name of the user to be deleted.
	 */
	public void deleteUser( String name ) {
		try {
			m_cms.deleteUser( name );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Echos the input to output.
	 * 
	 * @param echo The echo to be written to output.
	 */
	public void echo(String echo) {
		System.out.println(echo);
	}
	/**
	 * Exits the commandline-interface
	 */
	public void exit() {
		
		try {
			m_openCms.destroy();
		} catch (CmsException e) {
		   e.printStackTrace();
		}        

		System.exit(0);
	}
	/**
	 * Exports cms-resources to zip. In the zip-file the system - path will be included.
	 * 
	 * @param exportFile the name (absolute Path) of the export resource (zip)
	 * 
	 * @exception Throws CmsException if something goes wrong.
	 */
	public void exportAllResources(String exportFile)
		throws CmsException {
		// export the resources
		String [] exportPaths = {C_ROOT};
		try {
			m_cms.exportResources(exportFile, exportPaths , false);
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
/**
 * Exports a module.
 * 
 * @param modulename the name of the module to export
 * @param resource the folder to export.
 * @param filename the name of the file to export to.
 */
public void exportModule(String modulename, String resource, String filename) {
	try {
		String[] resources = {resource};
		I_CmsRegistry reg = m_cms.getRegistry();
		reg.exportModule(modulename, resources, filename);
	} catch (Exception exc) {
		CmsShell.printException(exc);
	}
}
	/**
	 * Exports cms-resources to zip.
	 * 
	 * @param exportFile the name (absolute Path) of the export resource (zip)
	 * @param pathList the names (absolute Path) of folders from which should be exported
	 *			separated by semicolons
	 * 
	 * @exception Throws CmsException if something goes wrong.
	 */
	public void exportResources(String exportFile, String pathList)
		throws CmsException {
		// export the resources
		StringTokenizer tok = new StringTokenizer(pathList, ";");
		Vector paths = new Vector();
		while (tok.hasMoreTokens()) {
			paths.addElement(tok.nextToken());
		} 
		String exportPaths[]= new String[paths.size()];
		for (int i=0; i< paths.size(); i++) {
			exportPaths[i] = (String) paths.elementAt(i);
		} 
		boolean excludeSystem = true; 
		if (pathList.startsWith("/system/") || (pathList.indexOf(";/system/") > -1)) {
			excludeSystem = false;
		}
		try {
			m_cms.exportResources(exportFile, exportPaths, excludeSystem);
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Returns all projects, which the user may access.
	 * 
	 * @return a Vector of projects.
	 */
	public void getAllAccessibleProjects() {
		try {
			Vector projects = m_cms.getAllAccessibleProjects();
			for( int i = 0; i < projects.size(); i++ ) {
				System.out.println( (CmsProject)projects.elementAt(i) );
			}
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}		
	}
	/**
	 * Returns  all I_CmsResourceTypes.
	 */
	public void getAllResourceTypes() {
		try {
			Hashtable resourceTypes = m_cms.getAllResourceTypes();
			Enumeration keys = resourceTypes.keys();
			
			while(keys.hasMoreElements()) {
				System.out.println(resourceTypes.get(keys.nextElement()));
			}
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}		
	}
	/**
	 * Returns all child groups of a group<P/>
	 * 
	 * @param groupname The name of the group.
	 */
	public void getChild(String groupname) {
		try {
			Vector groups = m_cms.getChild(groupname);
			for( int i = 0; i < groups.size(); i++ ) {
				System.out.println( (CmsGroup)groups.elementAt(i) );
			}
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Returns all child groups of a group<P/>
	 * This method also returns all sub-child groups of the current group.
	 * 
	 * @param groupname The name of the group.
	 */
	public void getChilds(String groupname) {
		try {
			Vector groups = m_cms.getChilds(groupname);
			for( int i = 0; i < groups.size(); i++ ) {
				System.out.println( (CmsGroup)groups.elementAt(i) );
			}
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Returns the current project for the user.
	 */
	public void getCurrentProject() {
		System.out.println(m_cms.getRequestContext().currentProject().toString());
	}
	/**
	 * Returns a Vector with all subfiles.<BR/>
	 * 
	 * @param foldername the complete path to the folder.
	 * 
	 * @return subfiles A Vector with all subfiles for the overgiven folder.
	 * 
	 * @exception CmsException will be thrown, if the user has not the rights 
	 * for this resource.
	 */
	public void getFilesInFolder(String foldername) {
		try {
			Vector files = m_cms.getFilesInFolder(foldername);
			for( int i = 0; i < files.size(); i++ ) {
				System.out.println( (CmsFile)files.elementAt(i) );
			}
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * This method can be called, to determine if the file-system was changed 
	 * in the past. A module can compare its previosly stored number with this
	 * returned number. If they differ, a change was made.
	 * 
	 * @return the number of file-system-changes.
	 */
	 public void getFileSystemChanges() {
		System.out.println( m_cms.getFileSystemChanges() );
	 }
	/**
	 * Returns all users of the cms.
	 */
	public void getGroups() {
		try {
			Vector groups = m_cms.getGroups();
			for( int i = 0; i < groups.size(); i++ ) {
				System.out.println( (CmsGroup)groups.elementAt(i) );
			}
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Returns all groups of a user.
	 * 
	 * @param username The name of the user.
	 */
	public void getGroupsOfUser(String username) {
		try {
			Vector groups = m_cms.getGroupsOfUser(username);
			for( int i = 0; i < groups.size(); i++ ) {
				System.out.println( (CmsGroup)groups.elementAt(i) );
			}
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Prints out all files for a module.
	 * 
	 * @param moduleZip The file-name of module to import.
	 */
	public void getModuleFiles(String name) {
		try {
			I_CmsRegistry reg = m_cms.getRegistry();
			Vector names = new Vector();
			Vector codes = new Vector();
			reg.getModuleFiles(name, names, codes);
			for (int i = 0; i < names.size(); i++) {
				System.out.print(names.elementAt(i));
				System.out.print(" -> ");
				System.out.println(codes.elementAt(i));
			}
		} catch (Exception exc) {
			CmsShell.printException(exc);
		}
	}
/**
 * Imports a module into the cms.
 * 
 * @param moduleZip The file-name of module to import.
 */
public void getModuleInfo() {
	try {
		I_CmsRegistry reg = m_cms.getRegistry();
		java.util.Enumeration names = reg.getModuleNames();

		// print out the available modules
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			getModuleInfo(name);
		}
		System.out.println("\ngeneral stuff");
		System.out.println("\tall repositories: ");
		String[] repositories = reg.getRepositories();
		for (int i = 0; i < repositories.length; i++) {
			System.out.println("\t\t" + repositories[i]);
		}
		System.out.println("\tall views: ");
		java.util.Vector views = new java.util.Vector();
		java.util.Vector urls = new java.util.Vector();
		int max = reg.getViews(views, urls);
		for (int i = 0; i < max; i++) {
			System.out.println("\t\t" + views.elementAt(i) + " -> " + urls.elementAt(i));
		}
	} catch (Exception exc) {
		CmsShell.printException(exc);
	}
}
/**
 * Prints out informations about a module.
 * 
 * @param String module the name of the module to get infos about.
 */
public void getModuleInfo(String name) {
	try {
		I_CmsRegistry reg = m_cms.getRegistry();
		if (reg.moduleExists(name)) {
			System.out.println("\nModule: " + name + " v" + reg.getModuleVersion(name));
			System.out.println("\tNice Name: " + reg.getModuleNiceName(name));
			System.out.println("\tDescription: " + reg.getModuleDescription(name));
			System.out.println("\tAuthor: " + reg.getModuleAuthor(name));
			System.out.println("\tUploaded by: " + reg.getModuleUploadedBy(name));
			System.out.println("\tEmail: " + reg.getModuleAuthorEmail(name));
			System.out.println("\tCreationdate: " + new java.util.Date(reg.getModuleCreateDate(name)));
			System.out.println("\tUploaddate: " + new java.util.Date(reg.getModuleUploadDate(name)));
			System.out.println("\tDocumentationPath: " + reg.getModuleDocumentPath(name));
			String[] repositories = reg.getModuleRepositories(name);
			System.out.println("\trepositories: ");
			if (repositories != null) {
				for (int i = 0; i < repositories.length; i++) {
					System.out.println("\t\t" + repositories[i]);
				}
			}
			String[] parameters = reg.getModuleParameterNames(name);
			System.out.println("\tparameters: ");
			if (parameters != null) {
				for (int i = 0; i < parameters.length; i++) {
					System.out.print("\t\t" + parameters[i]);
					System.out.print(" = " + reg.getModuleParameter(name, parameters[i]));
					System.out.print(" is " + reg.getModuleParameterType(name, parameters[i]));
					System.out.println("  (" + reg.getModuleParameterDescription(name, parameters[i]) + ")");
				}
			}
			System.out.println("\tWiew name: " + reg.getModuleViewName(name));
			System.out.println("\tWiew url: " + reg.getModuleViewUrl(name));
			System.out.println("\tDependencies");
			java.util.Vector modules = new java.util.Vector();
			java.util.Vector min = new java.util.Vector();
			java.util.Vector max = new java.util.Vector();
			reg.getModuleDependencies(name, modules, min, max);
			for (int i = 0; i < modules.size(); i++) {
				System.out.println("\t\t" + modules.elementAt(i) + ": min v" + min.elementAt(i) + " max v" + max.elementAt(i));
			}
		} else {
			System.out.println("No module with name " +name);
		}
	} catch (Exception exc) {
		CmsShell.printException(exc);
	}
}
	/**
	 * Returns the parent group of a group<P/>
	 * 
	 * <B>Security:</B>
	 * All users are granted, except the anonymous user.
	 * 
	 * @param groupname The name of the group.
	 */
	public void getParent(String groupname) {
		try {
			System.out.println(m_cms.getParent(groupname));
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Returns a CmsResourceTypes.
	 * 
	 * @param resourceType the name of the resource to get.
	 */
	public void getResourceType(String resourceType) {
		try {
			System.out.println( m_cms.getResourceType(resourceType) );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}		
	}
	/**
	 * Returns a Vector with all subfolders.<BR/>
	 * 
	 * @param foldername the complete path to the folder.
	 */
	public void getSubFolders(String foldername)
		throws CmsException { 
		try {
			Vector folders = m_cms.getSubFolders(foldername);
			for( int i = 0; i < folders.size(); i++ ) {
				System.out.println( (CmsFolder)folders.elementAt(i) );
			}
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Returns a value for a system-key.
	 * E.g. <code>&lt;system&gt;&lt;mailserver&gt;mail.server.com&lt;/mailserver&gt;&lt;/system&gt;</code>
	 * can be requested via <code>getSystemValue("mailserver");</code> and returns "mail.server.com.
	 *
	 * @parameter String the key of the system-value.
	 */
	public void getSystemValue(String key) {
		try {
			I_CmsRegistry reg = m_cms.getRegistry();
			System.out.println(reg.getSystemValue(key));
		} catch (Exception exc) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Returns all users of the cms.
	 */
	public void getUsers() {
		try {
			Vector users = m_cms.getUsers();
			for( int i = 0; i < users.size(); i++ ) {
				System.out.println( (CmsUser)users.elementAt(i) );
			}
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Returns all groups of a user.
	 * 
	 * @param groupname The name of the group.
	 */
	public void getUsersOfGroup(String groupname) {
		try {
			Vector users = m_cms.getUsersOfGroup(groupname);
			for( int i = 0; i < users.size(); i++ ) {
				System.out.println( (CmsUser)users.elementAt(i) );
			}
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Imports a module into the cms.
	 * 
	 * @param moduleZip The file-name of module to import.
	 */
	public void getViews() {
		try {
			I_CmsRegistry reg = m_cms.getRegistry();
			java.util.Vector views = new java.util.Vector();
			java.util.Vector urls = new java.util.Vector();
			int max = reg.getViews(views, urls);
			for (int i = 0; i < max; i++) {
				System.out.println(views.elementAt(i) + " -> " + urls.elementAt(i));
			}
		} catch (Exception exc) {
			CmsShell.printException(exc);
		}
	}
/**
 * Prints all possible commands.
 */
public void help()
{
	Method meth[] = getClass().getMethods();
	for (int z = 0; z < meth.length; z++)
	{
		if ((meth[z].getDeclaringClass() == getClass()) && (meth[z].getModifiers() == Modifier.PUBLIC))
		{
			CmsShell.printMethod(meth[z]);
		}
	}
}
/**
 * Prints signature of all possible commands containing a certain string.<br>
 * May also be used to print signature of a specific command by giving full command name.
 *
 * @author Jan Krag
 * @param String The String to search for in the commands
 */
public void help(String searchString)
{
	if (searchString.equals("help"))
		printHelpText();
	else
	{
		Method meth[] = getClass().getMethods();
		for (int z = 0; z < meth.length; z++)
		{
			if ((meth[z].getDeclaringClass() == getClass()) && (meth[z].getModifiers() == Modifier.PUBLIC) && (meth[z].getName().toLowerCase().indexOf(searchString.toLowerCase()) > -1))
			{
				CmsShell.printMethod(meth[z]);
			}
		}
	}
}
	/**
	 * Reads a given file from the local harddisk and uploads
	 * it to the OpenCms system.
	 * Used in the OpenCms console only.
	 * 
	 * @author Alexander Lucas
	 * @param filename Local file to be uploaded.
	 * @return Byte array containing the file content.
	 * @throws CmsException
	 */
	private byte[] importFile(String filename) throws CmsException {     
		File file = null;
		long len = 0;
		FileInputStream importInput = null;
		byte[] result;        
				
		// First try to load the file
		try {
			file = new File(filename);
		} catch(Exception e) {
			file = null;
		}
		if(file == null) {
			throw new CmsException("Could not load local file " + filename, CmsException.C_NOT_FOUND); 
		} 
		
		// File was loaded successfully.
		// Now try to read the content.
		try {
			len = file.length();
			result = new byte[(int)len];
			importInput = new FileInputStream(file);
			importInput.read(result);
			importInput.close();
		} catch(Exception e) {
			throw new CmsException(e.toString() , CmsException.C_UNKNOWN_EXCEPTION); 
		}
		return result;
	}
	/**
	 * Imports a import-resource (folder or zipfile) to the cms.
	 * 
	 * @param importFile the name (absolute Path) of the import resource (zip or folder)
	 * @param importPath the name (absolute Path) of folder in which should be imported
	 */
	public void importFolder(String importFile, String importPath) {
		// import the resources
		try {
			m_cms.importFolder(importFile, importPath);
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Checks for conflicting file names for a import.
	 * 
	 * @param moduleZip The file-name of module to import.
	 */
	public void importGetConflictingFileNames(String moduleZip) {
		try {
			I_CmsRegistry reg = m_cms.getRegistry();
			Vector conflicts = reg.importGetConflictingFileNames(moduleZip);
			System.out.println("Conflicts: " + conflicts.size());
			for(int i = 0; i < conflicts.size(); i++) {
				System.out.println(conflicts.elementAt(i));
			}
		} catch( Exception exc ) { 
			CmsShell.printException(exc);
		}
	}
	/**
	 * Checks for resources that should be copied to the import-project.
	 * 
	 * @param moduleZip The file-name of module to import.
	 */
	public void importGetResourcesForProject(String moduleZip) {
		try {
			I_CmsRegistry reg = m_cms.getRegistry();
			Vector resources = reg.importGetResourcesForProject(moduleZip);
			System.out.println("Resources: " + resources.size());
			for(int i = 0; i < resources.size(); i++) {
				System.out.println(resources.elementAt(i));
			}
		} catch( Exception exc ) { 
			CmsShell.printException(exc);
		}
	}
/**
 * Imports a module (zipfile) to the cms.
 * 
 * @param importFile java.lang.String the name (complete Path) of the import module
 */
public void importModule(String importFile) {
	// import the module
	try {
		I_CmsRegistry reg = m_cms.getRegistry();
		reg.importModule(importFile, new Vector());
	} catch (Exception exc) {
		CmsShell.printException(exc);
	}
}
/**
 * Imports an import-resource (folder or zipfile) to the cms.
 * Creation date: (09.08.00 16:28:48)
 * @param importFile java.lang.String the name (absolute Path) of the import resource (zip or folder)
 */
public void importResources(String importFile) {
	// import the resources
	try {
		m_cms.importResources(importFile, C_ROOT);
	} catch (Exception exc) {
		CmsShell.printException(exc);
	}
}
/**
 * Imports an import-resource (folder or zipfile) to the cms.
 * 
 * @param importFile the name (absolute Path) of the import resource (zip or folder)
 * @param importPath the name (absolute Path) of folder in which should be imported
 */
public void importResources(String importFile, String importPath) {
	// import the resources
	try {
		m_cms.importResources(importFile, importPath);
	} catch (Exception exc) {
		CmsShell.printException(exc);
	}
}
	/**
	 * Determines, if the user is Admin.
	 */
	public void isAdmin() {
		try {
			System.out.println( m_cms.getRequestContext().isAdmin() );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Determines, if the user is Projectleader.
	 */
	public void isProjectManager() {
		try {
			System.out.println( m_cms.getRequestContext().isProjectManager() );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Locks a resource<BR/>
	 * 
	 * A user can lock a resource, so he is the only one who can write this 
	 * resource.
	 * 
	 * @param resource The complete path to the resource to lock.
	 */
	public void lockResource(String resource) {
		try {
			m_cms.lockResource(resource);
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Logs a user into the system.
	 * 
	 * @param username The name of the user to log in.
	 * @param password The password.
	 */
	public void login(String username, String password) {
		try {
			m_cms.loginUser(username, password);
			whoami();
		} catch( Exception exc ) {
			CmsShell.printException(exc);
			System.out.println("Login failed!");
		}
	}
	/**
	 * Reads a the online-project from the Cms.
	 */
	public void onlineProject() {
		try {
			System.out.println( m_cms.onlineProject().toString() );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}		
	}
/**
 * Prints help text when Shell is startet.
 * Creation date: (09/29/00)
 * @author Jan Krag
 */
public void printHelpText()
{
	System.out.println("help              Gives a list of available commands with signature");
	System.out.println("help <command>    Shows signature of command");
	System.out.println("help <substring>  Lists only those commands containing this substring");
	System.out.println("help help         Prints this text");
	System.out.println("exit or quit      Leaves the Shell");
	System.out.println("");
}
	/**
	 * Publishes a project.
	 * 
	 * @param id The id of the project to be published.
	 */
	public void publishProject(String id) {
		try {
			int projectId = Integer.parseInt(id);
			m_cms.unlockProject(projectId);
			m_cms.publishProject(projectId);
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	// All methods, that may be called by the user:
	
	/**
	 * Exits the commandline-interface
	 */
	public void quit() {
		exit();
	}
	 /**
	 * Reads all file headers of a file in the OpenCms.<BR>
	 * This method returns a vector with the histroy of all file headers, i.e. 
	 * the file headers of a file, independent of the project they were attached to.<br>
	 * 
	 * The reading excludes the filecontent.
	 * 
	 * @param filename The name of the file to be read.
	 */
	public void readAllFileHeaders(String filename) {
		try {
			Vector files = m_cms.readAllFileHeaders(filename);
			for( int i = 0; i < files.size(); i++ ) {
				System.out.println( (CmsResource)files.elementAt(i) );
			}
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Returns a list of all propertyinformations of a file or folder.
	 * 
	 * @param resource The name of the resource of which the propertyinformation has to be 
	 * read.
	 */
	public void readAllProperties(String resource) {
		try {
			Hashtable propertyinfos = m_cms.readAllProperties(resource);
			Enumeration keys = propertyinfos.keys();
			Object key;
			
			while(keys.hasMoreElements()) {
				key = keys.nextElement();
				System.out.print(key + "=");
				System.out.println(propertyinfos.get(key));
			}
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}		
	}
	/**
	 * Reads all propertydefinitions for the given resource type.
	 * 
	 * @param resourcetype The name of the resource type to read the 
	 * propertydefinitions for.
	 */	
	public void readAllPropertydefinitions(String resourcetype) {
		try {
			Vector propertydefs = m_cms.readAllPropertydefinitions(resourcetype);
			for( int i = 0; i < propertydefs.size(); i++ ) {
				System.out.println( (CmsPropertydefinition)propertydefs.elementAt(i) );
			}
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}		
	}
	/**
	 * Reads all propertydefinitions for the given resource type.
	 * 
	 * @param resourcetype The name of the resource type to read the 
	 * propertydefinitions for.
	 * @param type The type of the propertydefinition (normal|mandatory|optional).
	 */	
	public void readAllPropertydefinitions(String resourcetype, String type) {
		try {
			Vector propertydefs = m_cms.readAllPropertydefinitions(resourcetype, 
														   Integer.parseInt(type));
			for( int i = 0; i < propertydefs.size(); i++ ) {
				System.out.println( (CmsPropertydefinition)propertydefs.elementAt(i) );
			}
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}		
	}
	/**
	 * Reads the export-path for the system.
	 * This path is used for db-export and db-import.
	 * 
	 * @return the exportpath.
	 */
	public void readExportPath()
		throws CmsException {
		try {
			System.out.println( m_cms.readExportPath() );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Reads a file from the Cms.<BR/>
	 * 
	 * @param filename The complete path to the file
	 */
	public void readFile(String filename) {
		try {
			System.out.println(m_cms.readFile(filename));
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Reads a file from the Cms.<BR/>
	 * 
	 * @param filename The complete path to the file
	 */
	public void readFileContent(String filename) {
		try {
			System.out.println(m_cms.readFile(filename));
			System.out.println(new String(m_cms.readFile(filename).getContents()));
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Reads a file header from the Cms.<BR/>
	 * The reading excludes the filecontent.
	 * 
	 * @param filename The complete path of the file to be read.
	 */
	public void readFileHeader(String filename) {
		try {
			System.out.println( m_cms.readFileHeader(filename) );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Reads a folder from the Cms.<BR/>
	 * 
	 * @param folder The complete path to the folder that will be read.
	 */
	public void readFolder(String folder) {
		try {
			System.out.println( m_cms.readFolder(folder, "") );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}		
	}
	/**
	 * Returns a group in the Cms.
	 * 
	 * @param groupname The name of the group to be returned.
	 * 
	 * @exception CmsException Throws CmsException if operation was not succesful
	 */
	public void readGroup(String groupname) { 
		try {
			System.out.println( m_cms.readGroup( groupname ) );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Gets all CmsMountPoints. 
	 * All mountpoints will be returned.
	 * 
	 * @return the mountpoints - or null if they doesen't exists.
	 */
	public void readMimeTypes() {
		try {
			Hashtable mimeTypes = m_cms.readMimeTypes();
			Enumeration keys = mimeTypes.keys();
			String key;
			
			while(keys.hasMoreElements()) {
				key = (String) keys.nextElement();
				System.out.println(key + " : " + mimeTypes.get(key));
			}
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}		
	}
	/**
	 * Reads a project from the Cms.
	 * 
	 * @param name The id of the project to read.
	 */
	public void readProject(String id) {
		try {
			int projectId = Integer.parseInt(id);
			System.out.println( m_cms.readProject(projectId).toString() );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}		
	}
	/**
	 * Returns a propertyinformation of a file or folder.
	 * 
	 * @param name The resource-name of which the propertyinformation has to be read.
	 * @param property The propertydefinition-name of which the propertyinformation has to be read.
	 */
	public void readProperty(String name, String property) {
		try {
			System.out.println( m_cms.readProperty(name, property) );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Reads the propertydefinition for the resource type.<BR/>
	 * 
	 * @param name The name of the propertydefinition to read.
	 * @param resourcetype The name of the resource type for the propertydefinition.
	 */
	public void readPropertydefinition(String name, String resourcetype) {
		try {
			System.out.println( m_cms.readPropertydefinition(name, resourcetype) );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Returns a user object.<P/>
	 * 
	 * @param username The name of the user that is to be read.
	 */
	public void readUser(String username) {
		try {
			System.out.println( m_cms.readUser(username) );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}		
	}
	/**
	 * Returns a user object.<P/>
	 * 
	 * @param username The name of the user that is to be read.
	 */
	public void readUser(String username, String password) {
		try {
			System.out.println( m_cms.readUser(username, password) );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}		
	}
	/** 
	 * Recovers the password for a user.
	 * 
	 * @param username The name of the user.
	 * @param recoverPassword The recover password to check the access.
	 * @param newPassword The new password.
	 */
	public void recoverPassword(String username, String recPassword, String newPassword) {
		try {
			m_cms.recoverPassword( username, recPassword, newPassword );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Removes a user from a group.
	 * 
	 * @param username The name of the user that is to be removed from the group.
	 * @param groupname The name of the group.
	 */	
	public void removeUserFromGroup(String username, String groupname) {
		try {
			m_cms.removeUserFromGroup( username, groupname );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Renames the file to the new name.
	 * 
	 * @param oldname The complete path to the resource which will be renamed.
	 * @param newname The new name of the resource (No path information allowed).
	 */		
	public void renameFile(String oldname, String newname) {
		try {
			m_cms.renameFile(oldname, newname);
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Sets the current project for the user.
	 * 
	 * @param id The id of the project to be set as current.
	 */
	public void setCurrentProject(String id) {
		try {
			int projectId = Integer.parseInt(id);
			System.out.println( m_cms.getRequestContext().setCurrentProject(projectId).toString() );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
/**
 * This method sets the author of the module.
 *
 * @param String the name of the module.
 * @param String the name of the author.
 */
public void setModuleAuthor(String modulename, String author) {
	try {
		I_CmsRegistry reg = m_cms.getRegistry();
		reg.setModuleAuthor(modulename, author);
	} catch (Exception exc) {
		CmsShell.printException(exc);
	}
}
/**
 * This method sets the email of author of the module.
 *
 * @param String the name of the module.
 * @param String the email of author of the module.
 */
public void setModuleAuthorEmail(String modulename, String email) {
	try {
		I_CmsRegistry reg = m_cms.getRegistry();
		reg.setModuleAuthorEmail(modulename, email);
	} catch (Exception exc) {
		CmsShell.printException(exc);
	}
}
/**
 * Sets the create date of the module.
 *
 * @param String the name of the module.
 * @param long the create date of the module.
 */
public void setModuleCreateDate(String modulname, String createdate) {
	try {
		I_CmsRegistry reg = m_cms.getRegistry();
		long date = Long.parseLong(createdate);
		reg.setModuleCreateDate(modulname, date);
	} catch (Exception exc) {
		CmsShell.printException(exc);
	}
}
/**
 * Sets the description of the module.
 *
 * @param String the name of the module.
 * @param String the description of the module.
 */
public void setModuleDescription(String module, String description) {
	try {
		I_CmsRegistry reg = m_cms.getRegistry();
		reg.setModuleDescription(module, description);
	} catch (Exception exc) {
		CmsShell.printException(exc);
	}
}
/**
 * Sets the url to the documentation of the module.
 * 
 * @param String the name of the module.
 * @param java.lang.String the url to the documentation of the module.
 */
public void setModuleDocumentPath(String modulename, String url) {
	try {
		I_CmsRegistry reg = m_cms.getRegistry();
		reg.setModuleDocumentPath(modulename, url);
	} catch (Exception exc) {
		CmsShell.printException(exc);
	}
}
/**
 * Sets the classname, that receives all maintenance-events for the module.
 * 
 * @param String the name of the module.
 * @param java.lang.Class that receives all maintenance-events for the module.
 */
public void setModuleMaintenanceEventClass(String modulname, String classname) {
	try {
		I_CmsRegistry reg = m_cms.getRegistry();
		reg.setModuleMaintenanceEventClass(modulname, classname);
	} catch (Exception exc) {
		CmsShell.printException(exc);
	}
}
/**
 * Sets the description of the module.
 *
 * @param String the name of the module.
 * @param String the nice name of the module.
 */
public void setModuleNiceName(String module, String nicename) {
	try {
		I_CmsRegistry reg = m_cms.getRegistry();
		reg.setModuleNiceName(module, nicename);
	} catch (Exception exc) {
		CmsShell.printException(exc);
	}
}
/**
 * This method sets the version of the module.
 *
 * @param String the name of the module.
 * @param the version of the module.
 */
public void setModuleVersion(String modulename, String version) {
	try {
		I_CmsRegistry reg = m_cms.getRegistry();
		int ver = Integer.parseInt(version);
		reg.setModuleVersion(modulename, ver);
	} catch (Exception exc) {
		CmsShell.printException(exc);
	}
}
/**
 * Sets a view for a module
 * 
 * @param String the name of the module.
 * @param String the name of the view, that is implemented by the module.
 * @param String the url of the view, that is implemented by the module.
 */
public void setModuleView(String modulename, String viewname, String viewurl) {
	try {
		I_CmsRegistry reg = m_cms.getRegistry();
		reg.setModuleView(modulename, viewname, viewurl);
	} catch (Exception exc) {
		CmsShell.printException(exc);
	}
}
	/** 
	 * Sets the password for a user.
	 * 
	 * @param username The name of the user.
	 * @param newPassword The new password.
	 */
	public void setPassword(String username, String newPassword) {
		try {
			m_cms.setPassword( username, newPassword );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/** 
	 * Sets the password for a user.
	 * 
	 * @param username The name of the user.
	 * @param oldPassword The old password.
	 * @param newPassword The new password.
	 */
	public void setPassword(String username, String oldPassword, String newPassword) {
		try {
			m_cms.setPassword( username, oldPassword, newPassword );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/** 
	 * Sets the recovery password for a user.
	 * 
	 * @param username The name of the user.
	 * @param password The password.
	 * @param newPassword The new recovery password.
	 */
	public void setRecoveryPassword(String username, String oldPassword, String newPassword) {
		try {
			m_cms.setRecoveryPassword( username, oldPassword, newPassword );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Sets the current group of the current user.
	 */
	public void setUserCurrentGroup(String groupname) {
		try {
			m_cms.getRequestContext().setCurrentGroup(groupname);
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Unlocks a project.
	 * 
	 * @param id The id of the project to be unlocked.
	 */
	public void unlockProject(String id) {
		try {
			int projectId = Integer.parseInt(id);
			m_cms.unlockProject(projectId);
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Unlocks a resource<BR/>
	 * 
	 * A user can unlock a resource, so other users may lock this file.
	 * 
	 * @param resource The complete path to the resource to lock.
	 */
	public void unlockResource(String resource) {
		try {
			m_cms.unlockResource(resource);
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Loads a File up to the cms from the lokal disc.
	 * 
	 * @param lokalfile The lokal file to load up.
	 * @param folder The folder in the cms to put the new file
	 * @param filename The name of the new file.
	 * @param type the filetype of the new file in the cms.
	 */
	public void uploadFile(String lokalfile, String folder, String filename, String type) {
		try {
			System.out.println(m_cms.createFile(folder, filename, 
												importFile(lokalfile), type));
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Returns the current group of the current user.
	 */
	public void userCurrentGroup() {
		System.out.println(m_cms.getRequestContext().currentGroup());
	}
	/**
	 * Returns the default group of the current user.
	 */
	public void userDefaultGroup() {
		System.out.println(m_cms.getRequestContext().currentUser().getDefaultGroup());
	}
	/**
	 * Checks if a user is member of a group.<P/>
	 *  
	 * @param nameuser The name of the user to check.
	 * @param groupname The name of the group to check.
	 */
	public void userInGroup(String username, String groupname)
	{
		try {
			System.out.println( m_cms.userInGroup( username, groupname ) );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Returns a version-string for this OpenCms.
	 */
	 public void version() {
		 System.out.println(m_cms.version());
	 }
	/**
	 * Returns the current user.
	 */
	public void whoami() {
		System.out.println(m_cms.getRequestContext().currentUser());
	}
	/**
	 * Writes the export-path for the system.
	 * This path is used for db-export and db-import.
	 * 
	 * @param mountpoint The mount point in the Cms filesystem.
	 */
	public void writeExportPath(String path)
		throws CmsException {
		try {
			m_cms.writeExportPath(path);
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/** 
	 * Writes a group to the Cms.
	 * 
	 * @param name The name of the group to be written.
	 * @param flags The flags of the user to be written.
	 */
	public void writeGroup( String name, String flags ) {
		try {
			// get the group, which has to be written
			CmsGroup group = m_cms.readGroup(name);
			
			if(Integer.parseInt(flags) == C_FLAG_DISABLED) {
				group.setDisabled();
			} else {
				group.setEnabled();
			}
			
			// write it back
			m_cms.writeGroup(group);		

		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Writes a propertyinformation for a file or folder.
	 * 
	 * @param name The resource-name of which the propertyinformation has to be set.
	 * @param property The propertydefinition-name of which the propertyinformation has to be set.
	 * @param value The value for the propertyinfo to be set.
	 */
	public void writeProperty(String name, String property, String value) {
		try {
			m_cms.writeProperty(name, property, value);
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/**
	 * Writes the propertydefinition for the resource type.<BR/>
	 * 
	 * @param name The name of the propertydefinition to overwrite.
	 * @param resourcetype The name of the resource type to read the 
	 * propertydefinitions for.
	 * @param type The new type of the propertydefinition (normal|mandatory|optional).
	 * 
	 * @exception CmsException Throws CmsException if something goes wrong.
	 */
	public void writePropertydefinition(String name, 
									String resourcetype, 
									String type) {
		try {
			CmsPropertydefinition propertydef = m_cms.readPropertydefinition(name, resourcetype);
			propertydef.setPropertydefType(Integer.parseInt(type));			
			System.out.println( m_cms.writePropertydefinition(propertydef) );
		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
	/** 
	 * Writes a user to the Cms.
	 * 
	 * @param name The name of the user to be written.
	 * @param flags The flags of the user to be written.
	 */
	public void writeUser( String name, String flags ) {
		try {
			// get the user, which has to be written
			CmsUser user = m_cms.readUser(name);
			
			if(Integer.parseInt(flags) == C_FLAG_DISABLED) {
				user.setDisabled();
			} else {
				user.setEnabled();
			}
			
			// write it back
			m_cms.writeUser(user);		

		} catch( Exception exc ) {
			CmsShell.printException(exc);
		}
	}
}
