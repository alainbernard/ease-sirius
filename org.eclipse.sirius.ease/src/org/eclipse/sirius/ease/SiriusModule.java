package org.eclipse.sirius.ease;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ease.modules.AbstractScriptModule;
import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.sirius.business.api.componentization.ViewpointRegistry;
import org.eclipse.sirius.business.api.modelingproject.ModelingProject;
import org.eclipse.sirius.business.api.session.Session;
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
	
	@WrapToScript
	public DRepresentation getRepresentation(Session siriusSession) {
		DAnalysis root = (DAnalysis) siriusSession.getSessionResource().getContents().get(0);
		DView dView = root.getOwnedViews().get(0);
		DRepresentation myRepresentation = dView.getOwnedRepresentationDescriptors().get(0).getRepresentation();
		return myRepresentation;
	}


//	@WrapToScript
//	public DRepresentation createRepresentation(IFile modelFile, Session siriusSession) throws IOException {
//		Viewpoint vp = siriusSession.getSelectedViewpoints(false).iterator().next();
//		RepresentationDescription rdescr = vp.getOwnedRepresentations().get(0);
//
//		ResourceSet rset = new ResourceSetImpl();
//		Resource res = rset.createResource(URI.createPlatformResourceURI(modelFile.getFullPath().toString(), true));
//		res.load(new HashMap<Object, Object>());
//		EObject eObject = res.getContents().get(0);
//
//		CreateRepresentationCommand command = new CreateRepresentationCommand(siriusSession, rdescr, eObject, "My EASE Diagram", new NullProgressMonitor());
//		IEditingSession editingSession = SessionUIManager.INSTANCE.getUISession(siriusSession);
//		editingSession
//		.notify(EditingSessionEvent.REPRESENTATION_ABOUT_TO_BE_CREATED_BEFORE_OPENING);
//		siriusSession.getTransactionalEditingDomain().getCommandStack()
//		.execute(command);
//		editingSession.notify(EditingSessionEvent.REPRESENTATION_CREATED_BEFORE_OPENING);
//		DRepresentation createdDRepresentation = command.getCreatedRepresentation();
//
//
//		//		final CommandStack commandStack = siriusSession.getTransactionalEditingDomain().getCommandStack();
//		//		commandStack.execute(new RecordingCommand(siriusSession.getTransactionalEditingDomain()) {
//		//			@Override
//		//			protected void doExecute() {
//		//				DDiagram dsemanticDiagram = (DDiagram) siriusSession.getSessionResource().getContents().get(1);
//		//				if(dsemanticDiagram != null){
//		//					dsemanticDiagram.setName(modelFile.getProject().getName() + " diagram");
//		//				}
//		//			}
//		//		});
//
//		// open the generated Sirius representation
//		//		DAnalysis root = (DAnalysis) siriusSession.getSessionResource().getContents().get(0);
//		//		DView dView = root.getOwnedViews().get(0);
//		//		DRepresentation myRepresentation = dView.getOwnedRepresentationDescriptors().get(0).getRepresentation();
//		DialectUIManager.INSTANCE.openEditor(siriusSession, createdDRepresentation, new NullProgressMonitor());
//		return createdDRepresentation;
//	}


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
			ModelingProjectManager.INSTANCE.convertToModelingProject(project, monitor);

			String fileExtn = modelFile.getFileExtension();
			// get runlog viewpoints from design pluin
			Set<Viewpoint> designViewpoints = new HashSet<>();
			Viewpoint found = null;
			Iterator<Viewpoint> it = ViewpointRegistry.getInstance().getViewpoints().iterator();
			while (it.hasNext() && found == null) {
				Viewpoint v = it.next();
				if (v.getModelFileExtension().equalsIgnoreCase(fileExtn)) {
					found = v;
					designViewpoints.add(v);
				}
			}

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
