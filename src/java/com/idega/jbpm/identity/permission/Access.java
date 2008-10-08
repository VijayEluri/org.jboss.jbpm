package com.idega.jbpm.identity.permission;

/**
 * 
 * <p>
 * <b>
 * about contactsCanBeSeen, seeContacts accesses </b> (those two are different):
 * <p>
 * contactsCanBeSeen means, that the role's contacts for which this access is specified, can be seen by the relevant subject (e.g. current user)
 * </p>
 * <p>
 * seeContacts - the current role can see contacts (if has this access) of the roles specified. otherwise (if the seeContacts access is not specified),
 * role is considered to be _not_ able to see those specified contacts
 * </p>
 * </p>
 * 
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.5 $
 *
 * Last modified: $Date: 2008/10/08 18:50:45 $ by $Author: civilis $
 */
public enum Access {

	read, write, modifyPermissions, processHandler, contactsCanBeSeen, seeContacts
}