package org.eclipse.sirius.ease;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.ease.modules.AbstractScriptModule;
import org.eclipse.ease.modules.ScriptParameter;
import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.sirius.business.api.componentization.ViewpointRegistry;
import org.eclipse.sirius.business.api.modelingproject.ModelingProject;
import org.eclipse.sirius.business.api.session.Session;
import org.eclipse.sirius.ecore.extender.business.api.accessor.exception.FeatureNotFoundException;
import org.eclipse.sirius.ecore.extender.business.api.accessor.exception.MetaClassNotFoundException;
import org.eclipse.sirius.ecore.extender.business.api.permission.exception.LockedInstanceException;
import org.eclipse.sirius.ext.base.Option;
import org.eclipse.sirius.ui.business.api.dialect.DialectUIManager;
import org.eclipse.sirius.ui.business.api.viewpoint.ViewpointSelectionCallback;
import org.eclipse.sirius.ui.business.internal.commands.ChangeViewpointSelectionCommand;
import org.eclipse.sirius.ui.tools.api.project.ModelingProjectManager;
import org.eclipse.sirius.viewpoint.DAnalysis;
import org.eclipse.sirius.viewpoint.DRepresentation;
import org.eclipse.sirius.viewpoint.DView;
import org.eclipse.sirius.viewpoint.description.Viewpoint;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

/**
 * A module that provides high-level Sirius functions to be used in any language supported by EASE. 
 * @author A. Bernard 
 *
 */
public class SiriusModule extends AbstractScriptModule {

	/**
	 * Convert the given project to a {@link ModelingProject}. This should be called somewhere in 
	 * a {@link WorkspaceModifyOperation}.
	 * @param project
	 * @return the project as a {@link ModelingProject}.
	 * @throws CoreException if an error occurs
	 */
	@WrapToScript
	public ModelingProject configureToModeling(IProject project) throws CoreException {
		ModelingProjectManager.INSTANCE.convertToModelingProject(project, new NullProgressMonitor());
		Option<ModelingProject> modelingProject = ModelingProject.asModelingProject(project);
		return modelingProject.get();
	}

	/**
	 * Get the {@link Viewpoint} that can be applied to the provided model file. It relies on the file extension 
	 * to compute the available viewpoints.
	 * @param modelFile
	 * @return a collection of viewpoints
	 */
	@WrapToScript
	public Set<Viewpoint> getViewpointsFor(IFile modelFile) {
		Set<Viewpoint> designViewpoints = new HashSet<>();
		String fileExtn = modelFile.getFileExtension();
		for (Viewpoint v : ViewpointRegistry.getInstance().getViewpoints()) {
			if (v.getModelFileExtension().equalsIgnoreCase(fileExtn)) {
				designViewpoints.add(v);
			}
		}
		return designViewpoints;
	}

	/**
	 * Create a Sirius {@link Session} for the provided model file. It uses the first available {@link Viewpoint} that 
	 * can be applied to the model file and applies it. If <code>initialization</code> is set to <code>true</code> in 
	 * the representation definition, then Sirius will create automatically.
	 * @param modelFile the file containing the EMF model
	 * @return the created Sirius session
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 */
	@WrapToScript
	public Session createSession(IFile modelFile) throws InvocationTargetException, InterruptedException {

		SiriusSessionCreationOperation operation = new SiriusSessionCreationOperation(modelFile);
		operation.run(new NullProgressMonitor());
		Session siriusSession = operation.getCreatedSession();
		DAnalysis root = (DAnalysis) siriusSession.getSessionResource().getContents().get(0);
		DView dView = root.getOwnedViews().get(0);
		DRepresentation myRepresentation = dView.getOwnedRepresentationDescriptors().get(0).getRepresentation();
		DialectUIManager.INSTANCE.openEditor(siriusSession, myRepresentation, new NullProgressMonitor());
		return operation.getCreatedSession();
	}

	/**
	 * Get the representation at the given index created in the provided Sirius session. 
	 * @param siriusSession
	 * @param index the index of the representation in the list of all representations in the given session
	 * @return the representation at the given index, or <code>null</code> if the index is too high
	 */
	@WrapToScript
	public DRepresentation getRepresentation(Session siriusSession, @ScriptParameter(defaultValue="0")int index) {
		DAnalysis root = (DAnalysis) siriusSession.getSessionResource().getContents().get(0);
		DView dView = root.getOwnedViews().get(0);
		if (index >= 0 && index < dView.getOwnedRepresentationDescriptors().size()) {
			return dView.getOwnedRepresentationDescriptors().get(index).getRepresentation();
		} 
		return null;
	}

	/**
	 * Load the Sirius {@link Session} already opened in the given project. 
	 * @param project
	 * @return the session, or <code>null</code> if none
	 */
	@WrapToScript
	public Session loadExistingSession(IProject project) {
		Option<ModelingProject> modelingProject = ModelingProject.asModelingProject(project);
		if (modelingProject.some()) {
			return modelingProject.get().getSession();
		} else {
			return null;
		}

	}

	/**
	 * Get the root of the EMF model displayed in the given representation. 
	 * @param session
	 * @param representation
	 * @param modelFileExtn the extension of the file containing the EMF model
	 * @return the loaded object, or <code>null</code> if not found
	 */
	@WrapToScript
	public EObject getModelRoot(Session session, DRepresentation representation, String modelFileExtn) {
		TransactionalEditingDomain ed = session.getTransactionalEditingDomain();
		for (Resource res : ed.getResourceSet().getResources()) {
			String resExtn = res.getURI().fileExtension();
			if (resExtn != null && resExtn.equals(modelFileExtn)) {
				EObject diagramRoot = res.getContents().get(0);
				return diagramRoot;
			}
		}
		return null;
	}

	/**
	 * Invoke an EMF 'Add' operation to add an element to the given feature of the given container.
	 * @param session
	 * @param container the container that will contain the created model element
	 * @param feature the name of the containment feature to which the new element will be added
	 * @param instanceName the class name of the object to be instantiated
	 * @return the created {@link EObject}
	 */
	@WrapToScript
	public EObject invokeAdd(Session session, EObject container, String feature, String instanceName) {
		try {
			EObject obj = session.getModelAccessor().createInstance(instanceName);
			TransactionalEditingDomain ed = session.getTransactionalEditingDomain();
			ed.getCommandStack().execute(new RecordingCommand(ed) {
				@Override
				protected void doExecute() {
					try {
						session.getModelAccessor().eAdd(container, feature, obj);
					} catch (LockedInstanceException | FeatureNotFoundException e) {
						Activator.getDefault().getLog().log(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
					}
				}
			});
			return obj;
		} catch (MetaClassNotFoundException | LockedInstanceException e) {
			Activator.getDefault().getLog().log(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
		} 
		return null;
	}

	/**
	 * Invoke an EMF 'Set' operation to set a value of an EMF model element. 
	 * @param session
	 * @param container the element that will be modified
	 * @param feature the name of the feature that will be modified
	 * @param value the value to be set
	 */
	@WrapToScript
	public void invokeSet(Session session, EObject container, String feature, Object value) {
		try {
			TransactionalEditingDomain ed = session.getTransactionalEditingDomain();
			ed.getCommandStack().execute(new RecordingCommand(ed) {
				@Override
				protected void doExecute() {
					try {
						session.getModelAccessor().eSet(container, feature, value);
					} catch (LockedInstanceException | FeatureNotFoundException e) {
						Activator.getDefault().getLog().log(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
					}
				}
			});
		} catch (LockedInstanceException e) {
			Activator.getDefault().getLog().log(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
		} 
	}


	/* END OF EASE VALIDATED FUNCTIONS */
	
//	@WrapToScript
//	public void invoke2(Session session, DRepresentation representation) {
//		ModelAccessor modelAccessor = session.getModelAccessor();
//		TaskHelper taskHelper = new TaskHelper(modelAccessor, new NoUICallback());
//		ModelOperation modelOperation = null;
//		 Collection<Viewpoint> selectedViewpoints = session.getSelectedViewpoints(true);
//	        for (Viewpoint viewpoint : selectedViewpoints) {
//	            Resource eResource = viewpoint.eResource();
//	            if (eResource != null && URI.createURI("platform:/plugin/org.eclipse.sirius.sample.basicfamily.advanced.design/description/basicfamily.odesign").trimFragment().equals(eResource.getURI())) {
//	                EObject modelOperationEObject = eResource.getEObject(URI.createURI("platform:/plugin/org.eclipse.sirius.sample.basicfamily.advanced.design/description/basicfamily.odesign#man").fragment());
//	                if (modelOperationEObject instanceof InitialOperation) {
//	                    modelOperation = ((InitialOperation) modelOperationEObject).getFirstModelOperations();
//	                }
//	            }
//	        }
//	}
//
//	@WrapToScript
//	public void invokeTool(Session session, DRepresentation representation) {
//		try {
//			EObject obj = session.getModelAccessor().createInstance("Man");
//			session.getModelAccessor().eAdd(session.getTransactionalEditingDomain().getResourceSet().getResources().get(3).getContents().get(0), "members", obj);
//			System.out.println(obj);
//		} catch (MetaClassNotFoundException e) {
//			e.printStackTrace();
//		} catch (LockedInstanceException e) {
//			e.printStackTrace();
//		} catch (FeatureNotFoundException e) {
//			e.printStackTrace();
//		}
//		CreateView createViewOp = ToolFactory.eINSTANCE.createCreateView();
//		List<DiagramElementMapping> maps = getMappings(representation.getRepresentationElements().get(0), session);
//		System.out.println(maps);
//	}

//	private List<DiagramElementMapping> getMappings(final DSemanticDecorator containerView,
//			Session session) {
//		final List<DiagramElementMapping> mappings = new ArrayList<DiagramElementMapping>();
//
//		if (containerView instanceof DSemanticDiagram) {
//
//			for (final DiagramElementMapping mapping : ((DSemanticDiagram)containerView).getDescription()
//					.getAllContainerMappings()) {
//				if (!mapping.isCreateElements()) {
//					mappings.add(mapping);
//				}
//			}
//			for (final DiagramElementMapping mapping : ((DSemanticDiagram)containerView).getDescription()
//					.getAllNodeMappings()) {
//				if (!mapping.isCreateElements()) {
//					mappings.add(mapping);
//				}
//			}
//		} else if (containerView instanceof DNodeContainerSpec) {
//			for (final DiagramElementMapping mapping : ((DNodeContainerSpec)containerView).getActualMapping()
//					.getAllContainerMappings()) {
//				if (!mapping.isCreateElements()) {
//					mappings.add(mapping);
//				}
//			}
//			for (final DiagramElementMapping mapping : ((DNodeContainerSpec)containerView).getActualMapping()
//					.getAllNodeMappings()) {
//				if (!mapping.isCreateElements()) {
//					mappings.add(mapping);
//				}
//			}
//		}
//		return mappings;
//	}


	/**
	 * A {@link WorkspaceModifyOperation} that automatically configures the project containing an EMF resource file, 
	 * opens a session.
	 * @author alain
	 *
	 */
	private class SiriusSessionCreationOperation extends WorkspaceModifyOperation {
		private Session createdSession;
		private IFile modelFile;

		public SiriusSessionCreationOperation(IFile modelFile) {
			this.modelFile = modelFile;
		}

		@Override
		protected void execute(IProgressMonitor monitor)
				throws CoreException, InvocationTargetException, InterruptedException {
			IProject project = modelFile.getProject();
			configureToModeling(project);

			// get runlog viewpoints from design pluin
			Set<Viewpoint> designViewpoints = getViewpointsFor(modelFile);

			// get Sirius session
			Option<ModelingProject> modelingProject = ModelingProject.asModelingProject(project);
			Session siriusSession = modelingProject.get().getSession();

			siriusSession.getTransactionalEditingDomain().getResourceSet().getURIConverter().getURIMap().putAll(EcorePlugin.computePlatformURIMap(true));

			// create command to set requested viewpoint
			@SuppressWarnings("restriction")
			ChangeViewpointSelectionCommand cc = new ChangeViewpointSelectionCommand(siriusSession, new ViewpointSelectionCallback(), designViewpoints,
					Collections.<Viewpoint> emptySet(), monitor);
			// execute command
			siriusSession.getTransactionalEditingDomain().getCommandStack().execute(cc);

			this.createdSession = siriusSession;

		}

		public Session getCreatedSession() {
			return createdSession;
		}
	}



}
