package org.eclipse.sirius.ease;

import java.lang.reflect.InvocationTargetException;
import java.rmi.activation.Activator;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ease.modules.AbstractScriptModule;
import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.emf.common.command.CommandStack;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.sirius.business.api.componentization.ViewpointRegistry;
import org.eclipse.sirius.business.api.modelingproject.ModelingProject;
import org.eclipse.sirius.business.api.session.Session;
import org.eclipse.sirius.diagram.DDiagram;
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

public class SiriusModule extends AbstractScriptModule {

	
	
	@WrapToScript
	public void toto() {
		System.out.println(ViewpointRegistry.getInstance().getViewpoints());
		
	}
	
	public Session createSession(IProject project, IFile modelFile) throws InvocationTargetException, InterruptedException {
		  WorkspaceModifyOperation operation = new WorkspaceModifyOperation() {
            @Override
            protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
            	ModelingProjectManager.INSTANCE.convertToModelingProject(project, monitor);

                // get runlog viewpoints from design pluin
                Set<Viewpoint> designViewpoints = new HashSet<>();
                
                System.out.println(ViewpointRegistry.getInstance().getViewpoints());
                //ewormsDesignViewpoints.add(e);

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

                // change representation name
                final CommandStack commandStack = siriusSession.getTransactionalEditingDomain().getCommandStack();
                commandStack.execute(new RecordingCommand(siriusSession.getTransactionalEditingDomain()) {
                      @Override
                      protected void doExecute() {
                    	  DDiagram dsemanticDiagram = (DDiagram) siriusSession.getSessionResource().getContents().get(1);
                            if(dsemanticDiagram != null){
                                 dsemanticDiagram.setName(project.getName() + " Runlog");
                            }
                      }
                });

                // open the generated Sirius representation
                DAnalysis root = (DAnalysis) siriusSession.getSessionResource().getContents().get(0);
                DView dView = root.getOwnedViews().get(0);
                DRepresentation myRepresentation = dView.getOwnedRepresentationDescriptors().get(0).getRepresentation();
                DialectUIManager.INSTANCE.openEditor(siriusSession, myRepresentation, monitor);
            }
		  };
		  operation.run(new NullProgressMonitor());
		return null;
	
	
          
	}

	
	
//	@WrapToScript
//	public DRe

}
