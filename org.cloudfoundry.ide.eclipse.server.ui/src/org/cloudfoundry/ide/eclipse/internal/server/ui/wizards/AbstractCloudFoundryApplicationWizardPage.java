/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VMware, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cloudfoundry.client.lib.ApplicationInfo;
import org.cloudfoundry.client.lib.CloudApplication;
import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.ModuleCache;
import org.cloudfoundry.ide.eclipse.internal.server.core.RuntimeType;
import org.cloudfoundry.ide.eclipse.internal.server.core.ModuleCache.ServerData;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @author Christian Dupuis
 * @author Leo Dos Santos
 * @author Terry Denney
 * @author Steffen Pingel
 */
@SuppressWarnings("restriction")
public abstract class AbstractCloudFoundryApplicationWizardPage extends WizardPage {

	private Pattern VALID_CHARS = Pattern.compile("[A-Za-z\\$_0-9\\-]+");

	protected static final String DEFAULT_DESCRIPTION = "Specify application details";

	// private CloudApplication app;

	private String appName;

	private boolean canFinish;

	private ApplicationInfo lastApplicationInfo;

	private Text nameText;

	private String serverTypeId;

	private final CloudFoundryServer server;

	private final ApplicationModule module;

	private final CloudFoundryDeploymentWizardPage deploymentPage;

	private Map<String, String> runtimeByLabels;

	private String selectedRuntime;

	private Combo runtimeCombo;

	private String selectedFramework;

	public AbstractCloudFoundryApplicationWizardPage(CloudFoundryServer server,
			CloudFoundryDeploymentWizardPage deploymentPage, ApplicationModule module) {
		this(server, deploymentPage, module, null);
	}

	public AbstractCloudFoundryApplicationWizardPage(CloudFoundryServer server,
			CloudFoundryDeploymentWizardPage deploymentPage, ApplicationModule module, String framework) {
		super("Deployment Wizard");
		this.server = server;
		this.deploymentPage = deploymentPage;
		this.module = module;
		this.selectedFramework = framework;
		if (module == null) {
			// this.app = null;
			this.lastApplicationInfo = null;
		}
		else {
			// this.app = module.getApplication();
			this.lastApplicationInfo = module.getLastApplicationInfo();
			this.serverTypeId = module.getServerTypeId();
		}

		if (lastApplicationInfo == null) {
			lastApplicationInfo = detectApplicationInfo(module);
		}
		appName = lastApplicationInfo.getAppName();
	}

	public String getSelectedFramework() {
		return selectedFramework;
	}

	protected void setFramework(String framework) {
		this.selectedFramework = framework;
	}

	protected CloudFoundryApplicationWizard getApplicationWizard() {
		return (CloudFoundryApplicationWizard) getWizard();
	}

	protected void createRuntimeArea(Composite composite) {

		if (selectedRuntime != null && runtimeByLabels.size() > 0) {
			// Only show the runtime if it is a standalone, or there is more
			// than one runtime to chose from. Don't show the runtime if there
			// is only one runtime
			// and its not standalone
			if (getApplicationWizard().isStandaloneApplication() && runtimeByLabels.size() == 1) {
				Label runtimeLabel = new Label(composite, SWT.NONE);
				runtimeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
				runtimeLabel.setText("Runtime: ");

				Label runtime = new Label(composite, SWT.NONE);
				runtime.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false));
				String label = selectedRuntime;
				for (Map.Entry<String, String> entry : runtimeByLabels.entrySet()) {
					if (entry.getValue().equals(selectedRuntime)) {
						label = entry.getKey();
						break;
					}
				}
				runtime.setText(label);
			}
			else if (runtimeByLabels.size() > 1) {
				Label runtimeLabel = new Label(composite, SWT.NONE);
				runtimeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
				runtimeLabel.setText("Runtime: ");

				runtimeCombo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
				runtimeCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				int index = 0;
				for (Map.Entry<String, String> entry : runtimeByLabels.entrySet()) {
					runtimeCombo.add(entry.getKey());
					if (entry.getValue().equals(selectedRuntime)) {
						index = runtimeCombo.getItemCount() - 1;
					}
				}
				runtimeCombo.select(index);

				runtimeCombo.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
						update();
						selectedRuntime = runtimeByLabels.get(runtimeCombo.getText());
					}
				});
			}
		}
	}

	public String getSelectedRuntime() {
		return selectedRuntime;
	}

	public static String getAppName(ApplicationModule module) {
		CloudApplication app = module.getApplication();
		String appName = null;
		if (app != null && app.getName() != null) {
			appName = app.getName();
		}
		if (appName == null) {
			appName = module.getName();
		}
		return appName;
	}

	/**
	 * Create a detect an application info based on the given module.
	 * @param module
	 * @return
	 */
	protected ApplicationInfo detectApplicationInfo(ApplicationModule module) {

		String appName = getAppName(module);
		ApplicationInfo applicationInfo = new ApplicationInfo(appName);

		return applicationInfo;
	}

	public void createControl(Composite parent) {
		setTitle("Application details");
		setDescription(DEFAULT_DESCRIPTION);
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(serverTypeId);
		if (banner != null) {
			setImageDescriptor(banner);
		}

		List<RuntimeType> runtimes = getApplicationWizard().getRuntimes();
		runtimeByLabels = new HashMap<String, String>();

		for (RuntimeType type : runtimes) {
			runtimeByLabels.put(type.getLabel(), type.name());
		}

		if (!runtimes.isEmpty()) {
			selectedRuntime = runtimes.get(0).name();
		}

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createContents(composite);

		setControl(composite);

		update(false);
	}

	/**
	 * Get a new application info based on the UI selection. THis only returns a
	 * basic application info with the application name. Subclasses can override
	 * or set additional values in the application info.
	 * @return new Application info with just the application name
	 */
	public ApplicationInfo getApplicationInfo() {
		ApplicationInfo info = new ApplicationInfo(appName);
		info.setFramework(getSelectedFramework());
		return info;
	}

	protected ApplicationInfo getLastApplicationInfo() {
		return lastApplicationInfo;
	}

	@Override
	public boolean isPageComplete() {
		return canFinish;
	}

	protected Composite createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		nameLabel.setText("Name:");

		nameText = new Text(composite, SWT.BORDER);
		nameText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		nameText.setEditable(true);
		appName = lastApplicationInfo.getAppName();
		nameText.setText(appName);
		nameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				appName = nameText.getText();
				deploymentPage.updateUrl();
				update();
			}
		});

		createAdditionalContents(composite);

		return composite;

	}

	protected Composite createAdditionalContents(Composite parent) {
		createRuntimeArea(parent);
		return parent;
	}

	protected void update() {
		update(true);
	}

	protected void update(boolean updateButtons) {
		canFinish = true;
		if (nameText.getText() == null || nameText.getText().length() == 0) {
			setDescription("Enter an application name.");
			canFinish = false;
		}

		Matcher matcher = VALID_CHARS.matcher(nameText.getText());
		if (canFinish && !matcher.matches()) {
			setErrorMessage("The entered name contains invalid characters.");
			canFinish = false;
		}
		else {
			setErrorMessage(null);
		}

		ModuleCache moduleCache = CloudFoundryPlugin.getModuleCache();
		ServerData data = moduleCache.getData(server.getServerOriginal());
		Collection<ApplicationModule> applications = data.getApplications();
		boolean duplicate = false;

		for (ApplicationModule application : applications) {
			if (application != module && application.getApplicationId().equals(nameText.getText())) {
				duplicate = true;
				break;
			}
		}

		if (canFinish && duplicate) {
			setErrorMessage("The entered name conflicts with an application deployed.");
			canFinish = false;
		}

		if (updateButtons) {
			getWizard().getContainer().updateButtons();
		}
	}

}
