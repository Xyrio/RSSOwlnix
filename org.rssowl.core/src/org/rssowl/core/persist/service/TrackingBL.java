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

package org.rssowl.core.persist.service;

import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.util.DateUtils;

import java.util.Date;

public class TrackingBL {

  public static void onVisited(IBookMark mark) {
    mark.setPopularity(mark.getPopularity() + 1);
    mark.setLastVisitDate(new Date());
  }

  public static void onVisited(INewsBin mark) {
    mark.setPopularity(mark.getPopularity() + 1);
    mark.setLastVisitDate(new Date());
  }

  public static void onVisited(ISearchMark mark) {
    mark.setPopularity(mark.getPopularity() + 1);
    mark.setLastVisitDate(new Date());
  }

  public static void onChanged(IBookMark mark) {
    mark.setLastUpdateDate(new Date());
    mark.setLastRecentDate(mark.getLastRecentNewsDate());
  }

  public static void onChanged(INewsBin mark) {
    mark.setLastUpdateDate(new Date());
    mark.setLastRecentDate(DateUtils.getRecentDate(mark.getNews()));
  }

  public static void onChanged(ISearchMark mark) {
    mark.setLastUpdateDate(new Date());
    mark.setLastRecentDate(DateUtils.getRecentDate(mark.getNews()));
  }
}
