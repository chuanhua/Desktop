package org.docear.plugin.services;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import org.docear.plugin.core.DocearController;
import org.docear.plugin.core.event.DocearEvent;
import org.docear.plugin.core.event.DocearEventType;
import org.docear.plugin.core.event.IDocearEventListener;
import org.docear.plugin.core.features.DocearFileBackupController;
import org.docear.plugin.services.features.io.DocearConnectionProvider;
import org.docear.plugin.services.features.recommendations.RecommendationsController;
import org.docear.plugin.services.features.recommendations.actions.ShowRecommendationsAction;
import org.docear.plugin.services.features.setup.action.DocearSetupWizardAction;
import org.docear.plugin.services.features.update.UpdateCheck;
import org.docear.plugin.services.features.update.action.DocearCheckForUpdatesAction;
import org.docear.plugin.services.features.upload.MapLifeCycleListener;
import org.docear.plugin.services.features.upload.UploadController;
import org.docear.plugin.services.features.user.DocearUser;
import org.docear.plugin.services.features.user.DocearUserController;
import org.docear.plugin.services.features.user.UserFileBackupHandler;
import org.docear.plugin.services.features.user.action.DocearClearUserDataAction;
import org.docear.plugin.services.features.user.workspace.DocearWorkspaceSettings;
import org.docear.plugin.services.workspace.DocearWorkspaceModel;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.IMapLifeCycleListener;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.plugin.workspace.WorkspaceController;

public class ServiceController {
	public static final String DOCEAR_INFORMATION_RETRIEVAL = "docear_information_retrieval";

	public static final String DOCEAR_SAVE_BACKUP = "docear_save_backup";
	
	private static ServiceController serviceController;
	
	private final IMapLifeCycleListener mapLifeCycleListener = new MapLifeCycleListener();

	private final Map<Class<? extends ADocearServiceFeature>, ADocearServiceFeature> features = new LinkedHashMap<Class<? extends ADocearServiceFeature>, ADocearServiceFeature>();
	
	private ServiceController(ModeController modeController) {
		DocearFileBackupController.setFileBackupHandler(new UserFileBackupHandler());
		WorkspaceController.getModeExtension(modeController).setModel(new DocearWorkspaceModel());
		initListeners(modeController);

		new ServiceConfiguration(modeController);

		addPluginDefaults(modeController);
		
		Controller.getCurrentController().addAction(new DocearClearUserDataAction());
		Controller.getCurrentController().addAction(new DocearCheckForUpdatesAction());
		Controller.getCurrentController().addAction(new ShowRecommendationsAction());
	}

	protected static void initialize(ModeController modeController) {
		if (serviceController == null) {
			serviceController = new ServiceController(modeController);
			
			serviceController.installFeature(new DocearWorkspaceSettings());
			serviceController.installFeature(new DocearConnectionProvider());
			ServiceController.getConnectionController().setDefaultHeader("version", Integer.toString(DocearController.getController().getApplicationBuildNumber()));
			serviceController.installFeature(new DocearUserController());
			
			if (DocearController.getController().isLicenseDialogNecessary()) {
				DocearSetupWizardAction.startWizard(true);
			}
			serviceController.installFeature(new UploadController());
			serviceController.installFeature(new RecommendationsController());
			serviceController.installFeature(new UpdateCheck());
			
			ServiceController.getFeature(DocearUserController.class).installView(modeController);
			ServiceController.getFeature(RecommendationsController.class).startRecommendationsRequest();
		}
	}
	
	public void installFeature(ADocearServiceFeature feature) {
		try {
			registerFeatureController(feature.getClass(), feature);
			feature.installDefaults(Controller.getCurrentModeController());
		} catch (AlreadyRegisteredException e) {
			LogUtils.warn(e);
		}
		
	}

	private void initListeners(ModeController modeController) {
		DocearController.getController().getLifeCycleObserver().addMapLifeCycleListener(mapLifeCycleListener);
	}

	public static ServiceController getController() {
		return serviceController;
	}
	
	public void registerFeatureController(Class<? extends ADocearServiceFeature> featureKey, ADocearServiceFeature feature) throws AlreadyRegisteredException {
		ADocearServiceFeature old = replaceFeatureController(featureKey, feature);
		if(old != null) {
			synchronized (features) {
				features.put(featureKey, old);
			}
			throw new AlreadyRegisteredException(String.valueOf(featureKey)); 
		}
	}
	
	public ADocearServiceFeature replaceFeatureController(Class<? extends ADocearServiceFeature> featureKey, ADocearServiceFeature feature) throws AlreadyRegisteredException {
		synchronized (features) {
			return features.put(featureKey, feature);
		}
	}
	
	public static <T extends ADocearServiceFeature> T getFeature(Class<T> featureKey) {
		synchronized (ServiceController.getController().features) {
			return (T) ServiceController.getController().features.get(featureKey);
		}
	}

	private void addPluginDefaults(ModeController modeController) {
		final URL defaults = this.getClass().getResource(ResourceController.PLUGIN_DEFAULTS_RESOURCE);
		if (defaults == null) throw new RuntimeException("cannot open " + ResourceController.PLUGIN_DEFAULTS_RESOURCE);
		Controller.getCurrentController().getResourceController().addDefaults(defaults);
		
		DocearController.getController().addDocearEventListener(new IDocearEventListener() {		
			public void handleEvent(DocearEvent event) {
				if (event.getType() == DocearEventType.APPLICATION_CLOSING) {
					shutdown();
				}
			}
		});
	}
	
	public URI getOnlineServiceUri() {
		return getFeature(DocearConnectionProvider.class).getOnlineServiceUri();
	}
	
	public URI getUserSettingsHome() {
		File home = new File(getFeature(DocearWorkspaceSettings.class).getSettingsPath());//new File(URIUtils.getFile(WorkspaceController.getApplicationSettingsHome()), "/users/"+getCurrentUser().getName());
		return home.toURI();
	}

	public static DocearUser getCurrentUser() {
		return DocearUserController.getActiveUser();
	}

	public static DocearConnectionProvider getConnectionController() {
		return getFeature(DocearConnectionProvider.class);
	}	
	
	private void shutdown() {
		
	}

}
