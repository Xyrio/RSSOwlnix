/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2009 RSSOwl Development Team                                  **
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

package org.rssowl.ui.internal.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.OwlUI;

/**
 * @author bpasero
 */
public class NewTypeDropdownAction extends AbstractSelectionAwareBookMarkAction<NewTypeDropdownAction> implements IWorkbenchWindowPulldownDelegate, IMenuCreator {
  private Menu fMenu;
  private LocalResourceManager fResources = new LocalResourceManager(JFaceResources.getResources());
  private IBindingService fBindingService = PlatformUI.getWorkbench().getService(IBindingService.class);

  /*
   * @see org.eclipse.ui.IWorkbenchWindowPulldownDelegate#getMenu(org.eclipse.swt.widgets.Control)
   */
  @Override
  public Menu getMenu(Control parent) {
    if (fMenu != null)
      OwlUI.safeDispose(fMenu);

    fMenu = new Menu(parent);
    newMenuItem("org.rssowl.ui.actions.NewBookMark", Messages.NewTypeDropdownAction_BOOKMARK, OwlUI.BOOKMARK, new NewBookMarkAction()); //$NON-NLS-1$
    newMenuItem("org.rssowl.ui.actions.NewNewsBin", Messages.NewTypeDropdownAction_NEWSBIN, OwlUI.NEWSBIN, new NewNewsBinAction()); //$NON-NLS-1$
    newMenuItem("org.rssowl.ui.actions.NewSearchMark", Messages.NewTypeDropdownAction_SAVED_SEARCH, OwlUI.SEARCHMARK, new NewSearchMarkAction()); //$NON-NLS-1$
    newMenuItem("org.rssowl.ui.actions.NewFolder", Messages.NewTypeDropdownAction_FOLDER, OwlUI.FOLDER, new NewFolderAction()); //$NON-NLS-1$
    return fMenu;
  }

  private void newMenuItem(String id, String text, ImageDescriptor imageDescriptor, AbstractSelectionAwareBookMarkAction<? extends IActionDelegate> newAction) {

    MenuItem menuItem = new MenuItem(fMenu, SWT.PUSH);
    menuItem.setText(getLabelWithBinding(id, text));
    menuItem.setImage(OwlUI.getImage(fResources, imageDescriptor));
    menuItem.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        try {
          newAction.init(fShell, fParent, fPosition).run(null);
        } catch (Exception e1) {
          Activator.getDefault().logError(e1.getMessage(), e1);
        }
      }
    });
  }

  /*
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
   */
  @Override
  public void dispose() {
    fResources.dispose();
    if (fMenu != null)
      OwlUI.safeDispose(fMenu);
  }

  /*
   * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
   */
  @Override
  public void run(IAction action) {
    try {
      new NewBookMarkAction().init(fShell, fParent, fPosition).run(null);
    } catch (Exception e) {
      Activator.getDefault().logError(e.getMessage(), e);
    }
  }

  /*
   * @see org.eclipse.jface.action.IMenuCreator#getMenu(org.eclipse.swt.widgets.Menu)
   */
  @Override
  public Menu getMenu(Menu parent) {
    return null;
  }

  private String getLabelWithBinding(String id, String label) {
    TriggerSequence binding = fBindingService.getBestActiveBindingFor(id);
    if (binding != null)
      return NLS.bind(Messages.NewTypeDropdownAction_LABEL_BINDING, label, binding.format());

    return label;
  }
}