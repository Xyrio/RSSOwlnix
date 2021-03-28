/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2011 RSSOwl Development Team                                  **
 **   http://www.rssowl.org/                                                 **
 **                                                                          **
 **   All rights reserved                                                    **
 **                                                                          **
 **   This program and the accompanying materials are made available under   **
 **   the terms of the Eclipse Public License v1.0 which accompanies this    **
 **   distribution, and is available at:                                     **
 **   http://www.rssowl.org/legal/epl-v10.html                               **
 **                                                                          **
 **   A copy is found in the file epl-v10.html and important notices to the  **
 **   license from the team is found in the textfile LICENSE.txt distributed **
 **   in this package.                                                       **
 **                                                                          **
 **   This copyright notice MUST APPEAR in all copies of the file!           **
 **                                                                          **
 **   Contributors:                                                          **
 **     RSSOwl Development Team - initial API and implementation             **
 **                                                                          **
 **  **********************************************************************  */
package org.rssowl.core.persist;

import java.util.Date;

public interface ITracking {

  /**
   * @return How often this Feed has been visited by the User.
   */
  int getPopularity();

  /**
   * @param popularity How often this Feed has been visited by the User.
   */
  void setPopularity(int popularity);

  /**
   * Get the Date this Mark was created.
   *
   * @return the creation date of this mark.
   */
  Date getCreationDate();

  /**
   * Set the Date this Mark was created.
   *
   * @param creationDate The creation date of this mark.
   */
  void setCreationDate(Date creationDate);

  /**
   * @return date last displayed to the user
   */
  Date getLastVisitDate();

  /**
   * @return date last recent date of news
   */
  Date getLastRecentDate();

  /**
   * @return date last updated the news
   */
  Date getLastUpdateDate();

  /**
   * @param lastVisitDate date last displayed to the user
   */
  void setLastVisitDate(Date lastVisitDate);

  /**
   * @param lastRecentDate date last recent date of news
   */
  void setLastRecentDate(Date lastRecentDate);

  /**
   * @param lastUpdateDate date last updated the news
   */
  void setLastUpdateDate(Date lastUpdateDate);

}

