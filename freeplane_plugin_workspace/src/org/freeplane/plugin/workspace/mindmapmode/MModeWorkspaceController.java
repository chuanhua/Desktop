package org.freeplane.plugin.workspace.mindmapmode;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.IMenuContributor;
import org.freeplane.core.ui.MenuBuilder;
import org.freeplane.core.ui.components.JResizer.Direction;
import org.freeplane.core.ui.components.OneTouchCollapseResizer;
import org.freeplane.core.ui.components.OneTouchCollapseResizer.CollapseDirection;
import org.freeplane.core.ui.components.OneTouchCollapseResizer.ComponentCollapseListener;
import org.freeplane.core.ui.components.ResizeEvent;
import org.freeplane.core.ui.components.ResizerListener;
import org.freeplane.core.util.Compat;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.ui.INodeViewLifeCycleListener;
import org.freeplane.features.ui.ViewController;
import org.freeplane.features.url.UrlManager;
import org.freeplane.plugin.workspace.URIUtils;
import org.freeplane.plugin.workspace.WorkspaceController;
import org.freeplane.plugin.workspace.actions.FileNodeDeleteAction;
import org.freeplane.plugin.workspace.actions.FileNodeNewFileAction;
import org.freeplane.plugin.workspace.actions.FileNodeNewMindmapAction;
import org.freeplane.plugin.workspace.actions.NodeCopyAction;
import org.freeplane.plugin.workspace.actions.NodeCutAction;
import org.freeplane.plugin.workspace.actions.NodeNewFolderAction;
import org.freeplane.plugin.workspace.actions.NodeNewLinkAction;
import org.freeplane.plugin.workspace.actions.NodeOpenLocationAction;
import org.freeplane.plugin.workspace.actions.NodePasteAction;
import org.freeplane.plugin.workspace.actions.NodeRefreshAction;
import org.freeplane.plugin.workspace.actions.NodeRemoveAction;
import org.freeplane.plugin.workspace.actions.NodeRenameAction;
import org.freeplane.plugin.workspace.actions.PhysicalFolderSortOrderAction;
import org.freeplane.plugin.workspace.actions.WorkspaceCollapseAction;
import org.freeplane.plugin.workspace.actions.WorkspaceExpandAction;
import org.freeplane.plugin.workspace.actions.WorkspaceImportProjectAction;
import org.freeplane.plugin.workspace.actions.WorkspaceNewMapAction;
import org.freeplane.plugin.workspace.actions.WorkspaceNewProjectAction;
import org.freeplane.plugin.workspace.actions.WorkspaceProjectOpenLocationAction;
import org.freeplane.plugin.workspace.actions.WorkspaceRemoveProjectAction;
import org.freeplane.plugin.workspace.components.IWorkspaceView;
import org.freeplane.plugin.workspace.components.TreeView;
import org.freeplane.plugin.workspace.creator.DefaultFileNodeCreator;
import org.freeplane.plugin.workspace.dnd.WorkspaceTransferable;
import org.freeplane.plugin.workspace.features.AWorkspaceModeExtension;
import org.freeplane.plugin.workspace.features.IWorkspaceSettingsHandler;
import org.freeplane.plugin.workspace.handler.DefaultFileNodeIconHandler;
import org.freeplane.plugin.workspace.handler.DirectoryMergeConflictDialog;
import org.freeplane.plugin.workspace.handler.FileExistsConflictDialog;
import org.freeplane.plugin.workspace.handler.LinkTypeFileIconHandler;
import org.freeplane.plugin.workspace.io.AFileNodeCreator;
import org.freeplane.plugin.workspace.io.FileReadManager;
import org.freeplane.plugin.workspace.io.FileSystemManager;
import org.freeplane.plugin.workspace.model.WorkspaceModel;
import org.freeplane.plugin.workspace.model.project.AWorkspaceProject;
import org.freeplane.plugin.workspace.model.project.IProjectSelectionListener;
import org.freeplane.plugin.workspace.model.project.ProjectSelectionEvent;
import org.freeplane.plugin.workspace.nodes.DefaultFileNode;
import org.freeplane.plugin.workspace.nodes.LinkTypeFileNode;
import org.freeplane.view.swing.map.NodeView;
import org.freeplane.view.swing.ui.mindmapmode.MNodeDropListener;

public class MModeWorkspaceController extends AWorkspaceModeExtension {

	abstract class ResizerEventAdapter implements ResizerListener, ComponentCollapseListener {
	}

	private FileReadManager fileTypeManager;
	private TreeView view;
	private IWorkspaceSettingsHandler settings;
	private WorkspaceModel wsModel;
	private AWorkspaceProject currentSelectedProject = null;
	private IProjectSelectionListener projectSelectionListener;
	private Runnable viewUpdater;

	public MModeWorkspaceController(ModeController modeController) {
		super(modeController);
		setupController(modeController);
	}
	
	public void start(ModeController modeController) {
		setupActions(modeController);
		setupModel(modeController);
		setupView(modeController);
	}
	
	private void setupController(ModeController modeController) {
		modeController.removeExtension(UrlManager.class);
		UrlManager.install(new MModeWorkspaceUrlManager());
		
		modeController.removeExtension(LinkController.class);
		LinkController.install(MModeWorkspaceLinkController.getController());
		
		//add link type entry to the chooser
		MModeWorkspaceLinkController.getController().prepareOptionPanelBuilder(((MModeController)modeController).getOptionPanelBuilder());
		
		modeController.addINodeViewLifeCycleListener(new INodeViewLifeCycleListener() {

			public void onViewCreated(Container nodeView) {
				NodeView node = (NodeView) nodeView;
				final DropTarget dropTarget = new DropTarget(node.getMainView(), new MNodeDropListener() {
					public void drop(final DropTargetDropEvent dtde) {
						DropTargetDropEvent evt = dtde;
						if(dtde.getTransferable().isDataFlavorSupported(WorkspaceTransferable.WORKSPACE_NODE_FLAVOR)) {
							evt = new DropTargetDropEvent(dtde.getDropTargetContext(), dtde.getLocation(), dtde.getDropAction(), dtde.getSourceActions(), false);
						}
						super.drop(evt);
					}
				});
				dropTarget.setActive(true);
			}

			public void onViewRemoved(Container nodeView) {
			}

		});
		
		modeController.addMenuContributor(new IMenuContributor() {
			public void updateMenus(ModeController modeController, MenuBuilder builder) {
				final String MENU_PROJECT_KEY = "/menu_bar/project";
				//insert project menu into main menu
				JMenu projectMenu = new JMenu(TextUtils.getText("menu.project.entry.label"));
				projectMenu.setMnemonic('o');				
				builder.addMenuItem("/menu_bar/format", projectMenu, MENU_PROJECT_KEY, MenuBuilder.AFTER);
				
				builder.addAction(MENU_PROJECT_KEY, WorkspaceController.getAction(WorkspaceNewProjectAction.KEY), MenuBuilder.AS_CHILD);
				builder.addAction(MENU_PROJECT_KEY, WorkspaceController.getAction(WorkspaceImportProjectAction.KEY), MenuBuilder.AS_CHILD);
				
				builder.addSeparator(MENU_PROJECT_KEY, MenuBuilder.AS_CHILD);
				final String MENU_PROJECT_ADD_KEY = builder.getMenuKey(MENU_PROJECT_KEY, "new");				
				final JMenu addMenu = new JMenu(TextUtils.getText("workspace.action.new.label"));
				builder.addMenuItem(MENU_PROJECT_KEY, addMenu, MENU_PROJECT_ADD_KEY, MenuBuilder.AS_CHILD);
				builder.addAction(MENU_PROJECT_ADD_KEY, new NodeNewFolderAction(), MenuBuilder.AS_CHILD);
				builder.addAction(MENU_PROJECT_ADD_KEY, new NodeNewLinkAction(), MenuBuilder.AS_CHILD);
				final WorkspaceRemoveProjectAction rmProjectAction = new WorkspaceRemoveProjectAction();
				builder.addAction(MENU_PROJECT_KEY, rmProjectAction, MenuBuilder.AS_CHILD);
				
				builder.addSeparator(MENU_PROJECT_KEY, MenuBuilder.AS_CHILD);
				setDefaultAccelerator(builder.getShortcutKey(builder.getMenuKey(MENU_PROJECT_KEY,WorkspaceProjectOpenLocationAction.KEY)), "control alt L");
				final WorkspaceProjectOpenLocationAction openLocAction = new WorkspaceProjectOpenLocationAction();
				builder.addAction(MENU_PROJECT_KEY, openLocAction, MenuBuilder.AS_CHILD);
				
				projectMenu.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
					public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
						rmProjectAction.setEnabled();
						openLocAction.setEnabled();
						if(WorkspaceController.getCurrentProject() == null) {
							addMenu.setEnabled(false);
						}
						else {
							addMenu.setEnabled(true);
						}						
					}
					
					public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
					
					public void popupMenuCanceled(PopupMenuEvent e) {}
				});
			}
			
			private void setDefaultAccelerator(final String shortcutKey, String accelerator) {
				if (accelerator != null) {				
					if (null == ResourceController.getResourceController().getProperty(shortcutKey, null)) {
						if (Compat.isMacOsX()) {
							accelerator = accelerator.replaceFirst("CONTROL", "META").replaceFirst("control", "meta");
						}
						
						ResourceController.getResourceController().setDefaultProperty(shortcutKey, accelerator);
					}
				}
			}
		});
	}

//	private void setupSettings(ModeController modeController) {
//		loadSettings(getSettingsPath());
//	}
	
	private void setupModel(ModeController modeController) {
		load();
	}

	private void setupView(ModeController modeController) {
		FileSystemManager.setDirectoryConflictHandler(new DirectoryMergeConflictDialog());
		FileSystemManager.setFileConflictHandler(new FileExistsConflictDialog());
		
		
		final OneTouchCollapseResizer otcr = new OneTouchCollapseResizer(Direction.LEFT, CollapseDirection.COLLAPSE_LEFT);
		otcr.addCollapseListener(getWorkspaceView());
		ResizerEventAdapter adapter = new ResizerEventAdapter() {
			
			public void componentResized(ResizeEvent event) {
				if(event.getSource().equals(getView())) {
					getWorkspaceSettings().setProperty(WorkspaceSettings.WORKSPACE_VIEW_WIDTH, String.valueOf(((JComponent) event.getSource()).getPreferredSize().width));
				}
			}

			public void componentCollapsed(ResizeEvent event) {
				if(event.getSource().equals(getView())) {
					getWorkspaceSettings().setProperty(WorkspaceSettings.WORKSPACE_VIEW_COLLAPSED, "true");
				}
			}

			public void componentExpanded(ResizeEvent event) {
				if(event.getSource().equals(getView())) {
					getWorkspaceSettings().setProperty(WorkspaceSettings.WORKSPACE_VIEW_COLLAPSED, "false");
				}
			}			
		};
		
		otcr.addResizerListener(adapter);
		otcr.addCollapseListener(adapter);
		
		Box resizableTools = Box.createHorizontalBox();
		resizableTools.add(getWorkspaceView());
		this.viewUpdater = new Runnable() {
			public void run() {
				boolean expanded = true;
				try {
					expanded = !Boolean.parseBoolean(getWorkspaceSettings().getProperty(WorkspaceSettings.WORKSPACE_VIEW_COLLAPSED, "false"));
				}
				catch (Exception e) {
					// ignore -> default is true
				}
				otcr.setExpanded(expanded);
				try {
					int width = Integer.parseInt(getWorkspaceSettings().getProperty(WorkspaceSettings.WORKSPACE_VIEW_WIDTH, "250"));
					getWorkspaceView().setPreferredSize(new Dimension(width, 40));
				}
				catch (Exception e) {
					// blindly accept
				}
			}
		};
		this.viewUpdater.run();
		resizableTools.add(otcr);
		
		modeController.getUserInputListenerFactory().addToolBar("workspace", ViewController.LEFT, resizableTools);
		getWorkspaceView().setModel(getModel());
		getView().expandPath(getModel().getRoot().getTreePath());
		for(AWorkspaceProject project : getModel().getProjects()) {
			getView().expandPath(project.getModel().getRoot().getTreePath());
		}
		
		getView().getNodeTypeIconManager().addNodeTypeIconHandler(LinkTypeFileNode.class, new LinkTypeFileIconHandler());
		getView().getNodeTypeIconManager().addNodeTypeIconHandler(DefaultFileNode.class, new DefaultFileNodeIconHandler());
		getView().refreshView();
	}
		
	private void setupActions(ModeController modeController) {
		WorkspaceController.addAction(new WorkspaceExpandAction());
		WorkspaceController.addAction(new WorkspaceCollapseAction());
		WorkspaceController.addAction(new WorkspaceNewProjectAction());
		WorkspaceController.addAction(new WorkspaceImportProjectAction());
		WorkspaceController.addAction(new NodeNewFolderAction());
		WorkspaceController.addAction(new NodeNewLinkAction());
		WorkspaceController.addAction(new NodeOpenLocationAction());
		
		//WORKSPACE - fixed: #332
		WorkspaceController.addAction(new NodeCutAction());
		WorkspaceController.addAction(new NodeCopyAction());
		WorkspaceController.addAction(new NodePasteAction());
		WorkspaceController.addAction(new NodeRenameAction());
		WorkspaceController.addAction(new NodeRemoveAction());
		WorkspaceController.addAction(new NodeRefreshAction());
		WorkspaceController.addAction(new WorkspaceRemoveProjectAction());
		
		WorkspaceController.replaceAction(new WorkspaceNewMapAction());
		WorkspaceController.addAction(new FileNodeNewMindmapAction());
		WorkspaceController.addAction(new FileNodeNewFileAction());
		WorkspaceController.addAction(new FileNodeDeleteAction());
		
		WorkspaceController.addAction(new PhysicalFolderSortOrderAction());
	}

	private void saveSettings() {
		
		// clear old settings
		String[] projectsIds = getWorkspaceSettings().getProperty(WorkspaceSettings.WORKSPACE_MODEL_PROJECTS, "").split(WorkspaceSettings.WORKSPACE_MODEL_PROJECTS_SEPARATOR);
		for (String projectID : projectsIds) {
			getWorkspaceSettings().removeProperty(projectID);
		}
		// build new project stack
		List<String> projectIDs = new ArrayList<String>();
		synchronized (getModel().getProjects()) {
			for(AWorkspaceProject project : getModel().getProjects()) {
				saveProject(project);
				if(projectIDs.contains(project.getProjectID())) {
					continue;
				}
				projectIDs.add(project.getProjectID());
				getWorkspaceSettings().setProperty(project.getProjectID(), project.getProjectHome().toString());
			}
		}
		StringBuilder sb = new StringBuilder();
		for (String prjId : projectIDs) {
			if(sb.length()>0) {
				sb.append(WorkspaceSettings.WORKSPACE_MODEL_PROJECTS_SEPARATOR);
			}
			sb.append(prjId);
		}
		getWorkspaceSettings().setProperty(WorkspaceSettings.WORKSPACE_MODEL_PROJECTS, sb.toString());
		try {
			getWorkspaceSettings().store();
		}
		catch (final Exception ex) {
			LogUtils.severe("could not store workspace settings.", ex);
		}
	}
	
	private void saveProject(AWorkspaceProject project) {
		try {
			getProjectLoader().storeProject(project);
		} catch (IOException e) {
			LogUtils.severe(e);
		}
		
	}

	private TreeView getWorkspaceView() {
		if (this.view == null) {
			this.view = new TreeView();
			this.view.setMinimumSize(new Dimension(100, 40));
			this.view.setPreferredSize(new Dimension(150, 40));
			this.view.addProjectSelectionListener(getProjectSelectionListener());
			getModel();
		}
		return this.view;
	}
	
	public IWorkspaceSettingsHandler getWorkspaceSettings() {
		if(settings == null) {
			settings = new WorkspaceSettings();
			try {
				settings.load();
			} catch (IOException e) {
				LogUtils.info("Workspace settings not loaded: "+e.getMessage());
			}
		}
		return settings;
	}
	
	public void setWorkspaceSettings(IWorkspaceSettingsHandler settings) {
		this.settings = settings;
	}

	public WorkspaceModel getModel() {
		if(wsModel == null) {
			wsModel = WorkspaceModel.createDefaultModel();
		}
		return wsModel;
	}
	
	public void setModel(WorkspaceModel model) {
		wsModel = model;
	}

	@Override
	public IWorkspaceView getView() {
		return getWorkspaceView();
	}
	
	public FileReadManager getFileTypeManager() {
		if (this.fileTypeManager == null) {
			this.fileTypeManager = new FileReadManager();
			Properties props = new Properties();
			try {
				props.load(this.getClass().getResourceAsStream("/conf/filenodetypes.properties"));

				Class<?>[] args = {};
				for (Object key : props.keySet()) {
					try {
						Class<?> clazz = DefaultFileNodeCreator.class;
						
						clazz = this.getClass().getClassLoader().loadClass(key.toString());

						AFileNodeCreator handler = (AFileNodeCreator) clazz.getConstructor(args).newInstance();
						handler.setFileTypeList(props.getProperty(key.toString(), ""), "\\|");
						this.fileTypeManager.addFileHandler(handler);
					}
					catch (ClassNotFoundException e) {
						LogUtils.warn("Class not found [" + key + "]", e);
					}
					catch (ClassCastException e) {
						LogUtils.warn("Class [" + key + "] is not of type: PhysicalNode", e);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		return this.fileTypeManager;
	}

	public URI getDefaultProjectHome() {
		File home = URIUtils.getAbsoluteFile(WorkspaceController.getApplicationHome());
		home = new File(home, "projects");
		return  home.toURI();
	}

	public void shutdown() {
		save();
	}

	private IProjectSelectionListener getProjectSelectionListener() {
		if(this.projectSelectionListener == null) {
			this.projectSelectionListener = new IProjectSelectionListener() {
				public void selectionChanged(ProjectSelectionEvent event) {
					currentSelectedProject = event.getSelectedProject();
				}
			};
		}
		return this.projectSelectionListener;
	}
	
	@Override
	public AWorkspaceProject getCurrentProject() {
		return currentSelectedProject;		
	}

	@Override
	public void save() {
		saveSettings();		
	}

	@Override
	public void load() {
		clear();
		if(this.viewUpdater != null) {
			this.viewUpdater.run();
		}
		String[] projectsIds = getWorkspaceSettings().getProperty(WorkspaceSettings.WORKSPACE_MODEL_PROJECTS, "").split(WorkspaceSettings.WORKSPACE_MODEL_PROJECTS_SEPARATOR);
		for (String projectID : projectsIds) {
			String projectHome = getWorkspaceSettings().getProperty(projectID);
			if(projectHome == null) {
				continue;
			}
			AWorkspaceProject project = null;
			try {
				project = AWorkspaceProject.create(projectID, URIUtils.createURI(projectHome));
				getModel().addProject(project);
				getProjectLoader().loadProject(project);
			}
			catch (Exception e) {
				LogUtils.severe(e);
				if(project != null) {
					getModel().removeProject(project);
				}
			}
		}
	}

	@Override
	public void clear() {
		getView().setPaintingEnabled(false);
		AWorkspaceProject[] projects = getModel().getProjects().toArray(new AWorkspaceProject[0]);
		for (AWorkspaceProject project : projects) {
			getModel().removeProject(project);
		}
		getView().setPaintingEnabled(true);
	}

}
